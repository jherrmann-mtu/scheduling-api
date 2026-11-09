package com.mtu.tsw.scheduling.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import com.mtu.tsw.scheduling.notification.NotificationService;
import com.mtu.tsw.scheduling.schedule.ScheduleRecords.HostStatus;
import com.mtu.tsw.scheduling.user.UserEntity;
import com.mtu.tsw.scheduling.user.UserRepository;

/**
 * Integration tests for {@link ScheduleService} against a real (H2) database.
 */
// auto_quote_keyword: "end" is a reserved word in H2
@DataJpaTest(properties = "spring.jpa.properties.hibernate.auto_quote_keyword=true")
@Import(ScheduleService.class)
class ScheduleIntegrationTests {

    private static final long MINUTE = 60L;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;
    /** Midnight (UTC) 30 days from now, so "past appointment" filtering never interferes. */
    private static final long BASE = ((System.currentTimeMillis() / 1000) / DAY) * DAY + 30 * DAY;

    @Autowired
    private ScheduleService service;

    @Autowired
    private AppointmentRepository repo;

    @Autowired
    private TestEntityManager em;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private UserRepository userRepository;

    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();
    private final UUID carol = UUID.randomUUID();

    /** Epoch time at the given time of day on the base day. */
    private static long t(int hour, int minute) {
        return BASE + hour * HOUR + minute * MINUTE;
    }

    /** Epoch time at the given time of day, {@code dayOffset} days after the base day. */
    private static long t(int dayOffset, int hour, int minute) {
        return t(hour, minute) + dayOffset * DAY;
    }

    private static long dayStart() {
        return t(0, 0);
    }

    private static long dayEnd() {
        return t(23, 59);
    }

    /** Builds an unsaved entity. The entity has no setters, so fields are set reflectively. */
    private AppointmentEntity newAppointment(String name, UUID host, List<UUID> guests,
            long start, long end, boolean isPublic) {
        var a = new AppointmentEntity();
        ReflectionTestUtils.setField(a, "host", host);
        ReflectionTestUtils.setField(a, "guests", new ArrayList<>(guests));
        ReflectionTestUtils.setField(a, "name", name);
        ReflectionTestUtils.setField(a, "description", "description");
        ReflectionTestUtils.setField(a, "location", "location");
        ReflectionTestUtils.setField(a, "start", start);
        ReflectionTestUtils.setField(a, "end", end);
        ReflectionTestUtils.setField(a, "isPublic", isPublic);
        return a;
    }

    /** Persists, flushes and clears the persistence context. Returns the (now detached) entity with its id set. */
    private AppointmentEntity persist(AppointmentEntity a) {
        em.persistAndFlush(a);
        em.clear();
        return a;
    }

    private AppointmentEntity persist(String name, UUID host, List<UUID> guests, long start, long end) {
        return persist(newAppointment(name, host, guests, start, end, false));
    }

    private AppointmentEntity persistPublic(String name, UUID host, long start, long end) {
        return persist(newAppointment(name, host, List.of(), start, end, true));
    }

    private static UserEntity user(UUID id) {
        var u = mock(UserEntity.class);
        when(u.getId()).thenReturn(id);
        return u;
    }

    private static List<String> names(List<AppointmentEntity> appointments) {
        return appointments.stream().map(AppointmentEntity::getName).toList();
    }

    @Test
    void getUserAppointments_host_returnsOnlyHostedInDateOrder() {
        persist("late", alice, List.of(), t(14, 0), t(15, 0));
        persist("early", alice, List.of(), t(9, 0), t(10, 0));
        persist("guest-of", bob, List.of(alice), t(11, 0), t(12, 0));
        persist("unrelated", bob, List.of(carol), t(11, 0), t(12, 0));

        var result = service.getUserAppointments(alice, HostStatus.HOST, 10);

        assertThat(names(result)).containsExactly("early", "late");
    }

    @Test
    void getUserAppointments_guest_returnsOnlyGuestedInDateOrder() {
        persist("hosted", alice, List.of(), t(9, 0), t(10, 0));
        persist("guest-late", bob, List.of(alice, carol), t(14, 0), t(15, 0));
        persist("guest-early", carol, List.of(alice), t(11, 0), t(12, 0));
        persist("unrelated", bob, List.of(carol), t(11, 0), t(12, 0));

        var result = service.getUserAppointments(alice, HostStatus.GUEST, 10);

        assertThat(names(result)).containsExactly("guest-early", "guest-late");
    }

