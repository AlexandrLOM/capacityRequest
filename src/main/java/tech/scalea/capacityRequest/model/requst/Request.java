package tech.scalea.capacityRequest.model.requst;

import lombok.Data;
import tech.scalea.capacityRequest.entity.InvCapacityRequestEntity;

import java.util.Date;
import java.util.List;

@Data
public class Request {

    public Date dueDate;
    List<InvCapacityRequestEntity> invCapacityRequestEntityList;
    public List<Setver> sererList;
}
