package tech.scalea.capacityRequest.model.response;

import lombok.Data;
import tech.scalea.capacityRequest.entity.InvCapacityRequestEntity;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
public class Response {

    private Date dueDate;
    private UUID capacityRequest;
    private List<InvCapacityRequestEntity> invCapacityRequestEntityList;
    private List<ServerInfo> sererList;
}
