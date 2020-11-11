package tech.scalea.capacityRequest.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

@Data
@Entity
@Table(name = "t_host")
public class THostEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "template_id", nullable = false, length = 36)
    private UUID templateId;

//    @Column(name = "host_type", length = 100, nullable = false)
//    @Enumerated(EnumType.STRING)
//    private HostType hostType;

    @Column(name = "host_type", length = 100, nullable = false)
    private String hostType;

    @Column(name = "host_model", length = 100, unique = true)
    private String hostModel;

//    @Column(name = "host_vendor", length = 50, nullable = false)
//    @Enumerated(EnumType.STRING)
//    private HostVendor hostVendor;

    @Column(name = "host_vendor", length = 50, nullable = false)
    private String hostVendor;

    @Column(name = "cpu_qty")
    private Integer cpuQty;

    @Column(name = "ram_capacity")
    private Integer ramCapacity;

    @Column(name = "hdd_capacity")
    private Float hddCapacity;

}
