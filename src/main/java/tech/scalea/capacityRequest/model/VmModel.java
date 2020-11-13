package tech.scalea.capacityRequest.model;

import lombok.Data;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

import java.util.UUID;

@PlanningEntity
@Data
public class VmModel {

    private String vmName;
    private UUID vmId;
    //private List<String> vmNameList;
    //private List<UUID> vmIdList;
    private String dcId;
    private String computeType;
    private String antiAffinityGroup;
    private String affinityGroup;
    private Integer vcpuQty;
    private Integer ramQty;
    private Integer storageQty;
    private boolean dedicatedCompute;

    // Planning variables: changes during planning, between score calculations.
    private ServerModel serverModel;

    public void setServerModel(ServerModel serverModel) {
        this.serverModel = serverModel;
    }

    @PlanningVariable(valueRangeProviderRefs = {"computerRange"})
    public ServerModel getServerModel() {
        return serverModel;
    }

    public int getRequiredMultiplicand() {
        return vcpuQty * ramQty;
    }
}
