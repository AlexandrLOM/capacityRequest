package tech.scalea.capacityRequest.service.impl;

import org.optaplanner.core.api.score.constraint.ConstraintMatchTotal;
import org.optaplanner.core.api.score.constraint.Indictment;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.impl.score.director.ScoreDirector;
import org.optaplanner.core.impl.score.director.ScoreDirectorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tech.scalea.capacityRequest.entity.InvCapacityRequestEntity;
import tech.scalea.capacityRequest.model.AllocationSolution;
import tech.scalea.capacityRequest.model.CalculationData;
import tech.scalea.capacityRequest.model.CapacityRequest;
import tech.scalea.capacityRequest.model.Result;
import tech.scalea.capacityRequest.model.ResultVcpuAndRam;
import tech.scalea.capacityRequest.model.ServerModel;
import tech.scalea.capacityRequest.model.VmModel;
import tech.scalea.capacityRequest.model.response.Alert;
import tech.scalea.capacityRequest.model.response.Response;
import tech.scalea.capacityRequest.model.response.ServerInfo;
import tech.scalea.capacityRequest.service.CapacityRequestService;
import tech.scalea.capacityRequest.service.DataPreparationService;

import javax.xml.crypto.Data;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CapacityRequestServiceImpl implements CapacityRequestService {

    private static final Logger logger = LoggerFactory.getLogger(CapacityRequestServiceImpl.class);

    private DataPreparationService dataPreparationService;

    @Autowired
    public CapacityRequestServiceImpl(DataPreparationService dataPreparationService) {
        this.dataPreparationService = dataPreparationService;
    }

    @Override
    public Response startCapacityCalculationByCapacityRequestId(UUID id) {
        Response response = new Response();
        response.setSererList(new ArrayList<>());

        CalculationData calculationData = dataPreparationService.getVmModelListByCapacityRequestIdAndAllVMsBefore(id);
        logger.info("Capacity request Id [{}], All VMs [{}]", id, calculationData.getVmModelList().size());
        response.setDueDate(calculationData.getDueDate());
        response.setCapacityRequest(id);
        response.setInvCapacityRequestEntityList(calculationData.getInvCapacityRequestEntityList());

        Set<String> dcIdSet = calculationData.getVmModelList().stream().map(VmModel::getDcId).collect(Collectors.toSet());
        for (String dcId : dcIdSet) {
            if (dcId == null) {
                logger.warn("For some VMs DcId is not specified. Skip them...");
                continue;
            }
            List<VmModel> vmModelListByDcId = calculationData.getVmModelList().stream()
                    .filter(vmModel -> dcId.equals(vmModel.getDcId()))
                    .collect(Collectors.toList());
            Set<String> typeSet = vmModelListByDcId.stream().map(VmModel::getComputeType).collect(Collectors.toSet());

            for (String type : typeSet) {
                if (type == null) {
                    logger.warn("For some VMs type is not specified. Skip them...");
                    continue;
                }

                List<VmModel> vmModelListByDcIdAndType = vmModelListByDcId.stream()
                        .filter(vmModel -> type.equals(vmModel.getComputeType()))
                        .collect(Collectors.toList());
                List<ServerModel> serverModelList = dataPreparationService.getServerModelListByDcIdAndComputeType(dcId, type);
                List<ServerModel> serverModeExpansionlList = dataPreparationService.getServerModelExpansionRequestByDueDateAndDcIdAndHostType(
                        calculationData.getDueDate(),
                        dcId,
                        type);
                serverModelList.addAll(serverModeExpansionlList);

                logger.info("Got allocationSolution for DC_id: {}, type: {}, HOSTs: {}({}), VMs: {}",
                        dcId, type, serverModelList.size(), serverModeExpansionlList.size(), vmModelListByDcIdAndType.size());

                response.getSererList().add(getResourceShortageInformation(serverModelList, vmModelListByDcIdAndType, dcId, type));
            }
        }
        return response;
    }

    private ServerInfo getResourceShortageInformation(List<ServerModel> serverModelList, List<VmModel> vmModelList, String dcId, String type) {
        ServerInfo serverInfo = dataPreparationService.getServerInfo(type);
        serverInfo.setDcId(dcId);
        serverInfo.setDedicatedExceededQuantity(0);
        serverInfo.setCapacityExceededQuantity(0);
        int numberOfServers = 0;

        AllocationSolution allocationSolution = startSolution(serverModelList, vmModelList);
        int hardScore = allocationSolution.getScore().getHardScore();

        List<ServerModel> newServerModelList = new ArrayList<>();
        newServerModelList.addAll(serverModelList);

        while (hardScore != 0) {
            if (newServerModelList.size() + serverModelList.size() > vmModelList.size()) {
                throw new RuntimeException("No solution for input data!" +
                        "\n" + newServerModelList +
                        "\n" + vmModelList);
            }

            serverInfo = analysisAllocationSolution(allocationSolution,
                    serverInfo);
            numberOfServers = serverInfo.getCapacityExceededQuantity() + serverInfo.getDedicatedExceededQuantity();
            logger.debug("Estimated servers count: {}", numberOfServers);

            List<ServerModel> newServes = getNewServerModelList(serverInfo, numberOfServers);
            newServerModelList.addAll(newServes);

            allocationSolution = startSolution(newServerModelList, vmModelList);
            hardScore = allocationSolution.getScore().getHardScore();

        }
        //serverInfo.setQuantity(numberOfServers);
        logger.debug("Final. Estimated servers count: {}", numberOfServers);
        return serverInfo;
    }

    private List<ServerModel> getNewServerModelList(ServerInfo serverInfo, Integer numberOfServers) {
        return getNewServerModelList(serverInfo.getDcId(),
                serverInfo.getType(),
                serverInfo.getVCpu(),
                serverInfo.getRam(),
                numberOfServers);
    }

    private List<ServerModel> getNewServerModelList(String dcId, String type, Integer vCpu, Integer ram, Integer numberOfServers) {
        List<ServerModel> serverModelList = new ArrayList<>();
        int i = numberOfServers;
        while (i > 0) {
            String id = UUID.randomUUID().toString();
            ServerModel serverModel = new ServerModel();
            serverModel.setDcId(dcId);
            serverModel.setHostName(id);
            serverModel.setHostId(id);
            serverModel.setVmCount(0);
            serverModel.setHostType(type);
            serverModel.setVCpuQuantity(vCpu);
            serverModel.setRamQuantity(ram);
            serverModelList.add(serverModel);

            i--;
        }

        return serverModelList;
    }

    private ServerInfo analysisAllocationSolution(AllocationSolution allocationSolution, ServerInfo serverInfo) {
        List<VmModel> resultVmList = new ArrayList<>();
        resultVmList.addAll(allocationSolution.getVmModelList());

        List<VmModel> violatorsAntiAffinityGroup = getViolatorsAntiAffinityGroup(resultVmList);
        List<VmModel> violatorsDedicatedCompute = getViolatorsDedicatedCompute(resultVmList);

        violatorsAntiAffinityGroup.forEach(resultVmList::remove);
        violatorsDedicatedCompute.forEach(resultVmList::remove);

        ResultVcpuAndRam resultVcpuAndRam = calculateRequiredNumberOfServers(resultVmList,
                serverInfo.getVCpu(),
                serverInfo.getRam());

        if ((serverInfo.getDescriptionCpuRam() == null || serverInfo.getDescriptionCpuRam().isEmpty())
                && (resultVcpuAndRam.getVCpu() < 0 || resultVcpuAndRam.getRam() < 0)) {
            serverInfo.setDescriptionCpuRam("vCPU: " + resultVcpuAndRam.getVCpu() + " RAM: " + resultVcpuAndRam.getRam());
        }

        int serversNumberByDedicatedCompute = violatorsDedicatedCompute.size();

        if (serverInfo.getDescriptionDedicatedGroups() == null || serverInfo.getDescriptionDedicatedGroups().isEmpty()) {
            serverInfo.setDescriptionDedicatedGroups("Dedicated Compute");
        }

        int serversNumberByAntiAffinityGroup = violatorsAntiAffinityGroup.size();
        Set<String> nameAAG = violatorsAntiAffinityGroup.stream().map(VmModel::getAntiAffinityGroup).collect(Collectors.toSet());
        if ((serverInfo.getDescriptionAntiAffinityGroups() == null || serverInfo.getDescriptionAntiAffinityGroups().isEmpty())
                && !violatorsAntiAffinityGroup.isEmpty()) {
            serverInfo.setDescriptionAntiAffinityGroups("Anti-affinity groups: ");
            nameAAG.forEach(name -> serverInfo.setDescriptionAntiAffinityGroups(
                    serverInfo.getDescriptionAntiAffinityGroups().concat(name + ", ")));
        }

        logger.debug("vCPU or RAM category: {}, Dedicated category: {}, Anti-Affinity category: {}",
                resultVcpuAndRam.getHostCount(), serversNumberByDedicatedCompute, serversNumberByAntiAffinityGroup);

        serverInfo.setCapacityExceededQuantity(serverInfo.getCapacityExceededQuantity()
                + Integer.max(resultVcpuAndRam.getHostCount(), serversNumberByAntiAffinityGroup));

        serverInfo.setDedicatedExceededQuantity(serverInfo.getDedicatedExceededQuantity()
                + serversNumberByDedicatedCompute);

        return serverInfo;

    }

//    private int calculateRequiredNumberOfServersForAntiAffinityGroup(List<Result> resultList) {
//        int quantity = 0;
//        List<VmModel> vmModelList = new ArrayList<>();
//        resultList.forEach(result -> vmModelList.addAll(result.getIncompatibleVmAntiAffinityGroup()));
//        Set<String> aagNameSet = vmModelList.stream().map(VmModel::getAntiAffinityGroup).collect(Collectors.toSet());
//        for (String aagName : aagNameSet) {
//
//            quantity = Integer.max(quantity, (int) vmModelList.stream().filter(vmModel -> aagName.equals(vmModel.getAntiAffinityGroup())).count());
//        }
//        return quantity;
//    }

    private ResultVcpuAndRam calculateRequiredNumberOfServers(List<VmModel> resultList, Integer vCpu, Integer ram) {
        int allVCpu = 0;
        int allRam = 0;
        Set<ServerModel> serverModelSet = resultList.stream().map(VmModel::getServerModel).collect(Collectors.toSet());
        for (ServerModel serverModel : serverModelSet) {
            int cpuServer = serverModel.getVCpuQuantity() - resultList.stream().filter(vmModel -> serverModel.equals(vmModel.getServerModel())).mapToInt(VmModel::getVcpuQty).sum();
            allVCpu = allVCpu + (cpuServer < 0 ? cpuServer : 0);
            int ramServer = serverModel.getRamQuantity() - resultList.stream().filter(vmModel -> serverModel.equals(vmModel.getServerModel())).mapToInt(VmModel::getRamQty).sum();
            allRam = allRam + (ramServer < 0 ? ramServer : 0);

        }
        logger.info("Total Capacity Exceeding, vCPU: {}, RAM: {}", allVCpu, allRam);
        return ResultVcpuAndRam.builder()
                .hostCount((int) Math.ceil(Double.max(Math.abs(allVCpu) / (double) vCpu, Math.abs(allRam) / (double) ram)))
                .vCpu(allVCpu)
                .ram(allRam)
                .build();
    }

    @Override
    public AllocationSolution startSolution(List<ServerModel> serverModelList, List<VmModel> vmModelList) {

        logger.debug("Servers: {}, VMs: {}",
                serverModelList.size(),
                vmModelList.size());

        // Build the Solver
        SolverFactory<AllocationSolution> solverFactory = SolverFactory.createFromXmlResource("allocationSolutionSolverConfig.xml");
        Solver<AllocationSolution> solver = solverFactory.buildSolver();

        AllocationSolution unsolvedCapacityRequestInf = new AllocationSolution();
        unsolvedCapacityRequestInf.setServerModelList(serverModelList);
        unsolvedCapacityRequestInf.setVmModelList(vmModelList);

        // Solve the problem
        AllocationSolution solvedCapacityRequestInf = solver.solve(unsolvedCapacityRequestInf);

        return solvedCapacityRequestInf;

    }

//    @Override
//    public List<Result> analysisResults(AllocationSolution calculateCapacity) {
//        List<Result> results = new ArrayList<>();
//        for (VmModel vmModel : calculateCapacity.getVmModelList()) {
//            Optional<Result> resultOptional = results.stream().filter(result -> vmModel.getServerModel().equals(result.getServerModel())).findFirst();
//            if (resultOptional.isPresent()) {
//                resultOptional.get().getVmModelList().add(vmModel);
//                resultOptional.get().setVcpuQty(resultOptional.get().getVcpuQty() - vmModel.getVcpuQty());
//                resultOptional.get().setRamQty(resultOptional.get().getRamQty() - vmModel.getRamQty());
//
//            } else {
//                Result result = new Result();
//                result.setServerModel(vmModel.getServerModel());
//                result.setVmModelList(new ArrayList<>());
//                result.getVmModelList().add(vmModel);
//                result.setVcpuQty(vmModel.getServerModel().getVCpuQuantity() - vmModel.getVcpuQty());
//                result.setRamQty(vmModel.getServerModel().getRamQuantity() - vmModel.getRamQty());
//                result.setIncompatibleVmDedicatedComputeList(new ArrayList<>());
//                result.setIncompatibleVmAntiAffinityGroup(new ArrayList<>());
//                results.add(result);
//            }
//
//        }
//        return results;
//    }

//    public CapacityRequest checkAntiAffinityGroup(CapacityRequest capacityRequest) {
//        logger.debug("Check AntiAffinity Group:");
//        for (Result result : capacityRequest.getResultList()) {
//            logger.debug("DcId: [{}], type [{}], Host: [{}]",
//                    result.getServerModel().getDcId(),
//                    result.getServerModel().getHostType(),
//                    result.getServerModel().getHostIdLong());
//            Set<String> nameAntiAffinityGroupSet = result.getVmModelList().stream().map(VmModel::getAntiAffinityGroup).collect(Collectors.toSet());
//            for (String name : nameAntiAffinityGroupSet) {
//                if (name == null) continue;
//                List<VmModel> vmModelList = result.getVmModelList().stream().filter(vmModel -> name.equals(vmModel.getAntiAffinityGroup())).collect(Collectors.toList());
//                logger.debug("Name: [{}], VMs: {}", name, vmModelList.size());
//                while (vmModelList.size() > 1) {
//                    VmModel vmModel = vmModelList.stream().findAny().get();
//                    result.getVmModelList().remove(vmModel);
//                    result.setVcpuQty(result.getVcpuQty() + vmModel.getVcpuQty());
//                    result.setRamQty(result.getRamQty() + vmModel.getRamQty());
//                    result.getIncompatibleVmAntiAffinityGroup().add(vmModel);
//                    vmModelList.remove(vmModel);
//                }
//            }
//        }
//        return capacityRequest;
//    }

    public List<VmModel> getViolatorsAntiAffinityGroup(List<VmModel> vmModelList) {
        logger.debug("Check AntiAffinity Group..");
        List<VmModel> violators = new ArrayList<>();
        Set<String> nameAntiAffinityGroupSet = vmModelList.stream()
                .map(VmModel::getAntiAffinityGroup)
                .collect(Collectors.toSet());
        for (String name : nameAntiAffinityGroupSet) {
            if (name == null) continue;

            List<VmModel> vmModelsAag = vmModelList.stream().filter(vmModel -> name.equals(vmModel.getAntiAffinityGroup())).collect(Collectors.toList());
            Set<ServerModel> serverModelSet = vmModelsAag.stream().map(VmModel::getServerModel).collect(Collectors.toSet());
            for (ServerModel serverModel : serverModelSet) {
                List<VmModel> vmModelListByAagAndServer = vmModelsAag.stream().filter(vmModel -> serverModel.equals(vmModel.getServerModel())).collect(Collectors.toList());
                if (vmModelListByAagAndServer.size() > 1) {
                    violators.addAll(vmModelListByAagAndServer);
                    violators.remove(vmModelListByAagAndServer.stream().findFirst().get());
                }
            }
        }
        return violators;
    }

    @Override
    public List<Alert> getAlertList(Response response) {
        List<Alert> alertList = new ArrayList<>();
        for (ServerInfo serverInfo : response.getSererList()) {
            if (serverInfo.getCapacityExceededQuantity() > 0) {
                alertList.add(creatAlert(serverInfo.getType(),
                        "Capacity Exceeded",
                        response.getCapacityRequest(),
                        response.getDueDate(),
                        serverInfo.getDcId(),
                        serverInfo.getCapacityExceededQuantity(),
                        (serverInfo.getDescriptionAntiAffinityGroups() == null ? "" : (serverInfo.getDescriptionAntiAffinityGroups()))
                                + (serverInfo.getDescriptionCpuRam() == null ? "" : (serverInfo.getDescriptionCpuRam()))));
            }
            if (serverInfo.getDedicatedExceededQuantity() > 0) {
                alertList.add(creatAlert(serverInfo.getType(),
                        "Dedicated Exceeded",
                        response.getCapacityRequest(),
                        response.getDueDate(),
                        serverInfo.getDcId(),
                        serverInfo.getDedicatedExceededQuantity(),
                        serverInfo.getDescriptionDedicatedGroups()));
            }
        }
        return alertList;
    }

    public Alert creatAlert(String hostType,
                            String category,
                            UUID capacityRequestId,
                            Date dueDate,
                            String dcId,
                            int hostCount,
                            String description) {
        Alert alert = new Alert();
        alert.setCategory(category);
        alert.setCapacityRequestId(capacityRequestId);
        alert.setDueDate(dueDate);
        alert.setDcid(dcId);
        alert.setHostType(hostType);
        alert.setHostCount(hostCount);
        alert.setDescription(description);
        return alert;
    }


//    public List<VmModel> addVmInVmModelList(List<VmModel> vmModelList, List<VmModel> resultList) {
//        for (VmModel vmModel : vmModelList) {
//            resultList = addVmInVmModelList(vmModel, resultList);
//        }
//        return resultList;
//    }

//    public List<VmModel> addVmInVmModelList(VmModel vmModel, List<VmModel> resultList) {
//        Optional<Result> resultOptional = resultList.stream().filter(result -> vmModel.getDcId().equals(result.getServerModel().getDcId()) && vmModel.getComputeType().equals(result.getServerModel().getHostType())).findFirst();
//        if (resultOptional.isPresent()) {
//            resultOptional.get().getVmModelList().add(vmModel);
//            resultOptional.get().setVcpuQty(resultOptional.get().getVcpuQty() - vmModel.getVcpuQty());
//            resultOptional.get().setRamQty(resultOptional.get().getRamQty() - vmModel.getRamQty());
//        } else {
//            Result result = new Result();
//            ServerModel serverModel = new ServerModel();
//            serverModel.setDcId(vmModel.getDcId());
//            serverModel.setHostType(vmModel.getComputeType());
//            serverModel.setVmCount(0);
//            result.setServerModel(serverModel);
//
//            result.setVcpuQty(0);
//            result.setRamQty(0);
//            result.setVmModelList(new ArrayList<>());
//
//            result.getVmModelList().add(vmModel);
//            result.setVcpuQty(result.getVcpuQty() - vmModel.getVcpuQty());
//            result.setRamQty(result.getRamQty() - vmModel.getRamQty());
//            resultList.add(result);
//        }
//        return resultList;
//    }

//    public CapacityRequest checkDedicatedCompute(CapacityRequest capacityRequest) {
//        logger.debug("Check Dedicated Compute:");
//        for (Result result : capacityRequest.getResultList()) {
//            logger.debug("DcId: [{}], type [{}], Host: [{}]",
//                    result.getServerModel().getDcId(),
//                    result.getServerModel().getHostType(),
//                    result.getServerModel().getHostIdLong());
//            List<VmModel> dedicatedVmList = result.getVmModelList().stream().filter(VmModel::isDedicatedCompute).collect(Collectors.toList());
//            logger.debug("VMs: [{}] , {}", dedicatedVmList.size(), dedicatedVmList);
//            if (dedicatedVmList.isEmpty()
//                    || dedicatedVmList.size() == 1 && capacityRequest.getResultList().size() == 1) {
//                continue;
//            } else {
//                List<VmModel> vmList = result.getVmModelList().stream().filter(vmModel -> !vmModel.isDedicatedCompute()).collect(Collectors.toList());
//                if (vmList.isEmpty() && result.getServerModel().getVmCount() == 0) {
//                    VmModel vmModel = dedicatedVmList.stream().findAny().get();
//                    dedicatedVmList.remove(vmModel);
//                    result.getVmModelList().removeAll(dedicatedVmList);
//                    for (VmModel vm : dedicatedVmList) {
//                        result.setVcpuQty(result.getVcpuQty() + vm.getVcpuQty());
//                        result.setRamQty(result.getRamQty() + vm.getRamQty());
//                    }
//                    result.getIncompatibleVmDedicatedComputeList().addAll(dedicatedVmList);
//                } else {
//                    result.getVmModelList().removeAll(dedicatedVmList);
//                    for (VmModel vm : dedicatedVmList) {
//                        result.setVcpuQty(result.getVcpuQty() + vm.getVcpuQty());
//                        result.setRamQty(result.getRamQty() + vm.getRamQty());
//                    }
//                    result.getIncompatibleVmDedicatedComputeList().addAll(dedicatedVmList);
//                }
//            }
//        }
//        return capacityRequest;
//    }

    public List<VmModel> getViolatorsDedicatedCompute(List<VmModel> resultList) {
        logger.info("Check Dedicated Compute..");
        List<VmModel> dedicatedVmList = resultList.stream().filter(VmModel::isDedicatedCompute).collect(Collectors.toList());
        List<VmModel> violators = new ArrayList<>();
        Set<ServerModel> serverModelSet = dedicatedVmList.stream().map(VmModel::getServerModel).collect(Collectors.toSet());
        for (ServerModel serverModel : serverModelSet) {
            List<VmModel> vms = dedicatedVmList.stream().filter(vmModel -> serverModel.equals(vmModel.getServerModel())).collect(Collectors.toList());
            if (serverModel.getVmCount() == 0 && vms.size() > 1) {
                violators.addAll(vms);
                violators.remove(vms.stream().findFirst().get());
            } else if (serverModel.getVmCount() != 0 && vms.size() > 1) {
                violators.addAll(vms);
            }
        }
        return violators;
    }

//    @Override
//    public List<Result> analysisResults(List<VmModel> vmModelList) {
//        List<Result> results = new ArrayList<>();
//        Result result = new Result();
//        result.setVmModelList(vmModelList);
//        result.setVcpuQty(vmModelList.stream().mapToInt(VmModel::getVcpuQty).sum());
//        result.setRamQty(vmModelList.stream().mapToInt(VmModel::getRamQty).sum());
//        results.add(result);
//        return results;
//    }

//    @Override
//    public List<CapacityRequest> capacityRequest(List<CapacityRequest> newCapacityRequestList) {
//        List<CapacityRequest> capacityRequestsList = new ArrayList<>();
//
//        List<InvCapacityRequestEntity> invCapacityRequestEntityList = dataPreparationService.getInvCapacityRequestEntityList();
//        Set<Date> dueDateSet = invCapacityRequestEntityList.stream().map(InvCapacityRequestEntity::getDueDate).collect(Collectors.toSet());
//        List<InvCapacityRequestEntity> invCapacityRequestEntities = new ArrayList<>();
//        for (Date dueDate : dueDateSet) {
//            logger.debug("dueDate: [{}]", dueDate);
//            CapacityRequest capacityRequest = new CapacityRequest();
//            capacityRequest.setDueDate(dueDate);
//            invCapacityRequestEntities.addAll(invCapacityRequestEntityList.stream().filter(invCapacityRequestEntity -> invCapacityRequestEntity.getDueDate().equals(dueDate)).collect(Collectors.toList()));
//            capacityRequest.setInvCapacityRequestEntityList(new ArrayList<>());
//            capacityRequest.getInvCapacityRequestEntityList().addAll(invCapacityRequestEntities);
//            capacityRequest.setSolverHard(0);
//            List<VmModel> vmModelList = new ArrayList<>();
//            List<Result> resultList = new ArrayList<>();
//
//            for (InvCapacityRequestEntity invCapacityRequestEntity : invCapacityRequestEntities) {
//                vmModelList.addAll(dataPreparationService.getVmModelListByCapacityRequestId(invCapacityRequestEntity.getId()));
//            }
//
//            Set<String> dcIdSet = vmModelList.stream().map(VmModel::getDcId).collect(Collectors.toSet());
//            for (String dcId : dcIdSet) {
//                if (dcId == null) {
//                    logger.warn("DC id not set for some VMs. [{}]", dcId);
//                    continue;
//                }
//                List<VmModel> vmModelListByDcId = vmModelList.stream().filter(vmMode -> dcId.equals(vmMode.getDcId())).collect(Collectors.toList());
//
//                Set<String> computeTypeSet = vmModelListByDcId.stream().map(VmModel::getComputeType).collect(Collectors.toSet());
//                for (String computeType : computeTypeSet) {
//                    logger.debug("dc id: [{}], compute type: [{}]",
//                            dcId,
//                            computeType);
//                    if (computeType == null) {
//                        logger.warn("Compute type not set for some VMs. [{}]", computeType);
//                        continue;
//                    }
//                    List<VmModel> vmModelListByDcIdAndComputeType = vmModelList.stream().filter(vmMode -> computeType.equals(vmMode.getComputeType())).collect(Collectors.toList());
//                    if (vmModelListByDcIdAndComputeType.isEmpty()) {
//                        continue;
//                    }
//                    List<ServerModel> serverModelList = dataPreparationService.getServerModelListByDcIdAndComputeType(dcId, computeType);
//                    if (serverModelList.isEmpty()) {
//                        resultList.addAll(analysisResults(vmModelList));
//                        continue;
//                    }
//
//                    if (!newCapacityRequestList.isEmpty()) {
//                        if (capacityRequest.getNewServers() == null) capacityRequest.setNewServers(new ArrayList<>());
//                        capacityRequest.getNewServers().addAll(getNewServerModel(newCapacityRequestList, dueDate, dcId, computeType));
//
//                        serverModelList.addAll(capacityRequest.getNewServers());
//                    }
//
//                    AllocationSolution allocationSolution = startSolution(serverModelList, vmModelListByDcIdAndComputeType);
//                    logger.debug("solver: {}", allocationSolution.getVmModelList());
//                    resultList.addAll(analysisResults(allocationSolution));
//                    capacityRequest.setSolverHard(capacityRequest.getSolverHard() + allocationSolution.getScore().getHardScore());
//                }
//            }
//            capacityRequest.setResultList(resultList);
//            capacityRequest = checkAntiAffinityGroup(capacityRequest);
//            capacityRequest = checkDedicatedCompute(capacityRequest);
//            capacityRequestsList.add(capacityRequest);
//        }
//
//        return capacityRequestsList;
//    }

//    @Override
//    public List<CapacityRequest> calculateRequiredNumberOfServers(List<CapacityRequest> capacityRequestList) {
//        List<CapacityRequest> newCapacityRequestList = new ArrayList<>();
//
//        for (CapacityRequest capacityRequest : capacityRequestList) {
//            //if (capacityRequest.getSolverHard() == 0) continue;
//
//            CapacityRequest newCapacityRequest = new CapacityRequest();
//            newCapacityRequest.setDueDate(capacityRequest.getDueDate());
//            newCapacityRequest.setInvCapacityRequestEntityList(capacityRequest.getInvCapacityRequestEntityList());
//            newCapacityRequest.setResultList(new ArrayList<>());
//            Set<String> dcIdSet = capacityRequest.getResultList().stream().map(result -> result.getServerModel().getDcId()).collect(Collectors.toSet());
//
//            for (String dcId : dcIdSet) {
//                List<Result> resultListByDcId = capacityRequest.getResultList().stream().filter(result -> dcId.equals(result.getServerModel().getDcId())).collect(Collectors.toList());
//                Set<String> typeSet = resultListByDcId.stream().map(result -> result.getServerModel().getHostType()).collect(Collectors.toSet());
//                for (String type : typeSet) {
//                    List<Result> resultListByDcIdAndType = capacityRequest.getResultList().stream().filter(result -> type.equals(result.getServerModel().getHostType())).collect(Collectors.toList());
//                    ServerModel serverModel = new ServerModel();
//                    serverModel.setDcId(dcId);
//                    serverModel.setHostType(type);
//                    serverModel.setVCpuQuantity(resultListByDcIdAndType.stream().filter(result -> 0 > result.getVcpuQty()).mapToInt(Result::getVcpuQty).sum());
//                    serverModel.setRamQuantity(resultListByDcIdAndType.stream().filter(result -> 0 > result.getRamQty()).mapToInt(Result::getRamQty).sum());
//                    serverModel.setVmCount(0);
//                    Result resultNew = new Result();
//                    resultNew.setServerModel(serverModel);
//                    resultNew.setIncompatibleVmDedicatedComputeList(new ArrayList<>());
//                    resultNew.setIncompatibleVmAntiAffinityGroup(new ArrayList<>());
//                    Set<String> nameAAGSet = new HashSet<>();
//                    for (Result result : resultListByDcIdAndType) {
//                        resultNew.getIncompatibleVmDedicatedComputeList().addAll(result.getIncompatibleVmDedicatedComputeList());
//                        nameAAGSet.addAll(result.getIncompatibleVmAntiAffinityGroup().stream().map(VmModel::getAntiAffinityGroup).collect(Collectors.toSet()));
//                    }
//                    for (String nameAAG : nameAAGSet) {
//                        for (Result result : resultListByDcIdAndType) {
//                            resultNew.getIncompatibleVmAntiAffinityGroup().addAll(result.getIncompatibleVmAntiAffinityGroup().stream()
//                                    .filter(vmModel -> nameAAG.equals(vmModel.getAntiAffinityGroup()))
//                                    .collect(Collectors.toList()));
//                        }
//                    }
//                    newCapacityRequest.getResultList().add(resultNew);
//                }
//            }
//            newCapacityRequestList.add(newCapacityRequest);
//        }
//        return newCapacityRequestList;
//    }

//    private List<ServerModel> getNewServerModel(List<CapacityRequest> capacityRequestList, Date date, String dcId, String type) {
//        List<ServerModel> newServerModelList = new ArrayList<>();
//        for (CapacityRequest capacityRequest : capacityRequestList) {
//            if (capacityRequest.getDueDate().equals(date)) {
//                for (Result result : capacityRequest.getResultList()) {
//                    if (result.getServerModel().getDcId().equals(dcId) && result.getServerModel().getHostType().equals(type)) {
//                        int serv = 0;
//                        Set<String> nameSet = result.getIncompatibleVmAntiAffinityGroup().stream().map(VmModel::getAntiAffinityGroup).collect(Collectors.toSet());
//                        List<List<VmModel>> vmList = new ArrayList<>();
//                        for (String name : nameSet) {
//                            vmList.add(result.getIncompatibleVmAntiAffinityGroup().stream().filter(vmModel -> name.equals(vmModel.getAntiAffinityGroup())).collect(Collectors.toList()));
//                        }
//                        serv = vmList.stream().mapToInt(List::size).max().orElse(0)
//                                + result.getIncompatibleVmDedicatedComputeList().size();
//                        if (serv == 0) {
//                            newServerModelList.addAll(getServerModelByCpuOrRam(dcId,
//                                    type,
//                                    Math.abs(result.getServerModel().getVCpuQuantity()),
//                                    Math.abs(result.getServerModel().getRamQuantity())
//                            ));
//                        } else {
//                            newServerModelList.addAll(getServerModel(dcId,
//                                    type,
//                                    serv));
//                        }
//                    }
//                }
//            }
//        }
//
//        return newServerModelList;
//    }

//    public List<ServerModel> getServerModelByCpuOrRam(String dcId, String type, int vCpu, int ram) {
//        int cpuServ = 0;
//        int ramServ = 0;
//        if ("SR_IOV".equals(type)) {
//            cpuServ = 68;
//            ramServ = 256;
//        }
//        if ("DPDK".equals(type)) {
//            cpuServ = 56;
//            ramServ = 256;
//        }
//        int i = (int) Double.max(Math.ceil(vCpu / cpuServ), Math.ceil(ram / ramServ));
//        List<ServerModel> serverModelList = new ArrayList<>();
//        while (i > 0) {
//            i--;
//            ServerModel serverModel = new ServerModel();
//            serverModel.setDcId(dcId);
//            serverModel.setHostType(type);
//            serverModel.setVCpuQuantity(cpuServ);
//            serverModel.setRamQuantity(ramServ);
//            serverModel.setVmCount(0);
//            serverModel.setHostId(UUID.randomUUID().toString());
//            serverModelList.add(serverModel);
//
//        }
//        return serverModelList;
//    }

//    public List<ServerModel> getServerModel(String dcId, String type, int count) {
//        int i = count;
//        int cpuServ = 0;
//        int ramServ = 0;
//        if ("SR_IOV".equals(type)) {
//            cpuServ = 68;
//            ramServ = 256;
//        }
//        if ("DPDK".equals(type)) {
//            cpuServ = 56;
//            ramServ = 256;
//        }
//        List<ServerModel> serverModelList = new ArrayList<>();
//        while (i > 0) {
//            i--;
//            ServerModel serverModel = new ServerModel();
//            serverModel.setDcId(dcId);
//            serverModel.setHostType(type);
//            serverModel.setVCpuQuantity(cpuServ);
//            serverModel.setRamQuantity(ramServ);
//            serverModel.setVmCount(0);
//            serverModel.setHostId(UUID.randomUUID().toString());
//            serverModelList.add(serverModel);
//
//        }
//        return serverModelList;
//    }

}
