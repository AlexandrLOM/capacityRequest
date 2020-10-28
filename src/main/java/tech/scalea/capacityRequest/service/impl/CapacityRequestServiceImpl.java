package tech.scalea.capacityRequest.service.impl;

import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.springframework.stereotype.Service;
import tech.scalea.capacityRequest.model.CapacityRequestInfo;
import tech.scalea.capacityRequest.model.ServerModel;
import tech.scalea.capacityRequest.model.VmModel;
import tech.scalea.capacityRequest.service.CapacityRequestService;

import java.util.List;

@Service
public class CapacityRequestServiceImpl implements CapacityRequestService {

    @Override
    public CapacityRequestInfo calculateCapacity(List<ServerModel> serverModelList, List<VmModel> vmModelList) {

        // Build the Solver
        SolverFactory<CapacityRequestInfo> solverFactory = SolverFactory.createFromXmlResource("cloudBalancingSolverConfig.xml");
        Solver<CapacityRequestInfo> solver = solverFactory.buildSolver();

        CapacityRequestInfo unsolvedCapacityRequestInf = new CapacityRequestInfo();
        unsolvedCapacityRequestInf.setServerModelList(serverModelList);
        unsolvedCapacityRequestInf.setVmModelList(vmModelList);

        // Solve the problem
        CapacityRequestInfo solvedCapacityRequestInf = solver.solve(unsolvedCapacityRequestInf);

        return solvedCapacityRequestInf;

    }
}
