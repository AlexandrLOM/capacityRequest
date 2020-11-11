package tech.scalea.capacityRequest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.scalea.capacityRequest.entity.THostEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface THostRepository extends JpaRepository<THostEntity, UUID> {

    Optional<THostEntity> findByHostType(String hostType);

    List<THostEntity> findAllByHostType(String hostType);

}
