package tech.scalea.capacityRequest.model;

import lombok.Data;
import tech.scalea.capacityRequest.entity.InvCapacityRequestEntity;

import java.util.Date;
import java.util.List;

@Data
public class CapacityRequest {

    public Date dueDate;
    List<InvCapacityRequestEntity> invCapacityRequestEntityList;
    List<Result> resultList;
    Integer solverHard;
    List<ServerModel> newServers;
//    List<Result> incompatibleVmAntiAffinityGroup;
//    List<Result> incompatibleVmDedicatedComputeList;
}
