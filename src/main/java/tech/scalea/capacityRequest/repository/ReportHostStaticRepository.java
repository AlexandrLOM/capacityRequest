package tech.scalea.capacityRequest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tech.scalea.capacityRequest.entity.ReportHostStatic;

import java.util.Date;
import java.util.List;

@Repository
public interface ReportHostStaticRepository extends JpaRepository<ReportHostStatic, Long> {

    List<ReportHostStatic> findAllByDcIdAndTimestamp(String dcId, Date timestamp);

    @Query(value = "select max(timestamp) from report_host_static where dc_id = :dcId",
            nativeQuery = true)
    Date findLastDateForDcId(String dcId);


}
