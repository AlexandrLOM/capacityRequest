package tech.scalea.capacityRequest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.scalea.capacityRequest.entity.InvExpansionRequestItemEntity;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExpansionRequestItemRepository extends JpaRepository<InvExpansionRequestItemEntity, UUID> {

    List<InvExpansionRequestItemEntity> findAllByInvExpansionRequestEntity_DueDateBeforeAndInvExpansionRequestEntity_InvDCAndHostType(Date date, String dcId, String hostType);

}
