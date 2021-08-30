package dev.alexengrig.tx.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Data
@Entity(name = "Man")
@Table(name = "man")
@NoArgsConstructor
@AllArgsConstructor
public class ManEntity {
    @Id
    @GeneratedValue(generator = "manIdSeqGen", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "manIdSeqGen", sequenceName = "man_id_seq", allocationSize = 1)
    private Long id;
    @OneToOne
    private ManEntity partner;
    @Column(name = "name", nullable = false)
    private String name;

    public ManEntity(String name) {
        this.name = name;
    }
}
