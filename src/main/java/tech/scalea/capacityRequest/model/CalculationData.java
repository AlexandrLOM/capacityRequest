package tech.scalea.capacityRequest.model;

import lombok.Data;
import tech.scalea.capacityRequest.entity.InvCapacityRequestEntity;

import java.util.Date;
import java.util.List;

@Data
public class CalculationData {

    private Date dueDate;
    private Date fromDate;
    private List<InvCapacityRequestEntity> invCapacityRequestEntityList;
    private List<VmModel> vmModelList;
}
