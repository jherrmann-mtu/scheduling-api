package com.mtu.tsw.scheduling.schedule;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.mtu.tsw.scheduling.notification.NotificationService;
import com.mtu.tsw.scheduling.schedule.ScheduleRecords.HostStatus;
import com.mtu.tsw.scheduling.user.UserEntity;
import com.mtu.tsw.scheduling.user.UserRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ScheduleUnitTests {

    private ScheduleService scheduleService;

    @Mock
    private NotificationService notificationService;

    @Mock 
    private AppointmentRepository appointmentRepository;

    @Mock 
    private UserRepository userRepository;

    @Mock 
    private UserEntity mockUser;

    @Mock 
    private AppointmentEntity mockApt;

    @BeforeEach 
    public void setup() {
        scheduleService = new ScheduleService(notificationService, appointmentRepository, userRepository);

        Mockito.when(mockApt.getId()).thenReturn(UUID.randomUUID());
        Mockito.when(mockApt.getHost()).thenReturn(UUID.randomUUID());

        Mockito.when(mockApt.getStart()).thenReturn(LocalDateTime.now());
        Mockito.when(mockApt.getEnd()).thenReturn(LocalDateTime.now());
        Mockito.when(mockApt.getTimezone()).thenReturn(ZoneId.systemDefault());

        Mockito.when(mockUser.getId()).thenReturn(UUID.randomUUID());
    }
    
    // Simple unit tests
    @Test 
    public void all_throwsWhenNullArgs() {
        List<Runnable> runs = new ArrayList<>();

        runs.add(() -> scheduleService.getUserAppointments(null, null, 0));
        runs.add(() -> scheduleService.getUserAppointments(null, null, 0, null, null, null));
        runs.add(() -> scheduleService.isAvailable(null, null, null, null));
        runs.add(() -> scheduleService.getAvailableAppointments(null, 0, null, null, null));
        runs.add(() -> scheduleService.searchAvailableHosts(null, 0));
        runs.add(() -> scheduleService.deleteAppointment(null, null));
        runs.add(() -> scheduleService.saveAppointment(null, null));

        runs.forEach((runnable) -> {
            assertThrows(IllegalArgumentException.class, runnable::run);
        });
    }

    @Test
    public void range_throwsWhenInvalidRange() {
        List<Runnable> runs = new ArrayList<>();

        LocalDateTime later = LocalDateTime.ofEpochSecond(200, 0, ZoneOffset.UTC);
        LocalDateTime earlier = LocalDateTime.ofEpochSecond(100, 0, ZoneOffset.UTC);
        TimeZone tz = TimeZone.getDefault();

        runs.add(() -> scheduleService.getUserAppointments(UUID.randomUUID(), HostStatus.EITHER, 1, later, earlier, tz));
        runs.add(() -> scheduleService.isAvailable(UUID.randomUUID(), later, earlier, tz));
        runs.add(() -> scheduleService.getAvailableAppointments(List.of(), 1, later, earlier, tz));

        runs.forEach((runnable) -> {
            assertThrows(IllegalArgumentException.class, runnable::run);
        });
    }

    @Test
    public void delete_throwsWhenAppointmentNoExist() {
        Mockito.when(appointmentRepository.existsById(any(UUID.class))).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> {
            scheduleService.deleteAppointment(mockApt, mockUser);
        });
    }

    @Test
    public void delete_SendsHostNotification() {
        Mockito.when(appointmentRepository.existsById(any(UUID.class))).thenReturn(true);

        UUID id = UUID.randomUUID();
        Mockito.when(mockApt.getHost()).thenReturn(id);
        Mockito.when(mockUser.getId()).thenReturn(id);

        scheduleService.deleteAppointment(mockApt, mockUser);

        Mockito
            .verify(notificationService)
            .sendHostCancelNotification(any(AppointmentEntity.class));
    }

    @Test
    public void delete_SendsGuestNotification() {
        Mockito.when(appointmentRepository.existsById(any(UUID.class))).thenReturn(true);

        // host and user are different by default, only need to mock guests

        UUID id = UUID.randomUUID();
        Mockito.when(mockApt.getGuests()).thenReturn(List.of(id));
        Mockito.when(mockUser.getId()).thenReturn(id);

        scheduleService.deleteAppointment(mockApt, mockUser);

        Mockito
            .verify(notificationService)
            .sendGuestCancelNotification(any(AppointmentEntity.class), any(UserEntity.class));
    }

    @Test 
    public void save_throwsWhenAppointmentNonUserOwned() {
        assertThrows(IllegalArgumentException.class, () -> scheduleService.saveAppointment(mockApt, mockUser));
    }

    @Test 
    public void save_SendsEditNotification() {
        Mockito.when(appointmentRepository.findById(any(UUID.class))).thenReturn(Optional.of(new AppointmentEntity()));
        
        UUID id = UUID.randomUUID();
        Mockito.when(mockApt.getHost()).thenReturn(id);
        Mockito.when(mockUser.getId()).thenReturn(id);

        scheduleService.saveAppointment(mockApt, mockUser);

        Mockito
            .verify(notificationService)
            .sendEditNotification(
                any(AppointmentEntity.class), 
                any(AppointmentEntity.class)
            );
    }

    @Test 
    public void save_SendsCreateNotification() {
        Mockito.when(appointmentRepository.existsById(any(UUID.class))).thenReturn(false);

        UUID id = UUID.randomUUID();
        Mockito.when(mockApt.getId()).thenReturn(null); // new appointments have null ID since they haven't hit the DB yet
        Mockito.when(mockApt.getHost()).thenReturn(id);
        Mockito.when(mockUser.getId()).thenReturn(id);

        scheduleService.saveAppointment(mockApt, mockUser);

        Mockito
            .verify(notificationService)
            .sendCreateNotification(any(AppointmentEntity.class));
    }
}
