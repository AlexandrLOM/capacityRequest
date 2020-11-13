package tech.scalea.capacityRequest.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@Data
@Entity
@Table(name = "report_storage_static")
public class ReportStorageStatic implements Serializable {

    @Id
    @GeneratedValue(generator = "rpt_stor_st_seq", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "rpt_stor_st_seq", sequenceName = "rpt_stor_st_seq", allocationSize = 1)
    private long id;

    @Column(name = "dc_id")
    private String dcId;

    @Column(name = "pool_type")
    private String poolType;

    @Column(name = "pool_name")
    private String poolName;

    @Column(name = "pool_total")
    private Float poolTotal;

    @Column(name = "used")
    private Float used;

    @Column(name = "used_percent")
    private Float usedPercent;

    @Column(name = "available_percent")
    private Float availablePercent;

    @Column(name = "timestamp")
    private Date timestamp;

}
