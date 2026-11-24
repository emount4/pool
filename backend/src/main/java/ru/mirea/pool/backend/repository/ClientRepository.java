package ru.mirea.pool.backend.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.mirea.pool.backend.domain.Client;

import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long> {

    @Query("""
            select client from Client client
            where locate(lower(:query), lower(concat(client.lastName, ' ', client.firstName))) > 0
                or locate(:query, client.phone) > 0
            order by client.lastName, client.firstName, client.id
            """)
    List<Client> search(@Param("query") String query, Pageable pageable);
}
