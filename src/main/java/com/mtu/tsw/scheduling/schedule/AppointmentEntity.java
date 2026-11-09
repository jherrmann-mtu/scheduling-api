package com.mtu.tsw.scheduling.schedule;

import java.util.List;
import java.util.Optional;
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
            apt.getStart(),
            apt.getEnd(),
            apt.isPublic
        );
    }
    
    public record AppointmentRequest(
        Optional<List<UUID>> guests,
        Optional<String> name,
        Optional<String> description,
        Optional<String> location,
        Optional<Long> start,
        Optional<Long> end,
        Optional<Boolean> isPublic
    ) {}

    public static AppointmentEntity createAppointment(UUID host, List<UUID> guests, String name, String description, String location, long start, long end, boolean isPublic) {
        return new AppointmentEntity(null,
            host,
            guests,
            name,
            description,
            location,
            start,
            end,
            isPublic
        );
    }

    public static AppointmentEntity updateAppointment(AppointmentEntity entity, AppointmentRequest request) {
        return new AppointmentEntity(
            entity.getId(), 
            entity.getHost(),
            request.guests().orElse(entity.getGuests()),
            request.name().orElse(entity.getName()),
            request.description().orElse(entity.getDescription()),
            request.location().orElse(entity.getLocation()),
            request.start().orElse(entity.getStart()),
            request.end().orElse(entity.getEnd()),
            request.isPublic().orElse(entity.isPublic())
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
    private long start;

    @Column(nullable = false)
    private long end;

    @Column(nullable = false)
    private boolean isPublic;

    public AppointmentEntity() {}

    private AppointmentEntity(UUID id, UUID host, List<UUID> guests, String name, String description, String location, long start, long end, boolean isPublic) {
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

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public boolean isPublic() {
        return isPublic;
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

