package tech.scalea.capacityRequest.model;

import lombok.Data;

@Data
public class ServerModel {

    private long hostIdLong;
    private String hostId;
    private String hostName;
    private String dcId;
    private String hostType;
    private Integer vCpuQuantity;
    private Integer ramQuantity;
    private Integer vmCount;

    public int getMultiplicand() {
        return vCpuQuantity * ramQuantity;
    }

}
