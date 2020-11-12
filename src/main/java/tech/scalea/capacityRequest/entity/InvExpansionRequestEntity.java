package tech.scalea.capacityRequest.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.UUID;

@Data
@Entity
@Table(schema = "dp", name = "inv_expansion_request")
public class InvExpansionRequestEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    private UUID id;

    @Column(name = "request_id")
    private UUID requestId;

    @Column(name = "request_description")
    private String requestDescription;

    @Column(name = "request_name")
    private String requestName;

    @Column(name = "due_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dueDate;

    @Column(name = "status")
    private String status;

    @Column(name = "cpu_qty")
    private Integer cpuQty;

    @Column(name = "ram_qty")
    private Double ramQty;

    @Column(name = "hdd_qty")
    private Double hddQty;

//    @ManyToOne
//    @JoinColumn(name = "dc_id", referencedColumnName = "dc_id")
//    private InvDcEntity invDCEntity;

    @Column(name = "dc_id")
    private String invDC;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "date_created")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateCreated;
}
