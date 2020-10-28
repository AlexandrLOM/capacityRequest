package tech.scalea.capacityRequest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tech.scalea.capacityRequest.model.CapacityRequestInfo;
import tech.scalea.capacityRequest.model.ServerModel;
import tech.scalea.capacityRequest.model.VmModel;
import tech.scalea.capacityRequest.service.CapacityRequestService;
import tech.scalea.capacityRequest.service.DataPreparationService;

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
	void ran(){
		String dcId = "b630802c-c8d1-40f4-a38d-ecedb0209354";


		CapacityRequestInfo capacityRequestInfo = capacityRequestService.calculateCapacity(dataPreparationService.getServerModelList(dcId), dataPreparationService.getVmModelList(dcId));
		System.out.println(toDisplayString(capacityRequestInfo));

		System.out.println("THE END");
	}


	public static String toDisplayString(CapacityRequestInfo capacityRequestInfo) {
		StringBuilder displayString = new StringBuilder();

		if (capacityRequestInfo.getScore().isFeasible()) {
			for (VmModel process : capacityRequestInfo.getVmModelList()) {
				ServerModel computer = process.getServerModel();
				displayString.append("  ").append(process.toString()).append(" -> ")
						.append(computer == null ? null : computer.toString()).append("\n");
			}
		}else {
			displayString.append("Not feasible");
		}

		return displayString.toString();
	}
}
