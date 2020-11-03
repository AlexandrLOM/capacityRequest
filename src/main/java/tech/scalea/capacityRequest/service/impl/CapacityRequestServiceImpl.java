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
                result.setIncompatibleVmAntiAffinityGroup(new ArrayList<>());
                result.setServerModel(vmModel.getServerModel());
                result.setVmModelList(new ArrayList<>());
                result.getVmModelList().add(vmModel);
                result.setVcpuQty(vmModel.getServerModel().getVCpuQuantity() - vmModel.getVcpuQty());
                result.setRamQty(vmModel.getServerModel().getRamQuantity() - vmModel.getRamQty());
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

    @Override
    public List<Result> analysisResults(List<VmModel> vmModelList) {
        List<Result> results = new ArrayList<>();
        Result result = new Result();
        result.setIncompatibleVmAntiAffinityGroup(new ArrayList<>());
        result.setVmModelList(vmModelList);
        result.setVcpuQty(vmModelList.stream().mapToInt(VmModel::getVcpuQty).sum());
        result.setRamQty(vmModelList.stream().mapToInt(VmModel::getRamQty).sum());
        results.add(result);
        return results;
    }

    @Override
    public List<CapacityRequest> capacityRequest() {
        List<CapacityRequest> capacityRequestsList = new ArrayList<>();

        List<InvCapacityRequestEntity> invCapacityRequestEntityList = dataPreparationService.getInvCapacityRequestEntityList();
        Set<Date> dueDateSet = invCapacityRequestEntityList.stream().map(InvCapacityRequestEntity::getDueDate).collect(Collectors.toSet());
        List<InvCapacityRequestEntity> invCapacityRequestEntities = new ArrayList<>();
        for (Date dueDate : dueDateSet) {
            CapacityRequest capacityRequest = new CapacityRequest();
            capacityRequest.setDueDate(dueDate);
            invCapacityRequestEntities.addAll(invCapacityRequestEntityList.stream().filter(invCapacityRequestEntity -> invCapacityRequestEntity.getDueDate().equals(dueDate)).collect(Collectors.toList()));
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
                }
            }
            capacityRequest.setResultList(resultList);
            capacityRequest = checkAntiAffinityGroup(capacityRequest);

            capacityRequestsList.add(capacityRequest);
        }

        return capacityRequestsList;
    }

}
