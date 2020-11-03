package tech.scalea.capacityRequest.model;

import lombok.Data;

import java.util.List;

@Data
public class Result {

    private ServerModel serverModel;
    private List<VmModel> vmModelList;
    private Integer vcpuQty;
    private Integer ramQty;
    private List<VmModel> incompatibleVmAntiAffinityGroup;

}
