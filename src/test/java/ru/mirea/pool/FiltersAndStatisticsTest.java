package ru.mirea.pool;

import org.junit.jupiter.api.Test;
import ru.mirea.pool.application.service.*;
import ru.mirea.pool.domain.model.*;
import ru.mirea.pool.domain.exception.ValidationException;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FiltersAndStatisticsTest {
    LocalDate today=LocalDate.of(2026,10,10);
    Client c=new Client(1L,"Иван","Иванов","+79991234567",null,today.minusYears(17));
    @Test void searchesAcrossFieldsAndCombinesFilters() {
        assertTrue(DataFilters.client(c,"ИВАН 12345",1,2,today));
        assertFalse(DataFilters.client(c,"Иван",2,2,today));
        assertFalse(DataFilters.client(c,"Иван",1,1,today));
    }
    @Test void dateBoundariesAreInclusiveAndInvalidPeriodIsRejected() {
        var v=new Visit(1L,1L,today,LocalTime.of(10,0),60,2,VisitStatus.PLANNED);
        assertTrue(DataFilters.visit(v,c,"Иванов",VisitStatus.PLANNED,2,today,today,1L));
        assertFalse(DataFilters.visit(v,c,"Иванов",VisitStatus.CANCELLED,2,today,today,1L));
        assertFalse(DataFilters.visit(v,c,"",null,3,null,null,null));
        assertThrows(ValidationException.class,()->DataFilters.validatePeriod(today,today.minusDays(1)));
    }
    @Test void statisticsHandleEmptyDataAndTies() {
        var service=new StatisticsService(null,null);
        var empty=service.calculate(0,List.of());
        assertEquals(0,empty.averageDurationMinutes());assertNull(empty.mostPopularLane());assertEquals(4,empty.visitsByStatus().size());
        var visits=List.of(new Visit(1L,today,LocalTime.of(10,0),60,2,VisitStatus.PLANNED),new Visit(1L,today,LocalTime.of(11,0),120,1,VisitStatus.COMPLETED));
        var stats=service.calculate(8,visits);
        assertEquals(8,stats.totalClients());assertEquals(2,stats.totalVisits());assertEquals(90,stats.averageDurationMinutes());assertEquals(1,stats.mostPopularLane());
    }
}
