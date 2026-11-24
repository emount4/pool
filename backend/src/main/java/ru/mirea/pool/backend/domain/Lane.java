package ru.mirea.pool.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.util.UUID;

@Getter
@Entity
@Table(name = "lanes")
public class Lane {

    @Generated(event = EventType.INSERT)
    @Column(nullable = false, insertable = false, updatable = false)
    private UUID publicId;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lane_number")
    @SequenceGenerator(name = "lane_number", sequenceName = "lane_number_seq", allocationSize = 1)
    private Integer number;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private boolean active;

    protected Lane() {
    }

    public Lane(String name, boolean active) {
        update(name, active);
    }

    public void update(String name, boolean active) {
        this.name = name;
        this.active = active;
    }
}
