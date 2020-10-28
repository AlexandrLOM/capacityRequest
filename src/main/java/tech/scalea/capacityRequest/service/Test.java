package tech.scalea.capacityRequest.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tech.scalea.capacityRequest.repository.CapacityRequestItemRepository;

@Service
public class Test {

    private final CapacityRequestItemRepository capacityRequestItemRepository;

    @Autowired
    public Test(CapacityRequestItemRepository capacityRequestItemRepository) {
        this.capacityRequestItemRepository = capacityRequestItemRepository;
    }


    public void show(){
        System.out.println(capacityRequestItemRepository.findAll());
    }
}
