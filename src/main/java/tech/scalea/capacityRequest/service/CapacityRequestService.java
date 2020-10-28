package tech.scalea.capacityRequest.service;

import tech.scalea.capacityRequest.model.CapacityRequestInfo;
import tech.scalea.capacityRequest.model.ServerModel;
import tech.scalea.capacityRequest.model.VmModel;

import java.util.List;

public interface CapacityRequestService {

    CapacityRequestInfo calculateCapacity(List<ServerModel> serverModelList, List<VmModel> vmModelList);


}
