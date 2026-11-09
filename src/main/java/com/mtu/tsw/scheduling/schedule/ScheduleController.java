package com.mtu.tsw.scheduling.schedule;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mtu.tsw.scheduling.exceptions.ExceptionMapper.MalformedArgumentException;
import com.mtu.tsw.scheduling.exceptions.ExceptionMapper.ResourceAbsentException;
import com.mtu.tsw.scheduling.exceptions.ExceptionMapper.ResourceConflictException;
import com.mtu.tsw.scheduling.schedule.AppointmentEntity.AppointmentRequest;
import com.mtu.tsw.scheduling.schedule.ScheduleRecords.HostStatus;
import com.mtu.tsw.scheduling.user.UserEntity;

@RestController
@RequestMapping("/api/schedule")
/**
 * Controller for API endpoints related to the schedule service.
 */
public class ScheduleController {

    private final ScheduleService schedule;

    public ScheduleController(ScheduleService schedule) {
        this.schedule = schedule;
    }

    private UserEntity getCurrentUser() {
        return (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private HostStatus parseScope(String scope) {
        if (scope.equalsIgnoreCase("either") || scope.isBlank())
            return HostStatus.EITHER;

        if (scope.equalsIgnoreCase("guest"))
            return HostStatus.GUEST;

        if (scope.equalsIgnoreCase("host"))
            return HostStatus.HOST;

        throw new MalformedArgumentException("Scope must be: 'EITHER' ^ 'GUEST' ^ 'HOST'");
    }

    public record UserResponse(UUID id, String name) {}

    @GetMapping("/host")
    public List<UserResponse> getHosts(@RequestParam("name") String name, @RequestParam("limit") int limit) {
        return schedule.searchAvailableHosts(name, limit)
            .stream()
            .map((entity) -> new UserResponse(entity.getId(), entity.getUsername()))
            .toList();
    }

    @GetMapping("/private/{scope}")
    public List<AppointmentEntity> getAllAppointments(@PathVariable("scope") String scope, @RequestParam("limit") int limit) {
        return schedule.getUserAppointments(getCurrentUser().getId(), parseScope(scope), limit);
    }
    
    @GetMapping("/public")
    public List<AppointmentEntity> getAvailablePublicAppointments(
        @RequestParam("limit") int limit,
        @RequestParam("start") long start,
        @RequestParam("end") long end,
        @RequestParam(name = "hosts", required = false) List<UUID> hosts
    ) {
        try {
            return schedule.getAvailableAppointments(hosts == null ? List.of() : hosts, limit, start, end);
        }catch (IllegalArgumentException e) {
            throw new MalformedArgumentException(e.getMessage());
        }  
    }

    @GetMapping("/private/range/{scope}")
    public List<AppointmentEntity> getAllAppointmentsOverTime(
        @PathVariable("scope") String scope,
        @RequestParam("limit") int limit,
        @RequestParam("start") long start,
        @RequestParam("end") long end
    ) {
        try {
            return schedule.getUserAppointments(getCurrentUser().getId(), parseScope(scope), limit, start, end);
        }catch (IllegalArgumentException e) {
            throw new MalformedArgumentException(e.getMessage());
        }   
    }

    private AppointmentEntity getAppointmentForUser(UUID id) {
        var user = getCurrentUser();

        var opt = schedule.getAppointment(user.getId(), id);

        if (opt.isEmpty())
            throw new ResourceAbsentException("Appointment not found");

        return opt.get();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteAppointment(@PathVariable("id") UUID id) {
        schedule.deleteAppointment(getAppointmentForUser(id), getCurrentUser());
        return ResponseEntity.ok("Deleted");
    }

    @GetMapping("/{id}")
    public AppointmentEntity getAppointment(@PathVariable("id") UUID id) {
        return getAppointmentForUser(id);
    }

    @PatchMapping("/{id}")
    public AppointmentEntity updateAppointment(@PathVariable("id") UUID id, AppointmentRequest request) {
        var entity = getAppointmentForUser(id);

        entity = AppointmentEntity.updateAppointment(entity, request);

        try {
            return schedule.saveAppointment(entity, getCurrentUser());
        } catch (IllegalArgumentException e) {
            throw new MalformedArgumentException(e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResourceConflictException("An appointment already exists during this time");
        } 
    }

    @PostMapping("/create")
    public AppointmentEntity createAppointment(@RequestBody AppointmentRequest request) {
        var user = getCurrentUser();
        try {
            return schedule.saveAppointment(
                AppointmentEntity.createAppointment(
                    user.getId(),
                    request.guests().orElseThrow(() -> new NoSuchElementException("guests")),
                    request.name().orElseThrow(() -> new NoSuchElementException("name")),
                    request.description().orElseThrow(() -> new NoSuchElementException("description")),
                    request.location().orElseThrow(() -> new NoSuchElementException("location")),
                    request.start().orElseThrow(() -> new NoSuchElementException("start")),
                    request.end().orElseThrow(() -> new NoSuchElementException("end")),
                    request.isPublic().orElseThrow(() -> new NoSuchElementException("isPublic"))
                ), 
                getCurrentUser()
            );
        } catch (NoSuchElementException | IllegalArgumentException e ) {
            throw new MalformedArgumentException("Invalid parameters. All parameters are required for appointment creation. Missing: " + e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResourceConflictException("An appointment already exists during this time");
        }
    }

}
