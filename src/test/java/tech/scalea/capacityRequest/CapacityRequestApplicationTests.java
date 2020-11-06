package tech.scalea.capacityRequest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tech.scalea.capacityRequest.entity.InvCapacityRequestEntity;
import tech.scalea.capacityRequest.enums.HostType;
import tech.scalea.capacityRequest.model.AllocationSolution;
import tech.scalea.capacityRequest.model.CapacityRequest;
import tech.scalea.capacityRequest.model.Result;
import tech.scalea.capacityRequest.model.ServerModel;
import tech.scalea.capacityRequest.model.VmModel;
import tech.scalea.capacityRequest.service.CapacityRequestService;
import tech.scalea.capacityRequest.service.DataPreparationService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
        for (Result result : resultList) {
            System.out.println(result.getServerModel().getHostName());
            System.out.println("CPU - " + result.getVcpuQty());
            System.out.println("RAM - " + result.getRamQty());
            System.out.println("VM - " + result.getVmModelList().size());
        }


        System.out.println("THE END");
    }

    @Test
    public void capacityRequest() {
        List<CapacityRequest> capacityRequestList = capacityRequestService.capacityRequest(new ArrayList<>());
        showResult(capacityRequestList);
//        capacityRequestList = capacityRequestService.calculateRequiredNumberOfServers(capacityRequestList);
//        for (CapacityRequest capacityRequest : capacityRequestList) {
//            if (capacityRequest.getNewServers() != null) capacityRequest.getNewServers().forEach(System.out::println);
//        }

//        while (capacityRequestList.stream().mapToInt(CapacityRequest::getSolverHard).sum() != 0){
//        capacityRequestList = capacityRequestService.capacityRequest(capacityRequestList);
//        showResult(capacityRequestList);
//        capacityRequestList = capacityRequestService.calculateRequiredNumberOfServers(capacityRequestList);
//        for (CapacityRequest capacityRequest : capacityRequestList) {
//            if (capacityRequest.getNewServers() != null) capacityRequest.getNewServers().forEach(System.out::println);
//        }
//        capacityRequestList = capacityRequestService.capacityRequest(capacityRequestList);
//       }

//        showResult(capacityRequestList);
//        capacityRequestList = capacityRequestService.calculateRequiredNumberOfServers(capacityRequestList);
//        for (CapacityRequest capacityRequest : capacityRequestList) {
//            if (capacityRequest.getNewServers() != null) capacityRequest.getNewServers().forEach(System.out::println);
//        }
//        capacityRequestList = capacityRequestService.capacityRequest(capacityRequestList);
//        showResult(capacityRequestList);
        System.out.println("THE END!");

    }

    public void showResult(List<CapacityRequest> capacityRequestList) {
        for (CapacityRequest capacityRequest : capacityRequestList) {
            System.out.println("-----------------------------------------------------------------------");
            System.out.println("Due date - " + capacityRequest.getDueDate());
            System.out.println("Capacity requests:");
            for(InvCapacityRequestEntity invCapacityRequestEntity : capacityRequest.getInvCapacityRequestEntityList()){
                System.out.println("ID: " + invCapacityRequestEntity.getId() + ", name:" + invCapacityRequestEntity.getRequestName());

            }

            Set<String> dcIdSet = capacityRequest.getResultList().stream().map(result -> result.getServerModel().getDcId()).collect(Collectors.toSet());
            for (String dcId : dcIdSet) {
                List<Result> resultListByDcId = capacityRequest.getResultList().stream().filter(result -> dcId.equals(result.getServerModel().getDcId())).collect(Collectors.toList());
                Set<String> typeSet = resultListByDcId.stream().map(result -> result.getServerModel().getHostType()).collect(Collectors.toSet());
                for (String type : typeSet) {
                    int serversNew = 0;
                    List<Result> resultListByDcIdAndType = resultListByDcId.stream().filter(result -> type.equals(result.getServerModel().getHostType())).collect(Collectors.toList());
                    System.out.println("");
                    System.out.println("Got allocationSolution for DC_id: " + dcId + ", host_type: " + type);
                    int vmsAag = 0;
                    for (Result result : resultListByDcIdAndType) {
                        vmsAag = vmsAag + result.getVmModelList().size() + result.getIncompatibleVmAntiAffinityGroup().size() + result.getIncompatibleVmDedicatedComputeList().size();
                    }
                    System.out.println("HOSTs: " + resultListByDcIdAndType.size()
                            + ", VMs " + vmsAag);
                    int cpu = resultListByDcIdAndType.stream().filter(result -> result.getVcpuQty() < 0).mapToInt(Result::getVcpuQty).sum();
                    if (cpu < 0) System.out.println("Total Capacity Exceeding, vCPU: " + cpu);
                    int ram = resultListByDcIdAndType.stream().filter(result -> result.getRamQty() < 0).mapToInt(Result::getRamQty).sum();
                    if (ram < 0) System.out.println("Total Capacity Exceeding, RAM: " + ram);
                    for (Result result : resultListByDcIdAndType) {
                        int cpu1 = 0;
                        int ram1 = 0;
                        if (result.getVcpuQty() < 0) cpu1 = result.getVcpuQty();
                        if (result.getRamQty() < 0) ram1 = result.getRamQty();
                        if (cpu1 < 0 || ram1 < 0) {
                            //System.out.println("Host id: " + result.getServerModel().getHostIdLong() + ", Capacity Exceeding, vCPU: " + cpu1 + ", RAM: " + ram1);
                        }
                    }
                    int df = resultListByDcIdAndType.stream().mapToInt(result -> result.getIncompatibleVmDedicatedComputeList().size()).sum();
                    System.out.println("Dedicated category: " + df);
                    for (Result result : resultListByDcIdAndType) {
                        for (VmModel vmModel : result.getIncompatibleVmDedicatedComputeList()) {
                            //System.out.println("mv id: " + vmModel.getVmId() + ", vCPU: " + vmModel.getVcpuQty() + ", RAM: " + vmModel.getRamQty());
                            //System.out.println("mv id: " + vmModel.getVmId() + ", vCPU: " + vmModel.getVcpuQty() + ", RAM: " + vmModel.getRamQty());
                        }
                    }
                    System.out.println("Anti-Affinity category:");
                    Set<String> groupSet = new HashSet<>();
                    for (Result result : resultListByDcIdAndType) {
                        groupSet.addAll(result.getIncompatibleVmAntiAffinityGroup().stream().map(VmModel::getAntiAffinityGroup).collect(Collectors.toSet()));
                    }
                    for (String group : groupSet) {
                        int vms = 0;
                        int vmAll = 0;
                        for (Result result : resultListByDcIdAndType) {
                            vms = vms + (int) result.getIncompatibleVmAntiAffinityGroup().stream().filter(vmModel -> group.equals(vmModel.getAntiAffinityGroup())).count();
                            vmAll = vmAll + (int) result.getVmModelList().stream().filter(vmModel -> group.equals(vmModel.getAntiAffinityGroup())).count();
                        }
                        vmAll = vmAll + vms;
                        System.out.println("Name: " + group + ", VMs: " + vms);
                        //System.out.println("Name: " + group + ", VMs " + vms + " from " + vmAll);
                        if (serversNew < vms) serversNew = vms;

                    }
                    serversNew = serversNew + df;
                    if(serversNew == 0){
                        double cpuNew = 0;
                        double ramNew = 0;

                        if("SR_IOV".equals(type)){
                            cpuNew = 68;
                            ramNew = 256;
                        } else if ("DPDK".equals(type)){
                            cpuNew = 56;
                            ramNew = 256;
                        }

                        serversNew = (int)Math.ceil(Double.max(Math.abs(cpu)/cpuNew, Math.abs(ram)/ramNew));
                    }

                    System.out.println("Estimated servers count: " + serversNew);
                }

            }
        }
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

