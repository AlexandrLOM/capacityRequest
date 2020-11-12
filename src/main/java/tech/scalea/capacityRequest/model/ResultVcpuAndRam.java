package tech.scalea.capacityRequest.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResultVcpuAndRam {

    private int vCpu;
    private int ram;
    private int hostCount;
}
