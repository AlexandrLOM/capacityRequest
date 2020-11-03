package tech.scalea.capacityRequest.service;

import tech.scalea.capacityRequest.entity.InvCapacityRequestEntity;
import tech.scalea.capacityRequest.model.ServerModel;
import tech.scalea.capacityRequest.model.VmModel;

import java.util.List;
import java.util.UUID;

public interface DataPreparationService {

    List<VmModel> getVmModelListByDcId(String dcId);

    List<ServerModel> getServerModelListByDcId(String dcId);

    List<String> getDcIdListAllCapacityRequestItems();

    List<VmModel> getVmModelListByCapacityRequestId(UUID id);

    List<ServerModel> getServerModelList();

    List<InvCapacityRequestEntity> getInvCapacityRequestEntityList();

    List<ServerModel> getServerModelListByDcIdAndComputeType(String dcId, String computeType);
}
