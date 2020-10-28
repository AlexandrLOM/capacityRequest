package tech.scalea.capacityRequest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.scalea.capacityRequest.entity.InvCapacityRequestItemEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface CapacityRequestItemRepository extends JpaRepository<InvCapacityRequestItemEntity, UUID> {

    List<InvCapacityRequestItemEntity> findAllByDcId(String dcId);

}
