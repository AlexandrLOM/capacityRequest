package tech.scalea.capacityRequest.service;

import tech.scalea.capacityRequest.model.AllocationSolution;
import tech.scalea.capacityRequest.model.ServerModel;
import tech.scalea.capacityRequest.model.VmModel;

import java.util.List;

public interface CapacityRequestService {

    AllocationSolution calculateCapacity(List<ServerModel> serverModelList, List<VmModel> vmModelList);


}
