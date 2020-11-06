package tech.scalea.capacityRequest.model.requst;

import lombok.Data;

@Data
public class Setver {
    private String dcId;
    private String type;
    private Integer vCpu;
    private Integer ram;
    private Integer quantity;
}
