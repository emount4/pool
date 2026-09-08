package ru.mirea.pool.domain.repository;

import ru.mirea.pool.domain.model.Client;

import java.util.List;
import java.util.Optional;

public interface ClientRepository {

    Client save(Client client);

    Optional<Client> findById(long id);

    List<Client> findAll();

    void update(Client client);

    void deleteById(long id);

    Optional<Client> findByPhone(String phone);

    Optional<Client> findByEmail(String email);

    List<Client> findByLastName(String lastName);

    boolean existsById(long id);
}
