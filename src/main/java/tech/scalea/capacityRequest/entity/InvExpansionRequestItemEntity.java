package tech.scalea.capacityRequest.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.UUID;

@Data
@Entity
@Table(schema = "dp", name = "inv_expansion_request_item")
public class InvExpansionRequestItemEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "request_id", referencedColumnName = "id")
    private InvExpansionRequestEntity invExpansionRequestEntity;

    @Column(name = "item_number")
    private Integer itemNumber;

//    @Column(name = "host_type")
//    @Enumerated(EnumType.STRING)
//    private HostType hostType;

    @Column(name = "host_type")
    private String hostType;

    @Column(name = "host_model")
    private String hostModel;

//    @Column(name = "host_vendor")
//    @Enumerated(EnumType.STRING)
//    private HostVendor hostVendor;

    @Column(name = "host_vendor")
    private String hostVendor;

    @Column(name = "cpu_qty")
    private Integer cpuQty;

    @Column(name = "ram_capacity")
    private Double ramCapacity;

    @Column(name = "hdd_capacity")
    private Double hddCapacity;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "compute_type")
    private String computeType;

    @Column(name = "storage_type")
    private String storageType;
}