    @Test
    void getUserAppointments_either_returnsHostedAndGuestedInDateOrder() {
        persist("hosted-late", alice, List.of(), t(14, 0), t(15, 0));
        persist("guest-mid", bob, List.of(alice), t(11, 0), t(12, 0));
        persist("hosted-early", alice, List.of(), t(9, 0), t(10, 0));
        persist("unrelated", bob, List.of(carol), t(11, 0), t(12, 0));

        var result = service.getUserAppointments(alice, HostStatus.EITHER, 10);

        assertThat(names(result)).containsExactly("hosted-early", "guest-mid", "hosted-late");
    }

    @Test
    void getUserAppointments_respectsLimitAndKeepsEarliest() {
        for (int i = 11; i >= 0; i--) { // inserted in reverse to prove ordering is by date
            persist("a" + i, alice, List.of(), t(i, 9, 0), t(i, 10, 0));
        }

        var result = service.getUserAppointments(alice, HostStatus.HOST, 3);

        assertThat(names(result)).containsExactly("a0", "a1", "a2");
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1, 51, 1000 })
    void getUserAppointments_invalidLimit_fallsBackToTen(int limit) {
        for (int i = 0; i < 12; i++) {
            persist("a" + i, alice, List.of(), t(i, 9, 0), t(i, 10, 0));
        }

        assertThat(service.getUserAppointments(alice, HostStatus.HOST, limit)).hasSize(10);
    }

    @Test
    void getUserAppointments_excludesPastAppointments() {
        long yesterday = System.currentTimeMillis() / 1000 - DAY;
        persist("past", alice, List.of(), yesterday, yesterday + HOUR);
        persist("future", alice, List.of(), t(9, 0), t(10, 0));

        var result = service.getUserAppointments(alice, HostStatus.HOST, 10);

        assertThat(names(result)).containsExactly("future");
    }

    @Test
    void getUserAppointments_nullArguments_throw() {
        assertThrows(IllegalArgumentException.class, () -> service.getUserAppointments(null, HostStatus.HOST, 10));
        assertThrows(IllegalArgumentException.class, () -> service.getUserAppointments(alice, null, 10));
    }

    @Test
    void getUserAppointmentsInRange_host_filtersByRange() {
        persist("inside", alice, List.of(), t(9, 0), t(10, 0));
        persist("next-day", alice, List.of(), t(1, 9, 0), t(1, 10, 0));
        persist("other-host", bob, List.of(), t(9, 0), t(10, 0));

        var result = service.getUserAppointments(alice, HostStatus.HOST, 10, dayStart(), dayEnd());

        assertThat(names(result)).containsExactly("inside");
    }

    @Test
    void getUserAppointmentsInRange_guest_filtersByRange() {
        persist("inside", bob, List.of(alice), t(9, 0), t(10, 0));
        persist("next-day", bob, List.of(alice), t(1, 9, 0), t(1, 10, 0));
        persist("not-a-guest", bob, List.of(carol), t(9, 0), t(10, 0));

        var result = service.getUserAppointments(alice, HostStatus.GUEST, 10, dayStart(), dayEnd());

        assertThat(names(result)).containsExactly("inside");
    }

    @Test
    void getUserAppointmentsInRange_either_appliesRangeToBothHostedAndGuested() {
        persist("hosted-inside", alice, List.of(), t(9, 0), t(10, 0));
        persist("guest-inside", bob, List.of(alice), t(11, 0), t(12, 0));
        persist("hosted-outside", alice, List.of(), t(1, 9, 0), t(1, 10, 0));
        persist("guest-outside", bob, List.of(alice), t(1, 9, 0), t(1, 10, 0));

        var result = service.getUserAppointments(alice, HostStatus.EITHER, 10, dayStart(), dayEnd());

        assertThat(names(result)).containsExactly("hosted-inside", "guest-inside");
    }

    @Test
    void getUserAppointmentsInRange_endBeforeStart_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getUserAppointments(alice, HostStatus.HOST, 10, t(10, 0), t(9, 0)));
    }

    @Test
    void getUserAppointmentsInRange_nullArguments_throw() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getUserAppointments(null, HostStatus.HOST, 10, dayStart(), dayEnd()));
        assertThrows(IllegalArgumentException.class,
                () -> service.getUserAppointments(alice, null, 10, dayStart(), dayEnd()));
    }

    @Test
    void isAvailable_noAppointments_true() {
        assertThat(service.isAvailable(alice, t(9, 0), t(10, 0))).isTrue();
    }

    @Test
    void isAvailable_hostWithAppointmentStartingInRange_false() {
        persist("busy", alice, List.of(), t(9, 30), t(10, 30));

        assertThat(service.isAvailable(alice, t(9, 0), t(10, 0))).isFalse();
    }

    @Test
    void isAvailable_guestWithAppointmentStartingInRange_false() {
        persist("busy", bob, List.of(alice), t(9, 30), t(10, 30));

        assertThat(service.isAvailable(alice, t(9, 0), t(10, 0))).isFalse();
    }

    @Test
    void isAvailable_appointmentOnDifferentDay_true() {
        persist("tomorrow", alice, List.of(), t(1, 9, 0), t(1, 10, 0));

        assertThat(service.isAvailable(alice, t(9, 0), t(10, 0))).isTrue();
    }

    @Test
    void isAvailable_requestedSlotInsideLongerAppointment_false() {
        persist("long", alice, List.of(), t(9, 0), t(11, 0));

        assertThat(service.isAvailable(alice, t(10, 0), t(10, 30))).isFalse();
    }

    @Test
    void isAvailable_existingAppointmentOverlapsStartOfRange_false() {
        persist("running", alice, List.of(), t(8, 0), t(10, 0));

        assertThat(service.isAvailable(alice, t(9, 0), t(11, 0))).isFalse();
    }

    @Test
    void isAvailable_backToBackAppointments_true() {
        persist("next", alice, List.of(), t(10, 0), t(11, 0));

        assertThat(service.isAvailable(alice, t(9, 0), t(10, 0))).isTrue();
    }

    @Test
    void isAvailable_guestOfAppointmentOutsideRange_true() {
        persist("far-away", bob, List.of(alice), t(30, 9, 0), t(30, 10, 0));

        assertThat(service.isAvailable(alice, t(9, 0), t(10, 0))).isTrue();
    }

    @Test
    void isAvailable_endBeforeStart_throws() {
        assertThrows(IllegalArgumentException.class, () -> service.isAvailable(alice, t(10, 0), t(9, 0)));
    }

    @Test
    void isAvailable_nullUser_throws() {
        assertThrows(IllegalArgumentException.class, () -> service.isAvailable(null, t(9, 0), t(10, 0)));
    }

    @Test
    void getAvailableAppointments_noHosts_returnsAnyPublicAppointmentInRange() {
        persistPublic("public-a", alice, t(9, 0), t(10, 0));
        persistPublic("public-b", bob, t(11, 0), t(12, 0));
        persist("private", carol, List.of(), t(9, 0), t(10, 0));
        persistPublic("public-other-day", carol, t(1, 9, 0), t(1, 10, 0));

        var result = service.getAvailableAppointments(List.of(), 10, dayStart(), dayEnd());

        assertThat(names(result)).containsExactly("public-a", "public-b");
    }

    @Test
    void getAvailableAppointments_withHosts_filtersByHostAndPublic() {
        persistPublic("alice-public", alice, t(9, 0), t(10, 0));
        persistPublic("bob-public", bob, t(11, 0), t(12, 0));
        persistPublic("carol-public", carol, t(13, 0), t(14, 0));
        persist("alice-private", alice, List.of(), t(15, 0), t(16, 0));

        var result = service.getAvailableAppointments(List.of(alice, bob), 10, dayStart(), dayEnd());

        assertThat(names(result)).containsExactly("alice-public", "bob-public");
    }

    @Test
    void getAvailableAppointments_respectsLimit() {
        for (int i = 0; i < 5; i++) {
            persistPublic("p" + i, alice, t(8 + i, 0), t(8 + i, 30));
        }

        var result = service.getAvailableAppointments(List.of(), 2, dayStart(), dayEnd());

        assertThat(names(result)).containsExactly("p0", "p1");
    }

    @Test
    void getAvailableAppointments_invalidArguments_throw() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getAvailableAppointments(null, 10, dayStart(), dayEnd()));
        assertThrows(IllegalArgumentException.class,
                () -> service.getAvailableAppointments(List.of(), 10, t(10, 0), t(9, 0)));
    }

    @Test
    void searchAvailableHosts_delegatesToUserRepository() {
        var found = mock(UserEntity.class);
        when(userRepository.searchByUsername("al", 5)).thenReturn(List.of(found));

        assertThat(service.searchAvailableHosts("al", 5)).containsExactly(found);
    }

    @Test
    void searchAvailableHosts_invalidLimit_usesTen() {
        service.searchAvailableHosts("al", 0);

        verify(userRepository).searchByUsername("al", 10);
    }

    @Test
    void searchAvailableHosts_nullQuery_throws() {
        assertThrows(IllegalArgumentException.class, () -> service.searchAvailableHosts(null, 10));
    }

    @Test
    void deleteAppointment_asHost_deletesAndNotifies() {
        var saved = persist("meeting", alice, List.of(bob), t(9, 0), t(10, 0));
        var entity = repo.findById(saved.getId()).orElseThrow();

        service.deleteAppointment(entity, user(alice));
        em.flush();
        em.clear();

        assertThat(repo.existsById(saved.getId())).isFalse();
        verify(notificationService).sendHostCancelNotification(entity);
    }

    @Test
    void deleteAppointment_asGuest_removesOnlyThatGuestAndNotifies() {
        var saved = persist("meeting", alice, List.of(bob, carol), t(9, 0), t(10, 0));
        var entity = repo.findById(saved.getId()).orElseThrow();
        var bobUser = user(bob);

        service.deleteAppointment(entity, bobUser);
        em.flush();
        em.clear();

        var reloaded = repo.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getHost()).isEqualTo(alice);
        assertThat(reloaded.getGuests()).containsExactly(carol);
        verify(notificationService).sendGuestCancelNotification(entity, bobUser);
        verify(notificationService, never()).sendHostCancelNotification(any());
    }

    @Test
    void deleteAppointment_byNonParticipant_throwsAndChangesNothing() {
        var saved = persist("meeting", alice, List.of(bob), t(9, 0), t(10, 0));
        var entity = repo.findById(saved.getId()).orElseThrow();

        assertThrows(IllegalStateException.class, () -> service.deleteAppointment(entity, user(carol)));

        em.flush();
        em.clear();
        assertThat(repo.findById(saved.getId()).orElseThrow().getGuests()).containsExactly(bob);
        verifyNoInteractions(notificationService);
    }

    @Test
    void deleteAppointment_nonexistentAppointment_throws() {
        var ghost = newAppointment("ghost", alice, List.of(), t(9, 0), t(10, 0), false);
        ReflectionTestUtils.setField(ghost, "id", UUID.randomUUID());

        assertThrows(IllegalArgumentException.class, () -> service.deleteAppointment(ghost, user(alice)));
        verifyNoInteractions(notificationService);
    }

    @Test
    void deleteAppointment_nullArguments_throw() {
        assertThrows(IllegalArgumentException.class, () -> service.deleteAppointment(null, user(alice)));
        assertThrows(IllegalArgumentException.class,
                () -> service.deleteAppointment(newAppointment("x", alice, List.of(), t(9, 0), t(10, 0), false), null));
    }

    @Test
    void saveAppointment_newAppointmentAsHost_isPersistedAndCreateNotificationSent() {
        var apt = newAppointment("new", alice, List.of(bob), t(9, 0), t(10, 0), false);

        service.saveAppointment(apt, user(alice));
        em.flush();
        em.clear();

        assertThat(repo.findAll()).extracting(AppointmentEntity::getName).containsExactly("new");
        verify(notificationService).sendCreateNotification(any());
        verify(notificationService, never()).sendEditNotification(any(), any());
    }

    @Test
    void saveAppointment_editExistingAppointmentAsHost_updatesAndEditNotificationSent() {
        var saved = persist("old name", alice, List.of(), t(9, 0), t(10, 0));
        var edited = newAppointment("new name", alice, List.of(), t(9, 0), t(10, 0), false);
        ReflectionTestUtils.setField(edited, "id", saved.getId());

        service.saveAppointment(edited, user(alice));
        em.flush();
        em.clear();

        assertThat(repo.findById(saved.getId()).orElseThrow().getName()).isEqualTo("new name");
        assertThat(repo.count()).isEqualTo(1);
        verify(notificationService).sendEditNotification(any(), any());
        verify(notificationService, never()).sendCreateNotification(any());
    }

    @Test
    void saveAppointment_sameStartAsExistingAppointment_throwsAndSavesNothing() {
        persist("existing", alice, List.of(), t(9, 0), t(10, 0));
        var clash = newAppointment("clash", alice, List.of(), t(9, 0), t(9, 30), false);

        assertThatThrownBy(() -> service.saveAppointment(clash, user(alice)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("available");

        assertThat(repo.count()).isEqualTo(1);
        verifyNoInteractions(notificationService);
    }

    @Test
    void saveAppointment_startsDuringExistingAppointment_throwsAndSavesNothing() {
        persist("existing", alice, List.of(), t(9, 0), t(10, 0));
        var clash = newAppointment("clash", alice, List.of(), t(9, 30), t(10, 30), false);

        assertThatThrownBy(() -> service.saveAppointment(clash, user(alice)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("available");

        assertThat(repo.count()).isEqualTo(1);
        verifyNoInteractions(notificationService);
    }

    @Test
    void saveAppointment_byNonHost_throwsAndSavesNothing() {
        var apt = newAppointment("not mine", bob, List.of(), t(9, 0), t(10, 0), false);

        assertThatThrownBy(() -> service.saveAppointment(apt, user(alice)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("host");

        assertThat(repo.count()).isZero();
        verifyNoInteractions(notificationService);
    }

    @Test
    void saveAppointment_nullArguments_throw() {
        assertThrows(IllegalArgumentException.class, () -> service.saveAppointment(null, user(alice)));
        assertThrows(IllegalArgumentException.class,
                () -> service.saveAppointment(newAppointment("x", alice, List.of(), t(9, 0), t(10, 0), false), null));
    }
}