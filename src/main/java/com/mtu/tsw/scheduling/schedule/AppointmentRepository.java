package com.mtu.tsw.scheduling.schedule;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends CrudRepository<AppointmentEntity, UUID> {
    
    /**
     * Find appointments the given user is hosting
     */
    @Query ("SELECT a FROM Appointment a WHERE a.host = :host AND a.end > :now ORDER BY a.start")
    public List<AppointmentEntity> findByHost(@Param("host") UUID host, @Param("now") long now, Limit limit);

    /**
     * Find appointments the given user a guest of 
     */
    @Query ("SELECT a FROM Appointment a WHERE :guest MEMBER OF a.guests AND a.end > :now ORDER BY a.start")
    public List<AppointmentEntity> findByGuest(@Param("guest") UUID guest, @Param("now") long now, Limit limit);

    /**
     * Find appointments the given user is attending (host or guest)
     */
    @Query ("SELECT a FROM Appointment a WHERE (:user MEMBER OF a.guests OR a.host = :user) AND a.end > :now ORDER BY a.start")
    public List<AppointmentEntity> findByParticipation(@Param("user") UUID user, @Param("now") long now, Limit limit);

    /**
     * Find appointments the given user is hosting in the datetime range
     */
    @Query ("SELECT a FROM Appointment a WHERE a.host = :host AND a.start BETWEEN :start AND :end ORDER BY a.start")
    public List<AppointmentEntity> findByHostRange(@Param("host") UUID host, Limit limit, @Param("start") long start, @Param("end") long end);

    /**
     * Find appointments the given user is a guest of in the datetime range
     */
    @Query ("SELECT a FROM Appointment a WHERE :guest MEMBER OF a.guests AND a.start BETWEEN :start AND :end ORDER BY a.start")
    public List<AppointmentEntity> findByGuestRange(@Param("guest") UUID guest, Limit limit, @Param("start") long start, @Param("end") long end);

    /**
     * Find appointments the given user is attending (host or guest) in the datetime range
     */
    @Query ("SELECT a FROM Appointment a WHERE (:user MEMBER OF a.guests OR a.host = :user) AND a.start < :end AND a.end > :start ORDER BY a.start")
    public List<AppointmentEntity> findByParticipationRange(@Param("user") UUID user, Limit limit, @Param("start") long start, @Param("end") long end);

    /**
     * Find public appointments that exist in the datetime range
     */
    @Query ("SELECT a FROM Appointment a WHERE a.isPublic AND a.start BETWEEN :start AND :end ORDER BY a.start")
    public List<AppointmentEntity> findAnyByRange(Limit limit, @Param("start") long start, @Param("end") long end);

    /**
     * Find public appointments that exist in the datetime range with any of the given hosts
     */
    @Query ("SELECT a FROM Appointment a WHERE a.host IN :host AND a.isPublic AND a.start BETWEEN :start AND :end ORDER BY a.start")
    public List<AppointmentEntity> findByRangeWithHosts(@Param("host") List<UUID> host, Limit limit, @Param("start") long start, @Param("end") long end);
}
