package tech.scalea.capacityRequest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.scalea.capacityRequest.entity.InvCapacityRequestEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface CapacityRequestRepository extends JpaRepository<InvCapacityRequestEntity, UUID> {

    List<InvCapacityRequestEntity> findByOrderByDueDate();


}
