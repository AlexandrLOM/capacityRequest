package tech.scalea.capacityRequest.service;

import tech.scalea.capacityRequest.model.AllocationSolution;
import tech.scalea.capacityRequest.model.CapacityRequest;
import tech.scalea.capacityRequest.model.Result;
import tech.scalea.capacityRequest.model.ServerModel;
import tech.scalea.capacityRequest.model.VmModel;

import java.util.List;

public interface CapacityRequestService {

    AllocationSolution calculateCapacity(List<ServerModel> serverModelList, List<VmModel> vmModelList);

    List<Result> analysisResults(AllocationSolution calculateCapacity);

    List<CapacityRequest> capacityRequest(List<CapacityRequest> newCapacityRequest);

    List<Result> analysisResults(List<VmModel> vmModelList);

    List<CapacityRequest> calculateRequiredNumberOfServers(List<CapacityRequest> capacityRequestList);

}
