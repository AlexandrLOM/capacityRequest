package tech.scalea.capacityRequest.service;

import tech.scalea.capacityRequest.model.AllocationSolution;
import tech.scalea.capacityRequest.model.CapacityRequest;
import tech.scalea.capacityRequest.model.Result;
import tech.scalea.capacityRequest.model.ServerModel;
import tech.scalea.capacityRequest.model.VmModel;
import tech.scalea.capacityRequest.model.response.Alert;
import tech.scalea.capacityRequest.model.response.Response;

import java.util.List;
import java.util.UUID;

public interface CapacityRequestService {

    AllocationSolution startSolution(List<ServerModel> serverModelList, List<VmModel> vmModelList);

//    List<Result> analysisResults(AllocationSolution calculateCapacity);

//    List<CapacityRequest> capacityRequest(List<CapacityRequest> newCapacityRequest);

//    List<Result> analysisResults(List<VmModel> vmModelList);

//    List<CapacityRequest> calculateRequiredNumberOfServers(List<CapacityRequest> capacityRequestList);

    Response startCapacityCalculationByCapacityRequestId(UUID id);

    List<Alert> getAlertList(Response response);

}
