package tech.scalea.capacityRequest.entity;

import lombok.Data;
import tech.scalea.capacityRequest.enums.HostStatus;
import tech.scalea.capacityRequest.enums.HostType;
import tech.scalea.capacityRequest.enums.HostVendor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

@Data
@Entity
@Table(name = "report_host_static")
public class ReportHostStatic implements Serializable {

    @Id
    @GeneratedValue(generator = "rpt_host_static_seq", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "rpt_host_static_seq", sequenceName = "rpt_host_static_seq", allocationSize = 1)
    private long id;

    @Column(name = "host_id")
    private String hostId;

    @Column(name = "host_name")
    private String hostName;

    @Column(name = "dc_id")
    private String dcId;

//    @Column(name = "host_type")
//    @Enumerated(EnumType.STRING)
//    private HostType hostType;

    private String hostType;

    @Column(name = "host_vendor")
    @Enumerated(EnumType.STRING)
    private HostVendor hostVendor;

    @Column(name = "rack_name")
    private String rackName;

    @Column(name = "alloc_cpu_qty")
    private Integer allocCpuQty;

    @Column(name = "alloc_ram_qty")
    private Long allocRamQty;

    @Column(name = "alloc_hdd_qty")
    private Float allocHddQty;

    @Column(name = "total_cpu_qty")
    private Integer totalCpuQty;

    @Column(name = "total_ram_qty")
    private Integer totalRamQty;

    @Column(name = "total_hdd_qty")
    private Float totalHddQty;

    @Column(name = "alloc_cpu_qty_free")
    private Integer allocCpuQtyFree;

    @Column(name = "alloc_ram_qty_free")
    private Long allocRamQtyFree;

    @Column(name = "alloc_hdd_qty_free")
    private Float allocHddQtyFree;

    @Column(name = "alloc_cpu_percent")
    private Float allocCpuPercent;

    @Column(name = "alloc_ram_percent")
    private Float allocRamPercent;

    @Column(name = "alloc_hdd_percent")
    private Float allocHddPercent;

    @Column(name = "alloc_cpu_percent_free")
    private Float allocCpuPercentFree;

    @Column(name = "alloc_ram_percent_free")
    private Float allocRamPercentFree;

    @Column(name = "alloc_hdd_percent_free")
    private Float allocHddPercentFree;

    @Column(name = "numa0_alloc_cpu_qty")
    private Integer numA0AllocCpuQty;

    @Column(name = "numa0_avail_cpu_qty")
    private Integer numA0AvailCpuQty;

    @Column(name = "numa0_alloc_ram_qty")
    private Integer numA0AllocRamQty;

    @Column(name = "numa0_avail_ram_qty")
    private Integer numA0AvailRamQty;

    @Column(name = "numa1_alloc_cpu_qty")
    private Integer numA1AllocCpuQty;

    @Column(name = "numa1_avail_cpu_qty")
    private Integer numA1AvailCpuQty;

    @Column(name = "numa1_alloc_ram_qty")
    private Integer numA1AllocRamQty;

    @Column(name = "numa1_avail_ram_qty")
    private Integer numA1AvailRamQty;

    @Column(name = "numa0_alloc_cpu_percent")
    private Float numA0AllocCpuPercent;

    @Column(name = "numa0_avail_cpu_percent")
    private Float numA0AvailCpuPercent;

    @Column(name = "numa0_alloc_ram_percent")
    private Float numA0AllocRamPercent;

    @Column(name = "numa0_avail_ram_percent")
    private Float numA0AvailRamPercent;

    @Column(name = "numa1_alloc_cpu_percent")
    private Float numA1AllocCpuPercent;

    @Column(name = "numa1_avail_cpu_percent")
    private Float numA1AvailCpuPercent;

    @Column(name = "numa1_alloc_ram_percent")
    private Float numA1AllocRamPercent;

    @Column(name = "numa1_avail_ram_percent")
    private Float numA1AvailRamPercent;

    @Column(name = "vm_qty")
    private Integer vmQty;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private HostStatus status;

    @Column(name = "timestamp")
    private Date timestamp;

}
