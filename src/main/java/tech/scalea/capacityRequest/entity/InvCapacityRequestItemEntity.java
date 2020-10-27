package tech.scalea.capacityRequest.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.UUID;

@Data
@Entity
@Table(name = "inv_capacity_request_item", schema = "dp")
public class InvCapacityRequestItemEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "request_id")
    private InvCapacityRequestEntity invCapacityRequestEntity;

    @Column(name = "item_number")
    private Integer itemNumber;

//    @ManyToOne
//    @JoinColumn(name = "dc_id", referencedColumnName = "dc_id")
//    private InvDcEntity invDC;

    @Column(name = "dc_id")
    private String dcId;

    @Column(name = "vm_name")
    private String vmName;

//    @ManyToOne
//    @JoinColumn(name = "vnf_id", referencedColumnName = "vnf_id")
//    private InvVnfEntity invVNF;

    @Column(name = "vnf_id")
    private String vnfId;

    @Column(name = "vm_type")
    private String vmType;

    @Column(name = "vm_qty")
    private Integer vmQty;

    @Column(name = "guest_os")
    private String guestOs;

    @Column(name = "vcpu_qty")
    private Integer vcpuQty;

    @Column(name = "cpu_overcommit")
    private String cpuOvercommit;

    @Column(name = "ram_qty")
    private Integer ramQty;

    @Column(name = "mem_overcommit")
    private String memOvercommit;

    @Column(name = "local_storage_qty")
    private Integer localStorageQty;

    @Column(name = "numa_required")
    private String numaRequired;

    @Column(name = "virtio_if_qty")
    private Integer virtioIfQty;

    @Column(name = "sriov_if_qty")
    private Integer sriovIfQty;

    @Column(name = "dpdk_if_qty")
    private Integer dpdkIfQty;

    @Column(name = "hyperthreading")
    private String hyperthreading;

    @Column(name = "hugepage_size")
    private Integer hugepageSize;

    @Column(name = "hugepage_qty")
    private Integer hugepageQty;

    @Column(name = "compute_ha_notes")
    private String computeHaNotes;

    @Column(name = "compute_type")
    private String computeType;

    @Column(name = "anti_affinity_group")
    private String antiAffinityGroup;

    @Column(name = "add_comp_req")
    private String addCompReq;

    @Column(name = "block_storage")
    private Integer blockStorage;

    @Column(name = "image_size")
    private Integer imageSize;

    @Column(name = "object_storage")
    private Integer objectStorage;

    @Column(name = "storage_iops")
    private Integer storageIops;

    @Column(name = "iops_max_read")
    private Integer iopsMaxRead;

    @Column(name = "iops_max_write")
    private Integer iopsMaxWrite;

    @Column(name = "read_write_ratio")
    private Integer readWriteRatio;

    @Column(name = "storage_ha_notes")
    private String storageHaNotes;

    @Column(name = "storage_type")
    private String storageType;

    @Column(name = "add_storage_req")
    private String addStorageReq;

    @Column(name = "other_notes")
    private String otherNotes;

    @Column(name = "affinity_group")
    private String affinityGroup;

    @Column(name = "dedicated_compute")
    private String dedicatedCompute;
}
