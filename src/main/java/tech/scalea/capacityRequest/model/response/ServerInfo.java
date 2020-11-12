package tech.scalea.capacityRequest.model.response;

import lombok.Data;

@Data
public class ServerInfo {

    private String dcId;
    private String type;
    private Integer vCpu;
    private Integer ram;
    private Integer dedicatedExceededQuantity;
    private Integer capacityExceededQuantity;
    private String descriptionDedicatedGroups;
    private String descriptionAntiAffinityGroups;
    private String descriptionCpuRam;
}
