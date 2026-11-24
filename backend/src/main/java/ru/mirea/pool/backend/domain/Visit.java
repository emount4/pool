package ru.mirea.pool.backend.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Entity
@Table(name = "visits")
public class Visit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Generated(event = EventType.INSERT)
    @Column(nullable = false, insertable = false, updatable = false)
    private UUID publicId;

    @Generated(event = EventType.INSERT, writable = true)
    @Column(nullable = false, length = 64)
    private String visitNumber;

    public void assignVisitNumber(String visitNumber) {
        this.visitNumber = visitNumber;
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lane_number")
    private Lane lane;

    @Column(nullable = false)
    private LocalDate visitDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private int durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VisitStatus status;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false)
    private boolean individual;

    @Column(nullable = false)
    private boolean archived;

    @ElementCollection
    @CollectionTable(name = "visit_tags", joinColumns = @JoinColumn(name = "visit_id"))
    @Column(name = "tag", nullable = false, length = 50)
    private Set<String> tags = new HashSet<>();

    public Set<String> getTags() {
        return Set.copyOf(tags);
    }

    public void addTag(String tag) {
        tags.add(tag);
    }

    public void removeTag(String tag) {
        tags.remove(tag);
    }

    @Version
    private long version;

    @Generated(event = EventType.INSERT)
    @Column(nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    protected Visit() {
    }

    public Visit(Client client, Lane lane, LocalDate visitDate, LocalTime startTime,
                 int durationMinutes, String title, String description, boolean individual) {
        this.status = VisitStatus.PLANNED;
        reschedule(client, lane, visitDate, startTime, durationMinutes, title, description, individual);
    }

    public void reschedule(Client client, Lane lane, LocalDate visitDate, LocalTime startTime,
                           int durationMinutes, String title, String description, boolean individual) {
        this.client = client;
        this.lane = lane;
        this.visitDate = visitDate;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
        this.title = title;
        this.description = description;
        this.individual = individual;
    }

    public void archive() {
        archived = true;
    }

    public void restore() {
        archived = false;
    }

    public void changeStatus(VisitStatus status) {
        this.status = status;
    }

    public VisitPriority getPriority() {
        return VisitPriority.calculate(individual, durationMinutes);
    }
}
