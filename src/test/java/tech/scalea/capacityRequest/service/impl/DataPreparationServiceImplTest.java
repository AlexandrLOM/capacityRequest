package tech.scalea.capacityRequest.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;

@SpringBootTest
class DataPreparationServiceImplTest {

    @Autowired
    private DataPreparationServiceImpl dataPreparationService;

    //@Test
    void formatDate() {

        Date dateIn = new Date();
        Date dateOut = new Date();
        System.out.println(dateIn);
        Date newDate = dataPreparationService.formatDate(dateIn, 1);
        System.out.println(newDate);

    }
}
