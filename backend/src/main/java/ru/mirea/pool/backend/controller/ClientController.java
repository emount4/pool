package ru.mirea.pool.backend.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ru.mirea.pool.backend.domain.Client;
import ru.mirea.pool.backend.repository.ClientRepository;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/clients")
@SecurityRequirement(name = "bearerAuth")
public class ClientController {

    private final ClientRepository clients;

    public ClientController(ClientRepository clients) {
        this.clients = clients;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER', 'OPERATOR', 'ADMIN')")
    public List<ClientChoice> list(@RequestParam(defaultValue = "") String query) {
        return clients.search(query.strip(), PageRequest.of(0, 50)).stream()
                .map(client -> new ClientChoice(client.getId(), client.getFirstName(), client.getLastName(),
                        client.getPhone()))
                .toList();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ClientChoice create(@Valid @RequestBody CreateClient request) {
        if (request.birthDate().isBefore(LocalDate.now().minusYears(120))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Возраст клиента должен быть до 120 лет.");
        }
        Client client = clients.saveAndFlush(new Client(request.firstName().strip(), request.lastName().strip(),
                request.phone(), request.email() == null || request.email().isBlank() ? null : request.email(),
                request.birthDate()));
        return new ClientChoice(client.getId(), client.getFirstName(), client.getLastName(), client.getPhone());
    }

    public record CreateClient(
            @NotBlank @Size(min = 2, max = 100) String firstName,
            @NotBlank @Size(min = 2, max = 100) String lastName,
            @NotBlank @Pattern(regexp = "[0-9]{7,15}") String phone,
            @Email @Size(max = 255) String email,
            @NotNull @PastOrPresent LocalDate birthDate
    ) {
    }

    public record ClientChoice(long id, String firstName, String lastName, String phone) {
    }
}
