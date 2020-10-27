package tech.scalea.capacityRequest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.scalea.capacityRequest.entity.InvCapacityRequestEntity;

import java.util.UUID;

@Repository
public interface CapacityRequestRepository extends JpaRepository<InvCapacityRequestEntity, UUID> {


}
