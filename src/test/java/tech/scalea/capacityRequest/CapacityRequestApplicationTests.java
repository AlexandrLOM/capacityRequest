package tech.scalea.capacityRequest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tech.scalea.capacityRequest.enums.HostType;
import tech.scalea.capacityRequest.model.AllocationSolution;
import tech.scalea.capacityRequest.model.Result;
import tech.scalea.capacityRequest.model.ServerModel;
import tech.scalea.capacityRequest.model.VmModel;
import tech.scalea.capacityRequest.service.CapacityRequestService;
import tech.scalea.capacityRequest.service.DataPreparationService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SpringBootTest
class CapacityRequestApplicationTests {

    @Autowired
    private DataPreparationService dataPreparationService;

    @Autowired
    private CapacityRequestService capacityRequestService;

    @Test
    void contextLoads() {
    }

    @Test
    void runByDc() {
        String dcId = "b630802c-c8d1-40f4-a38d-ecedb0209354";

        System.out.println("START");

        AllocationSolution allocationSolution = capacityRequestService.calculateCapacity(dataPreparationService.getServerModelListByDcId(dcId), dataPreparationService.getVmModelListByDcId(dcId));
        System.out.println(toDisplayString(allocationSolution));

        System.out.println("THE END");
    }

    @Test
    void runAllCapacityRequest() {

        System.out.println("START");

        List<String> dcIdList = dataPreparationService.getDcIdListAllCapacityRequestItems();
        for (String dcId : dcIdList) {
            System.out.println("---------------------------------------------------------");
            try {
                AllocationSolution allocationSolution = capacityRequestService.calculateCapacity(dataPreparationService.getServerModelListByDcId(dcId), dataPreparationService.getVmModelListByDcId(dcId));
                System.out.println(toDisplayString(allocationSolution));
//                System.out.println("best score:");
//                System.out.println("hard:" + allocationSolution.getScore().getHardScore());
//                System.out.println("soft:" + allocationSolution.getScore().getSoftScore());
            } catch (Exception e) {
                System.out.println("ERROR");
                System.out.println(e);
            }

            System.out.println("---------------------------------------------------------");
        }

        System.out.println("THE END");
    }

    @Test
    void runByCapacityRequest() {
        UUID requestId = UUID.fromString("34a1e31d-5172-49d7-977d-b341c0240514");

        List<ServerModel> serverModelList = dataPreparationService.getServerModelList();
        List<VmModel> vmModelList = dataPreparationService.getVmModelListByCapacityRequestId(requestId);

        AllocationSolution allocationSolution = capacityRequestService.calculateCapacity(serverModelList, vmModelList);
        System.out.println(toDisplayString(allocationSolution));

    }


    @Test
    void runMockData() {

        List<ServerModel> serverModelList = new ArrayList<>();

        ServerModel serverModel1 = new ServerModel();
        serverModel1.setRamQuantity(50);
        serverModel1.setVCpuQuantity(20);
        serverModel1.setHostName("S-01");
        serverModel1.setHostType(HostType.Server.getStringValue());
        serverModelList.add(serverModel1);

        ServerModel serverModel2 = new ServerModel();
        serverModel2.setRamQuantity(20);
        serverModel2.setVCpuQuantity(5);
        serverModel2.setHostName("S-02");
        serverModel2.setHostType(HostType.Server.getStringValue());
        serverModelList.add(serverModel2);

        ServerModel serverModel3 = new ServerModel();
        serverModel3.setRamQuantity(20);
        serverModel3.setVCpuQuantity(5);
        serverModel3.setHostName("S-03");
        serverModel3.setHostType(HostType.Mgt.getStringValue());
        serverModelList.add(serverModel3);

        ServerModel serverModel4 = new ServerModel();
        serverModel4.setRamQuantity(20);
        serverModel4.setVCpuQuantity(5);
        serverModel4.setHostName("S-03");
        serverModel4.setHostType(HostType.DCGW.getStringValue());
        serverModelList.add(serverModel4);


        List<VmModel> vmModelList = new ArrayList<>();

        VmModel vmMode1 = new VmModel();
        vmMode1.setVmName("vm-01");
        vmMode1.setRamQty(20);
        vmMode1.setVcpuQty(5);
        vmMode1.setComputeType(HostType.Server.getStringValue());
        vmMode1.setAntiAffinityGroup("AntiAff-01");
        vmMode1.setDedicatedCompute(false);
        vmModelList.add(vmMode1);

        VmModel vmMode2 = new VmModel();
        vmMode2.setVmName("vm-02");
        vmMode2.setRamQty(500);
        vmMode2.setVcpuQty(10);
        vmMode2.setComputeType(HostType.Server.getStringValue());
        vmMode2.setAntiAffinityGroup("AntiAff-01");
        vmMode2.setDedicatedCompute(false);
        vmModelList.add(vmMode2);

        VmModel vmMode3 = new VmModel();
        vmMode3.setVmName("vm-03");
        vmMode3.setRamQty(3);
        vmMode3.setVcpuQty(5);
        vmMode3.setComputeType(HostType.Server.getStringValue());
        vmMode3.setAntiAffinityGroup("AntiAff-02");
        vmMode3.setDedicatedCompute(false);
        vmModelList.add(vmMode3);

        VmModel vmMode4 = new VmModel();
        vmMode4.setVmName("vm-04");
        vmMode4.setRamQty(1);
        vmMode4.setVcpuQty(3);
        vmMode4.setComputeType(HostType.Mgt.getStringValue());
        vmMode4.setAntiAffinityGroup("AntiAff-04");
        vmMode4.setDedicatedCompute(false);
        vmModelList.add(vmMode4);

        VmModel vmMode5 = new VmModel();
        vmMode5.setVmName("vm-04");
        vmMode5.setRamQty(1);
        vmMode5.setVcpuQty(3);
        vmMode5.setComputeType(HostType.Leaf.getStringValue());
        vmMode5.setAntiAffinityGroup("AntiAff-05");
        vmMode5.setDedicatedCompute(false);
        vmModelList.add(vmMode5);

        System.out.println("START");

        AllocationSolution allocationSolution = capacityRequestService.calculateCapacity(serverModelList, vmModelList);
        System.out.println(toDisplayString(allocationSolution));
        List<Result> resultList = capacityRequestService.analysisResults(allocationSolution);
        for(Result result : resultList){
            System.out.println(result.getServerModel().getHostName());
            System.out.println("CPU - " + result.getVcpuQty());
            System.out.println("RAM - " + result.getRamQty());
            System.out.println("VM - " + result.getVmModelList().size());
        }


        System.out.println("THE END");
    }

    @Test
    public void capacityRequest(){
        System.out.println(capacityRequestService.capacityRequest());
    }


    public static String toDisplayString(AllocationSolution allocationSolution) {
        StringBuilder displayString = new StringBuilder();
        System.out.println("best score:");
        System.out.println("            hard:" + allocationSolution.getScore().getHardScore());
        System.out.println("            soft:" + allocationSolution.getScore().getSoftScore());
        if (allocationSolution.getScore().isFeasible()) {
            for (VmModel process : allocationSolution.getVmModelList()) {
                ServerModel computer = process.getServerModel();
                displayString.append("  ").append(process.toString()).append(" -> ")
                        .append(computer == null ? null : computer.toString()).append("\n");
            }
        } else {
            displayString.append("Not feasible");
        }

        return displayString.toString();
    }
}

