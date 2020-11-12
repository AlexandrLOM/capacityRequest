package tech.scalea.capacityRequest.model.response;

import lombok.Data;

import java.util.Date;
import java.util.UUID;

@Data
public class Alert {

    private String category;
    private UUID capacityRequestId;
    private Date dueDate;
    private String dcid;
    private String hostType;
    private Integer hostCount;
    private String description;
}
