package tech.scalea.capacityRequest.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "inv_vm")
public class InvVmEntity {

    @Id
    //@GeneratedValue(generator = "inv_vm_seq", strategy = GenerationType.SEQUENCE)
    //@SequenceGenerator(name = "inv_vm_seq", sequenceName = "inv_vm_seq", allocationSize = 1)
    private Long id;

    @Column(name = "host_id")
    private String hostId;

}
