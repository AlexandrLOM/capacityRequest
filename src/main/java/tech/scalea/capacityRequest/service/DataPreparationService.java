package tech.scalea.capacityRequest.service;

import tech.scalea.capacityRequest.entity.InvCapacityRequestEntity;
import tech.scalea.capacityRequest.model.CalculationData;
import tech.scalea.capacityRequest.model.ServerModel;
import tech.scalea.capacityRequest.model.VmModel;
import tech.scalea.capacityRequest.model.response.ServerInfo;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public interface DataPreparationService {

    List<VmModel> getVmModelListByCapacityRequestId(UUID id);

    List<ServerModel> getServerModelListByDcIdAndComputeType(String dcId, String computeType);

    CalculationData getVmModelListByCapacityRequestIdAndAllVMsBefore(UUID id);

    ServerInfo getServerInfoFromTemplate(String type);

    List<ServerModel> getServerModelExpansionRequestByDueDateAndDcIdAndHostType(Date dueDate, Date fromDate, String dcId, String hostType);

    Double getStorageExpansionRequest(Date dueDate, Date fromDate, String dcId, String type);

    Double getStorageServers(String dcId, String type);
}
