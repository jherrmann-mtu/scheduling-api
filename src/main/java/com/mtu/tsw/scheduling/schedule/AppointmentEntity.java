package com.mtu.tsw.scheduling.schedule;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

/**
 * Represents an appointment in the database.
 */
@Entity(name="Appointment")
public class AppointmentEntity {

    public static AppointmentEntity removeGuest(AppointmentEntity apt, UUID guest) {
        return new AppointmentEntity(
            apt.id, 
            apt.host, 
            apt.getGuests().stream().filter((id) -> !id.equals(guest)).toList(),
            apt.name,
            apt.description,
            apt.location,
            apt.start,
            apt.end,
            apt.isPublic
        );
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private UUID host;

    @ElementCollection
    @CollectionTable (name = "guests", joinColumns = @JoinColumn(name = "guest") )
    @Column(name = "guestRef")
    private List<UUID> guests;

    @Column(nullable = false, length=64)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private ZonedDateTime start;

    @Column(nullable = false)
    private ZonedDateTime end;

    @Column(nullable = false)
    private boolean isPublic;

    public AppointmentEntity() {}

    private AppointmentEntity(UUID id, UUID host, List<UUID> guests, String name, String description, String location,
            ZonedDateTime start, ZonedDateTime end, boolean isPublic) {
        this.id = id;
        this.host = host;
        this.guests = guests;
        this.name = name;
        this.description = description;
        this.location = location;
        this.start = start;
        this.end = end;
        this.isPublic = isPublic;
    }

    public UUID getId() {
        return id;
    }

    public UUID getHost() {
        return host;
    }

    public List<UUID> getGuests() {
        return guests;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public LocalDateTime getStart() {
        return start.toLocalDateTime();
    }

    public LocalDateTime getEnd() {
        return end.toLocalDateTime();
    }

    public ZoneId getTimezone() {
        return start.getZone();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AppointmentEntity other = (AppointmentEntity) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

}

