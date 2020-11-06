package tech.scalea.capacityRequest.service.impl;

import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tech.scalea.capacityRequest.entity.InvCapacityRequestEntity;
import tech.scalea.capacityRequest.model.AllocationSolution;
import tech.scalea.capacityRequest.model.CapacityRequest;
import tech.scalea.capacityRequest.model.Result;
import tech.scalea.capacityRequest.model.ServerModel;
import tech.scalea.capacityRequest.model.VmModel;
import tech.scalea.capacityRequest.service.CapacityRequestService;
import tech.scalea.capacityRequest.service.DataPreparationService;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CapacityRequestServiceImpl implements CapacityRequestService {

    private static final Logger logger = LoggerFactory.getLogger(CapacityRequestServiceImpl.class);

    private DataPreparationService dataPreparationService;

    @Autowired
    public CapacityRequestServiceImpl(DataPreparationService dataPreparationService) {
        this.dataPreparationService = dataPreparationService;
    }

    @Override
    public AllocationSolution calculateCapacity(List<ServerModel> serverModelList, List<VmModel> vmModelList) {

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

    @Override
    public List<Result> analysisResults(AllocationSolution calculateCapacity) {
        List<Result> results = new ArrayList<>();
        for (VmModel vmModel : calculateCapacity.getVmModelList()) {
            Optional<Result> resultOptional = results.stream().filter(result -> vmModel.getServerModel().equals(result.getServerModel())).findFirst();
            if (resultOptional.isPresent()) {
                resultOptional.get().getVmModelList().add(vmModel);
                resultOptional.get().setVcpuQty(resultOptional.get().getVcpuQty() - vmModel.getVcpuQty());
                resultOptional.get().setRamQty(resultOptional.get().getRamQty() - vmModel.getRamQty());

            } else {
                Result result = new Result();
                result.setServerModel(vmModel.getServerModel());
                result.setVmModelList(new ArrayList<>());
                result.getVmModelList().add(vmModel);
                result.setVcpuQty(vmModel.getServerModel().getVCpuQuantity() - vmModel.getVcpuQty());
                result.setRamQty(vmModel.getServerModel().getRamQuantity() - vmModel.getRamQty());
                result.setIncompatibleVmDedicatedComputeList(new ArrayList<>());
                result.setIncompatibleVmAntiAffinityGroup(new ArrayList<>());
                results.add(result);
            }

        }
        return results;
    }

    public CapacityRequest checkAntiAffinityGroup(CapacityRequest capacityRequest) {
        for (Result result : capacityRequest.getResultList()) {
            Set<String> nameAntiAffinityGroupSet = result.getVmModelList().stream().map(VmModel::getAntiAffinityGroup).collect(Collectors.toSet());
            for (String name : nameAntiAffinityGroupSet) {
                if (name == null) continue;
                List<VmModel> vmModelList = result.getVmModelList().stream().filter(vmModel -> name.equals(vmModel.getAntiAffinityGroup())).collect(Collectors.toList());
                while (vmModelList.size() > 1) {
                    VmModel vmModel = vmModelList.stream().findAny().get();
                    result.getVmModelList().remove(vmModel);
                    result.setVcpuQty(result.getVcpuQty() + vmModel.getVcpuQty());
                    result.setRamQty(result.getRamQty() + vmModel.getRamQty());
                    result.getIncompatibleVmAntiAffinityGroup().add(vmModel);
                    vmModelList.remove(vmModel);
                }
            }
        }
        return capacityRequest;
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

    public CapacityRequest checkDedicatedCompute(CapacityRequest capacityRequest) {
        for (Result result : capacityRequest.getResultList()) {
            List<VmModel> dedicatedVmList = result.getVmModelList().stream().filter(VmModel::isDedicatedCompute).collect(Collectors.toList());
            if (dedicatedVmList.isEmpty()
                    || dedicatedVmList.size() == 1 && capacityRequest.getResultList().size() == 1) {
                continue;
            } else {
                List<VmModel> vmList = result.getVmModelList().stream().filter(vmModel -> !vmModel.isDedicatedCompute()).collect(Collectors.toList());
                if (vmList.isEmpty() && result.getServerModel().getVmCount() == 0) {
                    VmModel vmModel = dedicatedVmList.stream().findAny().get();
                    dedicatedVmList.remove(vmModel);
                    result.getVmModelList().removeAll(dedicatedVmList);
                    for (VmModel vm : dedicatedVmList) {
                        result.setVcpuQty(result.getVcpuQty() + vm.getVcpuQty());
                        result.setRamQty(result.getRamQty() + vm.getRamQty());
                    }
                    result.getIncompatibleVmDedicatedComputeList().addAll(dedicatedVmList);
                } else {
                    result.getVmModelList().removeAll(dedicatedVmList);
                    for (VmModel vm : dedicatedVmList) {
                        result.setVcpuQty(result.getVcpuQty() + vm.getVcpuQty());
                        result.setRamQty(result.getRamQty() + vm.getRamQty());
                    }
                    result.getIncompatibleVmDedicatedComputeList().addAll(dedicatedVmList);
                }
            }
        }
        return capacityRequest;
    }

    @Override
    public List<Result> analysisResults(List<VmModel> vmModelList) {
        List<Result> results = new ArrayList<>();
        Result result = new Result();
        result.setVmModelList(vmModelList);
        result.setVcpuQty(vmModelList.stream().mapToInt(VmModel::getVcpuQty).sum());
        result.setRamQty(vmModelList.stream().mapToInt(VmModel::getRamQty).sum());
        results.add(result);
        return results;
    }

    @Override
    public List<CapacityRequest> capacityRequest(List<CapacityRequest> newCapacityRequest) {
        List<CapacityRequest> capacityRequestsList = new ArrayList<>();

        List<InvCapacityRequestEntity> invCapacityRequestEntityList = dataPreparationService.getInvCapacityRequestEntityList();
        Set<Date> dueDateSet = invCapacityRequestEntityList.stream().map(InvCapacityRequestEntity::getDueDate).collect(Collectors.toSet());
        List<InvCapacityRequestEntity> invCapacityRequestEntities = new ArrayList<>();
        for (Date dueDate : dueDateSet) {
            CapacityRequest capacityRequest = new CapacityRequest();
            capacityRequest.setDueDate(dueDate);
            invCapacityRequestEntities.addAll(invCapacityRequestEntityList.stream().filter(invCapacityRequestEntity -> invCapacityRequestEntity.getDueDate().equals(dueDate)).collect(Collectors.toList()));
            capacityRequest.setInvCapacityRequestEntityList(new ArrayList<>());
            capacityRequest.getInvCapacityRequestEntityList().addAll(invCapacityRequestEntities);
            capacityRequest.setSolverHard(0);
            List<VmModel> vmModelList = new ArrayList<>();
            List<Result> resultList = new ArrayList<>();

            for (InvCapacityRequestEntity invCapacityRequestEntity : invCapacityRequestEntities) {
                vmModelList.addAll(dataPreparationService.getVmModelListByCapacityRequestId(invCapacityRequestEntity.getId()));
            }

            Set<String> dcIdSet = vmModelList.stream().map(VmModel::getDcId).collect(Collectors.toSet());
            for (String dcId : dcIdSet) {
                if (dcId == null) {
                    logger.warn("DC id not set for some VMs. [{}]", dcId);
                    continue;
                }
                List<VmModel> vmModelListByDcId = vmModelList.stream().filter(vmMode -> dcId.equals(vmMode.getDcId())).collect(Collectors.toList());

                Set<String> computeTypeSet = vmModelListByDcId.stream().map(VmModel::getComputeType).collect(Collectors.toSet());
                for (String computeType : computeTypeSet) {
                    if (computeType == null) {
                        logger.warn("Compute type not set for some VMs. [{}]", computeType);
                        continue;
                    }
                    List<VmModel> vmModelListByDcIdAndComputeType = vmModelList.stream().filter(vmMode -> computeType.equals(vmMode.getComputeType())).collect(Collectors.toList());
                    if (vmModelListByDcIdAndComputeType.isEmpty()) {
                        continue;
                    }
                    List<ServerModel> serverModelList = dataPreparationService.getServerModelListByDcIdAndComputeType(dcId, computeType);
                    if (serverModelList.isEmpty()) {
                        resultList.addAll(analysisResults(vmModelList));
                        continue;
                    }
                    AllocationSolution allocationSolution = calculateCapacity(serverModelList, vmModelListByDcIdAndComputeType);
                    resultList.addAll(analysisResults(allocationSolution));
                    capacityRequest.setSolverHard(capacityRequest.getSolverHard() + allocationSolution.getScore().getHardScore());
                }
            }
            capacityRequest.setResultList(resultList);
            capacityRequest = checkAntiAffinityGroup(capacityRequest);
            capacityRequest = checkDedicatedCompute(capacityRequest);
            capacityRequestsList.add(capacityRequest);
        }

        return capacityRequestsList;
    }

    @Override
    public List<CapacityRequest> calculateRequiredNumberOfServers(List<CapacityRequest> capacityRequestList) {
        List<CapacityRequest> newCapacityRequestList = new ArrayList<>();

        for (CapacityRequest capacityRequest : capacityRequestList) {
            if (capacityRequest.getSolverHard() == 0) continue;

            CapacityRequest newCapacityRequest = new CapacityRequest();
            newCapacityRequest.setDueDate(capacityRequest.getDueDate());
            newCapacityRequest.setInvCapacityRequestEntityList(capacityRequest.getInvCapacityRequestEntityList());
            newCapacityRequest.setResultList(new ArrayList<>());
            Set<String> dcIdSet = capacityRequest.getResultList().stream().map(result -> result.getServerModel().getDcId()).collect(Collectors.toSet());

            for (String dcId : dcIdSet) {
                List<Result> resultListByDcId = capacityRequest.getResultList().stream().filter(result -> dcId.equals(result.getServerModel().getDcId())).collect(Collectors.toList());
                Set<String> typeSet = resultListByDcId.stream().map(result -> result.getServerModel().getHostType()).collect(Collectors.toSet());
                for (String type : typeSet) {
                    List<Result> resultListByDcIdAndType = capacityRequest.getResultList().stream().filter(result -> type.equals(result.getServerModel().getHostType())).collect(Collectors.toList());
                    ServerModel serverModel = new ServerModel();
                    serverModel.setDcId(dcId);
                    serverModel.setHostType(type);
                    serverModel.setVCpuQuantity(resultListByDcIdAndType.stream().filter(result -> 0 > result.getVcpuQty()).mapToInt(Result::getVcpuQty).sum());
                    serverModel.setRamQuantity(resultListByDcIdAndType.stream().filter(result -> 0 > result.getRamQty()).mapToInt(Result::getRamQty).sum());
                    Result resultNew = new Result();
                    resultNew.setServerModel(serverModel);
                    resultNew.setIncompatibleVmDedicatedComputeList(new ArrayList<>());
                    resultNew.setIncompatibleVmAntiAffinityGroup(new ArrayList<>());
                    Set<String> nameAAGSet = new HashSet<>();
                    for (Result result : resultListByDcIdAndType) {
                        resultNew.getIncompatibleVmDedicatedComputeList().addAll(result.getIncompatibleVmDedicatedComputeList());
                        nameAAGSet.addAll(result.getIncompatibleVmAntiAffinityGroup().stream().map(VmModel::getAntiAffinityGroup).collect(Collectors.toSet()));
                    }
                    for (String nameAAG : nameAAGSet) {
                        for (Result result : resultListByDcIdAndType) {
                            resultNew.getIncompatibleVmAntiAffinityGroup().addAll(result.getIncompatibleVmAntiAffinityGroup().stream()
                                    .filter(vmModel -> nameAAG.equals(vmModel.getAntiAffinityGroup()))
                                    .collect(Collectors.toList()));
                        }
                    }
                    newCapacityRequest.getResultList().add(resultNew);
                }
            }
            newCapacityRequestList.add(newCapacityRequest);
        }
        return newCapacityRequestList;
    }

    private List<ServerModel> getNewServerModel(List<CapacityRequest> capacityRequestList, Date date, String dcId, String type) {
        List<ServerModel> newServerModelList = new ArrayList<>();
        for (CapacityRequest capacityRequest : capacityRequestList) {
            if (capacityRequest.getDueDate().equals(date)) {
                for (Result result : capacityRequest.getResultList()) {
                    if (result.getServerModel().getDcId().equals(dcId) && result.getServerModel().getHostType().equals(type)) {
                        int serv = 0;
                        Set<String> nameSet = result.getIncompatibleVmAntiAffinityGroup().stream().map(VmModel::getAntiAffinityGroup).collect(Collectors.toSet());
                        List<List<VmModel>> vmList = new ArrayList<>();
                        for (String name : nameSet) {
                            vmList.add(result.getIncompatibleVmAntiAffinityGroup().stream().filter(vmModel -> name.equals(vmModel.getAntiAffinityGroup())).collect(Collectors.toList()));
                        }
                        serv = vmList.stream().mapToInt(List::size).max().orElse(0)
                                + result.getIncompatibleVmDedicatedComputeList().size();
                        if (serv == 0) {
                            newServerModelList.addAll(getServerModelByCpuOrRam(dcId,
                                    type,
                                    Math.abs(result.getServerModel().getVCpuQuantity()),
                                    Math.abs(result.getServerModel().getRamQuantity())
                                    ));
                        } else {

                        }

                    }
                }
            }

        }

        return newServerModelList;
    }

    public List<ServerModel> getServerModelByCpuOrRam(String dcId, String type, int vCpu, int ram) {
        int cpuServ = 0;
        int ramServ = 0;
        if ("SR_IOV".equals(type)) {
            cpuServ = 68;
            ramServ = 256;
        }
        if ("DPDK".equals(type)) {
            cpuServ = 56;
            ramServ = 256;
        }
        int i = (int) Double.max(Math.ceil(vCpu / cpuServ), Math.ceil(ram / ramServ));
        List<ServerModel> serverModelList = new ArrayList<>();
        while (i > 0) {
            i--;
            ServerModel serverModel = new ServerModel();
            serverModel.setDcId(dcId);
            serverModel.setHostType(type);
            serverModel.setVCpuQuantity(cpuServ);
            serverModel.setRamQuantity(ramServ);
            serverModelList.add(serverModel);

        }
        return serverModelList;
    }

    public List<ServerModel> getServerModel(String dcId, String type, int count) {
        int i = count;
        int cpuServ = 0;
        int ramServ = 0;
        if ("SR_IOV".equals(type)) {
            cpuServ = 68;
            ramServ = 256;
        }
        if ("DPDK".equals(type)) {
            cpuServ = 56;
            ramServ = 256;
        }
        List<ServerModel> serverModelList = new ArrayList<>();
        while (i > 0) {
            i--;
            ServerModel serverModel = new ServerModel();
            serverModel.setDcId(dcId);
            serverModel.setHostType(type);
            serverModel.setVCpuQuantity(cpuServ);
            serverModel.setRamQuantity(ramServ);
            serverModelList.add(serverModel);

        }
        return serverModelList;
    }

}
