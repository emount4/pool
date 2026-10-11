package ru.mirea.pool;

import org.junit.jupiter.api.*;
import ru.mirea.pool.application.service.ClientService;
import ru.mirea.pool.domain.model.Client;
import ru.mirea.pool.domain.repository.*;
import ru.mirea.pool.domain.exception.*;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClientServiceTest {
    ClientRepository clients; VisitRepository visits; ClientService service;
    @BeforeEach void setUp() {
        clients = mock(ClientRepository.class); visits = mock(VisitRepository.class); service = new ClientService(clients,visits);
        when(clients.save(any())).thenAnswer(i -> i.getArgument(0));
    }
    @Test void normalisesContactData() {
        var c = service.createClient(" Иван ", " Иванов ", "+7 (999) 123-45-67", " IVAN@EXAMPLE.COM ", LocalDate.of(2000,1,1));
        assertEquals("Иван",c.getFirstName()); assertEquals("+79991234567",c.getPhone()); assertEquals("ivan@example.com",c.getEmail());
    }
    @Test void rejectsInvalidFieldsBeforeWriting() {
        assertThrows(ValidationException.class, () -> service.createClient("", "Иванов", "+79991234567", null, LocalDate.now()));
        assertThrows(ValidationException.class, () -> service.createClient("И".repeat(101), "Иванов", "+79991234567", null, LocalDate.now()));
        assertThrows(ValidationException.class, () -> service.createClient("Иван", "Иванов", "phone", null, LocalDate.now()));
        assertThrows(ValidationException.class, () -> service.createClient("Иван", "Иванов", "+79991234567", "invalid", LocalDate.now()));
        assertThrows(ValidationException.class, () -> service.createClient("Иван", "Иванов", "+79991234567", null, LocalDate.now().plusDays(1)));
        verify(clients,never()).save(any());
    }
    @Test void rejectsDuplicateContacts() {
        when(clients.findByPhone("+79991234567")).thenReturn(Optional.of(new Client(1L,"Иван","Иванов","+79991234567",null,LocalDate.now())));
        assertThrows(BusinessRuleException.class, () -> service.createClient("Пётр","Петров","+79991234567",null,LocalDate.now()));
        verify(clients,never()).save(any());
    }
    @Test void protectsClientsWithVisits() {
        when(clients.findById(1)).thenReturn(Optional.of(new Client())); when(visits.existsByClientId(1)).thenReturn(true);
        assertThrows(BusinessRuleException.class, () -> service.deleteClient(1)); verify(clients,never()).deleteById(anyLong());
    }
}
