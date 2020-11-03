package tech.scalea.capacityRequest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tech.scalea.capacityRequest.entity.InvCapacityRequestItemEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface CapacityRequestItemRepository extends JpaRepository<InvCapacityRequestItemEntity, UUID> {

    List<InvCapacityRequestItemEntity> findAllByDcId(String dcId);

    @Query(value = "select dc_id from dp.inv_capacity_request_item group by dc_id",
    nativeQuery = true)
    List<String> findDcIdList();

    List<InvCapacityRequestItemEntity> findAllByInvCapacityRequestEntity_Id(UUID id);

}
