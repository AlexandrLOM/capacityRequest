package tech.scalea.capacityRequest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.scalea.capacityRequest.entity.InvVmEntity;

@Repository
public interface InvVmRepository extends JpaRepository<InvVmEntity, Long> {

    int countByHostId(String hostId);

}
