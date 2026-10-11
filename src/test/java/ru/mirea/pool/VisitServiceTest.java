package ru.mirea.pool;

import org.junit.jupiter.api.*;
import ru.mirea.pool.application.service.VisitService;
import ru.mirea.pool.application.auth.UserSession;
import ru.mirea.pool.domain.model.*;
import ru.mirea.pool.domain.repository.*;
import ru.mirea.pool.domain.exception.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VisitServiceTest {
    ClientRepository clients; VisitRepository visits; VisitService service;
    LocalDate date=LocalDate.now().plusDays(5);
    UserSession actor=new UserSession(1,"operator",UserRole.OPERATOR);
    @BeforeEach void setUp() {
        clients=mock(ClientRepository.class);visits=mock(VisitRepository.class);service=new VisitService(clients,visits);
        when(clients.existsById(1)).thenReturn(true);when(visits.save(any())).thenAnswer(i -> i.getArgument(0));
    }
    Visit visit(long client, int lane, VisitStatus status) { return new Visit(5L,client,date,LocalTime.of(10,0),60,lane,status,1L); }
    @Test void rejectsPastAndInvalidSchedules() {
        assertThrows(BusinessRuleException.class,()->service.createVisit(actor,1,date.minusYears(1),LocalTime.of(10,0),60,1));
        assertThrows(ValidationException.class,()->service.createVisit(actor,1,date,LocalTime.of(10,0),45,1));
        assertThrows(ValidationException.class,()->service.createVisit(actor,1,date,LocalTime.of(10,15),60,1));
        assertThrows(ValidationException.class,()->service.createVisit(actor,1,date,LocalTime.of(10,0),60,9));
        assertThrows(BusinessRuleException.class,()->service.createVisit(actor,1,date,LocalTime.of(21,30),60,1));
        verify(visits,never()).save(any());
    }
    @Test void requiresExistingClientAndSession() {
        assertThrows(AccessDeniedException.class,()->service.createVisit(null,1,date,LocalTime.of(10,0),60,1));
        assertThrows(EntityNotFoundException.class,()->service.createVisit(actor,99,date,LocalTime.of(10,0),60,1));
    }
    @Test void rejectsOverlapsAndAllowsAdjacentSlots() {
        when(visits.findByClientIdAndDate(1,date)).thenReturn(List.of(visit(1,2,VisitStatus.PLANNED)));
        assertThrows(BusinessRuleException.class,()->service.createVisit(actor,1,date,LocalTime.of(10,30),60,3));
        assertDoesNotThrow(()->service.createVisit(actor,1,date,LocalTime.of(11,0),60,3));
    }
    @Test void rejectsOccupiedLaneAndReleasesCancelledLane() {
        when(visits.findByDate(date)).thenReturn(List.of(visit(2,3,VisitStatus.PLANNED)));
        assertThrows(BusinessRuleException.class,()->service.createVisit(actor,1,date,LocalTime.of(10,0),60,3));
        when(visits.findByDate(date)).thenReturn(List.of(visit(2,3,VisitStatus.CANCELLED)));
        assertEquals(VisitStatus.PLANNED,service.createVisit(actor,1,date,LocalTime.of(10,0),60,3).getStatus());
    }
    @Test void availableSlotsExcludeClientConflictsAndOccupiedLanes() {
        when(visits.findByDate(date)).thenReturn(List.of(visit(1,1,VisitStatus.PLANNED)));
        assertTrue(service.getAvailableSlots(1,date,60).stream().noneMatch(s->s.startTime().equals(LocalTime.of(10,0))));
        when(visits.findByDate(date)).thenReturn(List.of(visit(2,3,VisitStatus.PLANNED)));
        var slot=service.getAvailableSlots(1,date,60).stream().filter(s->s.startTime().equals(LocalTime.of(10,0))).findFirst().orElseThrow();
        assertFalse(slot.availableLaneNumbers().contains(3));assertEquals(7,slot.availableLaneNumbers().size());
    }
    @Test void doesNotRewriteHistoryOrCancelCompletedVisits() {
        when(visits.findById(5)).thenReturn(Optional.of(visit(1,1,VisitStatus.COMPLETED)));
        assertThrows(BusinessRuleException.class,()->service.updateVisit(5,1,date,LocalTime.of(12,0),60,2));
        assertThrows(BusinessRuleException.class,()->service.cancelVisit(5));verify(visits,never()).update(any());
    }
}
