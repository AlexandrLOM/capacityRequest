package tech.scalea.capacityRequest.service.impl;

import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.springframework.stereotype.Service;
import tech.scalea.capacityRequest.model.AllocationSolution;
import tech.scalea.capacityRequest.model.ServerModel;
import tech.scalea.capacityRequest.model.VmModel;
import tech.scalea.capacityRequest.service.CapacityRequestService;

import java.util.List;

@Service
public class CapacityRequestServiceImpl implements CapacityRequestService {

    @Override
    public AllocationSolution calculateCapacity(List<ServerModel> serverModelList, List<VmModel> vmModelList) {

        // Build the Solver
        SolverFactory<AllocationSolution> solverFactory = SolverFactory.createFromXmlResource("allocationSolutionSolverConfig.xml");
        Solver<AllocationSolution> solver = solverFactory.buildSolver();

        AllocationSolution unsolvedCapacityRequestInf = new AllocationSolution();
        unsolvedCapacityRequestInf.setServerModelList(serverModelList);
        unsolvedCapacityRequestInf.setVmModelList(vmModelList);

        // Solve the problem
        AllocationSolution solvedCapacityRequestInf = solver.solve(unsolvedCapacityRequestInf);

        return solvedCapacityRequestInf;

    }
}
