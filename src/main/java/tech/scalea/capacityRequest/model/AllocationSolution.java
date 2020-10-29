package tech.scalea.capacityRequest.model;

import lombok.Data;
import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;

import java.util.List;

@Data
@PlanningSolution
public class AllocationSolution {

    private List<ServerModel> serverModelList;

    private List<VmModel> vmModelList;

    private HardSoftScore score;

    private long id;

    public AllocationSolution() {
    }

    public AllocationSolution(long id, List<ServerModel> serverModelList, List<VmModel> vmModelList) {
        this.id= id;
        this.serverModelList = serverModelList;
        this.vmModelList = vmModelList;
    }

    @ValueRangeProvider(id = "computerRange")
    @ProblemFactCollectionProperty
    public List<ServerModel> getServerModelList() {
        return serverModelList;
    }

    @PlanningEntityCollectionProperty
    public List<VmModel> getVmModelList() {
        return vmModelList;
    }

    @PlanningScore
    public HardSoftScore getScore() {
        return score;
    }
}
