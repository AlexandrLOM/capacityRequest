package tech.scalea.capacityRequest.model;

import lombok.Data;
import tech.scalea.capacityRequest.enums.HostType;

@Data
public class ServerModel {

    private long hostIdLong;
    private String hostId;
    private String hostName;
    private String dcId;
    private HostType hostType;
    private Integer vCpuQuantity;
    private Integer ramQuantity;
    private Integer vmCount;

    public int getMultiplicand() {
        return vCpuQuantity * ramQuantity;
    }

}
