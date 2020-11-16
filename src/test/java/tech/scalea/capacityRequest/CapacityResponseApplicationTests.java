package tech.scalea.capacityRequest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tech.scalea.capacityRequest.entity.InvCapacityRequestEntity;
import tech.scalea.capacityRequest.model.response.Alert;
import tech.scalea.capacityRequest.model.response.Response;
import tech.scalea.capacityRequest.model.response.ServerInfo;
import tech.scalea.capacityRequest.service.CapacityRequestService;
import tech.scalea.capacityRequest.service.DataPreparationService;

import java.util.List;

import java.util.UUID;


@SpringBootTest
class CapacityResponseApplicationTests {

    @Autowired
    private DataPreparationService dataPreparationService;

    @Autowired
    private CapacityRequestService capacityRequestService;

    @Test
    void contextLoads() {
    }

    //@Test
    void startCapacityCalculationByCapacityRequestIdTest(){
        //UUID id = UUID.fromString("34a1e31d-5172-49d7-977d-b341c0240513");
        UUID id = UUID.fromString("34a1e31d-5172-49d7-977d-b341c0240514");
        Response response = capacityRequestService.startCapacityCalculationByCapacityRequestId(id);
        System.out.println(response.getDueDate());
        for (InvCapacityRequestEntity invCapacityRequestEntity : response.getInvCapacityRequestEntityList()){
            System.out.println(invCapacityRequestEntity.getId());
        }
        for(ServerInfo serverInfo : response.getSererList()){
            System.out.println(serverInfo);
        }
        System.out.println("---------------------------------------------------");
        System.out.println("Alerts:");
        List<Alert> alertList = capacityRequestService.getAlertList(response);
        for(Alert alert : alertList){
//            System.out.println("Alert:");
//            System.out.println("category: " + alert.getCategory());
//            System.out.println("due date: " + alert.getDueDate());
//            System.out.println("dc id: " + alert.getDcid());
//            System.out.println("host type: " + alert.getHostType());
//            System.out.println("host count: " + alert.getHostCount());
//            System.out.println("Description: " + alert.getDescription());
//            System.out.println("");
            System.out.println(alert);
        }
        System.out.println("---------------------------------------------------");
    }


}

