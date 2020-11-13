package tech.scalea.capacityRequest.service;

import tech.scalea.capacityRequest.model.AllocationSolution;
import tech.scalea.capacityRequest.model.ServerModel;
import tech.scalea.capacityRequest.model.VmModel;
import tech.scalea.capacityRequest.model.response.Alert;
import tech.scalea.capacityRequest.model.response.Response;

import java.util.List;
import java.util.UUID;

public interface CapacityRequestService {

    AllocationSolution startSolution(List<ServerModel> serverModelList, List<VmModel> vmModelList);

    Response startCapacityCalculationByCapacityRequestId(UUID id);

    List<Alert> getAlertList(Response response);

}
