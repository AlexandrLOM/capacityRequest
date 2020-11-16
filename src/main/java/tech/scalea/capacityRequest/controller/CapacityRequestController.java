package tech.scalea.capacityRequest.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import tech.scalea.capacityRequest.model.response.Alert;
import tech.scalea.capacityRequest.model.response.Response;
import tech.scalea.capacityRequest.service.CapacityRequestService;

import java.util.List;
import java.util.UUID;

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/v1/cre")
public class CapacityRequestController {

    private static final Logger logger = LoggerFactory.getLogger(CapacityRequestController.class);

    private CapacityRequestService capacityRequestService;

    @Autowired
    public CapacityRequestController(CapacityRequestService capacityRequestService) {
        this.capacityRequestService = capacityRequestService;
    }

    @GetMapping(path = "/capacity_evaluation", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Alert> capacityEvaluation(@RequestParam(required = false) UUID requestId){
        logger.debug("Capacity evaluation, requestId: {}", requestId);

        Response response = capacityRequestService.startCapacityCalculationByCapacityRequestId(requestId);

        List<Alert> alertList = capacityRequestService.getAlertList(response);

        return alertList;
    }
}
