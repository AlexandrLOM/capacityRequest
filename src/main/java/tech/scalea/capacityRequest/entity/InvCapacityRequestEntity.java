package tech.scalea.capacityRequest.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Data
@Entity
@Table(name = "inv_capacity_request", schema = "dp")
public class InvCapacityRequestEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    private UUID id;

    @Column(name = "request_id")
    private String requestId;

    @Column(name = "request_name")
    private String requestName;

    @Column(name = "request_description")
    private String requestDescription;

    @Column(name = "due_date")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date dueDate;

    @Column(name = "status")
    private String status;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "date_created")
    private Date createdWhen;
}
