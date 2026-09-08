package ru.mirea.pool.domain.repository;

import ru.mirea.pool.domain.model.SystemUser;

import java.util.List;
import java.util.Optional;

public interface SystemUserRepository {

    SystemUser save(SystemUser user);

    Optional<SystemUser> findById(long id);

    Optional<SystemUser> findByUsername(String username);

    List<SystemUser> findAll();

    void update(SystemUser user);

    boolean existsByUsername(String username);

    long countActiveAdmins();
}
