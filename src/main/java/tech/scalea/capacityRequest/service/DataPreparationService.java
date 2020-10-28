package tech.scalea.capacityRequest.service;

import tech.scalea.capacityRequest.model.ServerModel;
import tech.scalea.capacityRequest.model.VmModel;

import java.util.List;

public interface DataPreparationService {

    List<VmModel> getVmModelList(String dcId);

    List<ServerModel> getServerModelList(String dcId);
}
