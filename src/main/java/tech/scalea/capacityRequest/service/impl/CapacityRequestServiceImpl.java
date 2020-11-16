package tech.scalea.capacityRequest.service.impl;

import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tech.scalea.capacityRequest.model.AllocationSolution;
import tech.scalea.capacityRequest.model.CalculationData;
import tech.scalea.capacityRequest.model.ResultVcpuAndRam;
import tech.scalea.capacityRequest.model.ServerModel;
import tech.scalea.capacityRequest.model.VmModel;
import tech.scalea.capacityRequest.model.response.Alert;
import tech.scalea.capacityRequest.model.response.Response;
import tech.scalea.capacityRequest.model.response.ServerInfo;
import tech.scalea.capacityRequest.service.CapacityRequestService;
import tech.scalea.capacityRequest.service.DataPreparationService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CapacityRequestServiceImpl implements CapacityRequestService {

    private static final Logger logger = LoggerFactory.getLogger(CapacityRequestServiceImpl.class);

    private DataPreparationService dataPreparationService;

    private AllocationSolution unsolvedCapacityRequestInf;

    @Autowired
    public CapacityRequestServiceImpl(DataPreparationService dataPreparationService,
                                      AllocationSolution allocationSolution) {
        this.dataPreparationService = dataPreparationService;
        this.unsolvedCapacityRequestInf = allocationSolution;
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
                //logger.warn("For some VMs DcId is not specified. Skip them...");
                throw new IllegalArgumentException("For some VMs DcId is not specified!");
            }
            List<VmModel> vmModelListByDcId = calculationData.getVmModelList().stream()
                    .filter(vmModel -> dcId.equals(vmModel.getDcId()))
                    .collect(Collectors.toList());
            Set<String> typeSet = vmModelListByDcId.stream().map(VmModel::getComputeType).collect(Collectors.toSet());

            for (String type : typeSet) {
                if (type == null) {
                    //ogger.warn("For some VMs type is not specified. Skip them...");
                    throw new IllegalArgumentException("For some VMs type is not specified!");

                } else if (type.equals("STORAGE_SSD") || type.equals("STORAGE_SAS")) {

                    ServerInfo serverInfo = calculateStorage(vmModelListByDcId, type, dcId, calculationData.getDueDate(), calculationData.getFromDate());
                    if (serverInfo != null) {
                        response.getSererList().add(serverInfo);
                    }

                } else {

                    List<VmModel> vmModelListByDcIdAndType = vmModelListByDcId.stream()
                            .filter(vmModel -> type.equals(vmModel.getComputeType()))
                            .collect(Collectors.toList());
                    List<ServerModel> serverModelList = dataPreparationService.getServerModelListByDcIdAndComputeType(dcId, type);
                    List<ServerModel> serverModeExpansionlList = dataPreparationService.getServerModelExpansionRequestByDueDateAndDcIdAndHostType(
                            calculationData.getDueDate(),
                            calculationData.getFromDate(),
                            dcId,
                            type);
                    serverModelList.addAll(serverModeExpansionlList);

                    logger.info("Got allocationSolution for DC_id: {}, type: {}, HOSTs: {}({}), VMs: {}",
                            dcId, type, serverModelList.size(), serverModeExpansionlList.size(), vmModelListByDcIdAndType.size());

                    response.getSererList().add(getResourceShortageInformation(serverModelList, vmModelListByDcIdAndType, dcId, type));
                }
            }
        }
        return response;
    }

    public ServerInfo getResourceShortageInformation(List<ServerModel> serverModelList, List<VmModel> vmModelList, String dcId, String type) {
        ServerInfo serverInfo = dataPreparationService.getServerInfoFromTemplate(type);
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
                throw new IllegalArgumentException("No solution for input data!" +
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

    public List<ServerModel> getNewServerModelList(ServerInfo serverInfo, Integer numberOfServers) {
        return getNewServerModelList(serverInfo.getDcId(),
                serverInfo.getType(),
                serverInfo.getVCpu(),
                serverInfo.getRam(),
                numberOfServers);
    }

    public List<ServerModel> getNewServerModelList(String dcId, String type, Integer vCpu, Integer ram, Integer numberOfServers) {
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

    public ServerInfo analysisAllocationSolution(AllocationSolution allocationSolution, ServerInfo serverInfo) {
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

    public ResultVcpuAndRam calculateRequiredNumberOfServers(List<VmModel> resultList, Integer vCpu, Integer ram) {
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

        //AllocationSolution unsolvedCapacityRequestInf = new AllocationSolution();
        unsolvedCapacityRequestInf.setServerModelList(serverModelList);
        unsolvedCapacityRequestInf.setVmModelList(vmModelList);

        // Solve the problem
        AllocationSolution solvedCapacityRequestInf = solver.solve(unsolvedCapacityRequestInf);

        return solvedCapacityRequestInf;

    }

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
            if (serverInfo.getCapacityExceededQuantity() != null && serverInfo.getCapacityExceededQuantity() > 0) {
                alertList.add(creatAlert(serverInfo.getType(),
                        "Capacity Exceeded",
                        response.getCapacityRequest(),
                        response.getDueDate(),
                        serverInfo.getDcId(),
                        serverInfo.getCapacityExceededQuantity(),
                        (serverInfo.getDescriptionAntiAffinityGroups() == null ? "" : (serverInfo.getDescriptionAntiAffinityGroups()))
                                + (serverInfo.getDescriptionCpuRam() == null ? "" : (serverInfo.getDescriptionCpuRam()))));
            } else if (serverInfo.getDedicatedExceededQuantity() != null && serverInfo.getDedicatedExceededQuantity() > 0) {
                alertList.add(creatAlert(serverInfo.getType(),
                        "Dedicated Exceeded",
                        response.getCapacityRequest(),
                        response.getDueDate(),
                        serverInfo.getDcId(),
                        serverInfo.getDedicatedExceededQuantity(),
                        serverInfo.getDescriptionDedicatedGroups()));
            } else if (serverInfo.getStorageExceededQuantity() != null && serverInfo.getStorageExceededQuantity() > 0) {
                alertList.add(creatAlert(serverInfo.getType(),
                        "Insufficient Storage Capacity",
                        response.getCapacityRequest(),
                        response.getDueDate(),
                        serverInfo.getDcId(),
                        serverInfo.getStorageExceededQuantity(),
                        serverInfo.getDescriptionStorage()));
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

    public ServerInfo calculateStorage(List<VmModel> vmModelList, String type, String dcId, Date dueDate, Date fromDate) {
        List<VmModel> vmModelListByDcIdAndType = vmModelList.stream()
                .filter(vmModel -> type.equals(vmModel.getComputeType()))
                .collect(Collectors.toList());
        Double storageCapacityRequest = vmModelListByDcIdAndType.stream().mapToInt(VmModel::getStorageQty).sum() / 1000D;
        Double storageExpansionRequest = dataPreparationService.getStorageExpansionRequest(dueDate, fromDate, dcId, type);
        Double storageServers = dataPreparationService.getStorageServers(dcId, type);
        Double storageTotal = (storageServers == null ? 0 : storageServers) + (storageExpansionRequest == null ? 0 : storageExpansionRequest);

        Double resultStorage = Math.round((storageTotal - storageCapacityRequest) * 10000) / 10000D;
        logger.info("Got allocationSolution for DC_id: {}, type: {}, total storage: {}({}), need storage: {}, result storage: {}",
                dcId,
                type,
                storageTotal,
                storageExpansionRequest == null ? 0 : storageExpansionRequest,
                storageCapacityRequest == null ? 0 : storageCapacityRequest,
                resultStorage == null ? 0 : resultStorage);

        if (resultStorage < 0) {
            ServerInfo serverInfo = dataPreparationService.getServerInfoFromTemplate(type);
            serverInfo.setDcId(dcId);
            if (serverInfo.getStorage() == 0) {
                throw new IllegalArgumentException("Server store options not found for " + type + ": " + serverInfo.getStorage());
            }
            serverInfo.setStorageExceededQuantity((int) Math.ceil(
                    Math.abs(resultStorage)
                            / serverInfo.getStorage()));
            serverInfo.setDescriptionStorage(type + ": " + resultStorage);

            logger.info("Final. Estimated servers count: {}", serverInfo.getStorageExceededQuantity());
            return serverInfo;
        }
        return null;
    }

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
}
