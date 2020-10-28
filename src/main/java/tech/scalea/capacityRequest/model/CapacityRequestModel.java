package tech.scalea.capacityRequest.model;

import lombok.Data;

import java.util.List;

@Data
public class CapacityRequestModel {

    private List<ServerModel> serverModelList;
    private List<VmModel> vmModelList;

}
