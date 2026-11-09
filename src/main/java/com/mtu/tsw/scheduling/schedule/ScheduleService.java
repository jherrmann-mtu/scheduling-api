package com.mtu.tsw.scheduling.schedule;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import com.mtu.tsw.scheduling.notification.NotificationService;
import com.mtu.tsw.scheduling.schedule.ScheduleRecords.HostStatus;
import com.mtu.tsw.scheduling.user.UserEntity;
import com.mtu.tsw.scheduling.user.UserRepository;

@Service
public class ScheduleService {
    
    private final NotificationService notificationService;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;

    public ScheduleService(NotificationService notificationService, AppointmentRepository appointmentRepository, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
    }

    private void throwForNull(Object... objects) {
        for (Object obj : objects) {
            if (obj == null) {
                throw new IllegalArgumentException("Arguments may not be null");
            }
        }
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0 || limit > 50)
            limit = 10;
        return limit;
    }

    public Optional<AppointmentEntity> getAppointment(UUID userID, UUID appt) {
        var opt = appointmentRepository.findById(appt);
        if (opt.isEmpty())
            return opt;

        var apptEntity = opt.get();

        if (apptEntity.getHost().equals(userID) || apptEntity.getGuests().contains(userID))
            return opt;
        
        return Optional.empty();
    }

    /**
     * Get appointments which the given user is included in.
     * 
     * Appointment list will be ordered by date. Past appointments will be excluded.
     * @param userID User to search
     * @param hostStatus Which types of appointments to include. See {@link HostStatus} for more information.
     * @param limit Maximum length of returned list
     * @return List of appointments. May be empty.
     */
    public List<AppointmentEntity> getUserAppointments(UUID userID, HostStatus hostStatus, int limit) {
        throwForNull(userID, hostStatus);
        limit = normalizeLimit(limit);

        if (hostStatus == HostStatus.EITHER) {
            return appointmentRepository.findByParticipation(userID, Instant.now().getEpochSecond(), Limit.of(limit));
        }else if (hostStatus == HostStatus.GUEST) {
            return appointmentRepository.findByGuest(userID, Instant.now().getEpochSecond(), Limit.of(limit));
        }else if (hostStatus == HostStatus.HOST) {
            return appointmentRepository.findByHost(userID, Instant.now().getEpochSecond(), Limit.of(limit));
        }

        throw new IllegalArgumentException("HostStatus must be one of 'EITHER', 'GUEST', 'HOST'");
    }

    /**
     * Get appointments which the given user is included in across a given date range.
     * Appointment list will be ordered by date.
     * @param userID User to search
     * @param hostStatus Which types of appointments to include. See {@link HostStatus} for more information.
     * @param limit Maximum length of returned list
     * @param rangeStart Beginning of datetime range
     * @param rangeEnd End of datetime range
     * @return List of appointments. May be empty.
     */
    public List<AppointmentEntity> getUserAppointments(UUID userID, HostStatus hostStatus, int limit, long start, long end) {
        throwForNull(userID, hostStatus, start, end);

        if (end < start)
            throw new IllegalArgumentException("Start of time range must be before end");

        limit = normalizeLimit(limit);

        if (hostStatus == HostStatus.EITHER) {
            return appointmentRepository.findByParticipationRange(userID, Limit.of(limit), start, end);
        }else if (hostStatus == HostStatus.GUEST) {
            return appointmentRepository.findByGuestRange(userID, Limit.of(limit), start, end);
        }else if (hostStatus == HostStatus.HOST) {
            return appointmentRepository.findByHostRange(userID, Limit.of(limit), start, end);
        }

        throw new IllegalArgumentException("HostStatus must be one of 'EITHER', 'GUEST', 'HOST'");
    }

    /**
     * Determine if the given user is available during the given time
     * @param userID User to check against
     * @param rangeStart Beginning of datetime range
     * @param rangeEnd End of datetime range
     * @return True if the user has no appointments during the given time, false otherwise.
     */
    public boolean isAvailable(UUID userID, long rangeStart, long rangeEnd) {
        return isAvailable(null, userID, rangeStart, rangeEnd);
    }

    /**
     * Determine if the given user is available during the given time
     * @param appointmentID Appointment to ignore for the check. Useful for pre-edit checks (since appointment being edited would show up). May be null.
     * @param userID User to check against
     * @param rangeStart Beginning of datetime range
     * @param rangeEnd End of datetime range
     * @return True if the user has no appointments during the given time, false otherwise.
     */
    private boolean isAvailable(UUID appointmentID, UUID userID, long rangeStart, long rangeEnd) {
        throwForNull(userID, rangeStart, rangeEnd);

        if (rangeEnd <= rangeStart)
            throw new IllegalArgumentException("Start of time range must be before end");

        return appointmentRepository.findByParticipationRange(userID, Limit.of(2), rangeStart, rangeEnd)
            .stream().filter((a) -> !a.getId().equals(appointmentID))
            .count() == 0;
    }

    /**
     * Get appointments which are available during the given time range.
     * @param hosts Hosts to filter by. If the list is empty, get any hosts.
     * @param limit Maximum length of returned list
     * @param rangeStart Beginning of datetime range
     * @param rangeEnd End of datetime range
     * @return List of appointments. May be empty.
     */
    public List<AppointmentEntity> getAvailableAppointments(List<UUID> hosts, int limit, long start, long end) {
        throwForNull(hosts, start, end);

        if (end <= start)
            throw new IllegalArgumentException("Start of time range must be before end");

        limit = normalizeLimit(limit);

        if (hosts.isEmpty())
            return appointmentRepository.findAnyByRange(Limit.of(limit), start, end);

        return appointmentRepository.findByRangeWithHosts(hosts, Limit.of(limit), start, end);
    }

    /**
     * Get a list of users who have marked themselves as public appointment hosts.
     * @param usernameQuery Username to search against
     * @param limit Maximum length of returned list
     * @return List of users. May be empty.
     */
    public List<UserEntity> searchAvailableHosts(String usernameQuery, int limit) {
        throwForNull(usernameQuery);
        limit = normalizeLimit(limit);

        return userRepository.searchByUsername(usernameQuery, limit);
    }

    public void deleteAppointment(AppointmentEntity appointment, UserEntity currentUser) {
        throwForNull(appointment, currentUser);

        if (!appointmentRepository.existsById(appointment.getId()))
            throw new IllegalArgumentException("Cannot delete an appointment which doesn't exist");

        if (appointment.getHost().equals(currentUser.getId())) {
            appointmentRepository.delete(appointment);
            notificationService.sendHostCancelNotification(appointment);
        }else if(appointment.getGuests().indexOf(currentUser.getId()) != -1){
            appointmentRepository.save(AppointmentEntity.removeGuest(appointment, currentUser.getId()));
            notificationService.sendGuestCancelNotification(appointment, currentUser);
        }else {
            throw new IllegalStateException("Users can only delete appointments which they are either a host or guest of");
        }
    }

    public AppointmentEntity saveAppointment(AppointmentEntity appointment, UserEntity currentUser) {
        throwForNull(appointment, currentUser);

        if (!isAvailable(appointment.getId(), currentUser.getId(), appointment.getStart(), appointment.getEnd())) {
            throw new IllegalStateException("User must be available during appointment time");
        }

        if (!appointment.getHost().equals(currentUser.getId())) {
                throw new IllegalArgumentException("User can only modify appointment as host");
            }

        if (appointment.getId() == null) {
            // new appointment
            var rtn = appointmentRepository.save(appointment);
            notificationService.sendCreateNotification(appointment);
            return rtn;
        }

        var before = appointmentRepository.findById(appointment.getId());
        var rtn = appointmentRepository.save(appointment);
        notificationService.sendEditNotification(before.get(), appointment);

        return rtn;
    }

}
