package ru.mirea.pool.backend.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.pool.backend.domain.WebUser;

import java.util.Optional;
import java.util.UUID;

public interface WebUserRepository extends JpaRepository<WebUser, UUID> {

    @EntityGraph(attributePaths = "roles")
    Optional<WebUser> findByUsernameIgnoreCase(String username);

    @Override
    @EntityGraph(attributePaths = "roles")
    Optional<WebUser> findById(UUID id);
}
