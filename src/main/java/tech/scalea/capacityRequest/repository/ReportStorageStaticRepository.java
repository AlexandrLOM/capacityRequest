package tech.scalea.capacityRequest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tech.scalea.capacityRequest.entity.ReportStorageStatic;

import java.util.Date;
import java.util.List;

@Repository
public interface ReportStorageStaticRepository extends JpaRepository<ReportStorageStatic, Long> {

    List<ReportStorageStatic> findAllByDcIdAndPoolTypeAndTimestamp(String dcId, String type, Date timestamp);

    @Query(value = "select max(timestamp) from report_storage_static where dc_id = :dcId",
            nativeQuery = true)
    Date findLastDateForDcId(String dcId);

    @Query(value = "select max(timestamp) from report_storage_static",
            nativeQuery = true)
    Date findLastDate();

}
