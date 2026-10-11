package ru.mirea.pool;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import ru.mirea.pool.application.AppContext;
import ru.mirea.pool.domain.model.*;
import ru.mirea.pool.domain.exception.*;
import ru.mirea.pool.infrastructure.database.*;
import ru.mirea.pool.infrastructure.exception.DatabaseException;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in. Creates and later removes only its own isolated schema. */
@EnabledIfEnvironmentVariable(named="POOL_TEST_DB_URL",matches=".+")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JdbcIntegrationTest {
    static String schema="pool_test_"+UUID.randomUUID().toString().replace("-","");
    static DatabaseManager database;
    static AppContext context;
    static Properties config;
    @BeforeAll static void init() throws Exception {
        config=new Properties();config.setProperty("db.url",System.getenv("POOL_TEST_DB_URL"));
        config.setProperty("db.username",System.getenv().getOrDefault("POOL_TEST_DB_USER","postgres"));
        config.setProperty("db.password",System.getenv().getOrDefault("POOL_TEST_DB_PASSWORD","postgres"));
        try(var c=new DatabaseManager(config).getConnection();var s=c.createStatement()) {s.execute("CREATE SCHEMA "+schema);}
        config.setProperty("db.schema",schema);
        database=new DatabaseManager(config);context=new AppContext(database);
        try(var c=database.getConnection();var s=c.createStatement()) {
            s.execute(Files.readString(Path.of("database/schema.sql")));
            s.execute(Files.readString(Path.of("database/data.sql")));
            s.execute("SET search_path TO "+schema+",public");
            s.execute(Files.readString(Path.of("database/migrations/002_no_overlaps.sql")));
        }
    }
    @AfterAll static void clean() throws Exception {
        if(database!=null)try(var c=database.getConnection();var s=c.createStatement()){s.execute("DROP SCHEMA "+schema+" CASCADE");}
    }
    @Test @Order(1) void seedAndLogin() {
        assertEquals(8,context.clients.getAllClients().size());assertEquals(20,context.visits.getAllVisits().size());
        assertEquals(UserRole.ADMIN,context.auth.login("admin","Admin123".toCharArray()).role());
        assertEquals(UserRole.OPERATOR,context.auth.login("operator1","Operator123".toCharArray()).role());
    }
    @Test @Order(2) void crudPersistsAndRespectsReferences() {
        var client=context.clients.createClient("Тестовый","Клиент","+79990000001","test@example.org",LocalDate.of(2000,1,2));
        long id=client.getId();assertEquals("Клиент",new AppContext(database).clients.getClientById(id).getLastName());
        context.clients.updateClient(id,"Изменённый","Клиент","+79990000001",null,LocalDate.of(2000,1,2));
        assertEquals("Изменённый",context.clients.getClientById(id).getFirstName());
        var actor=context.auth.login("admin","Admin123".toCharArray());
        var date=LocalDate.now().plusDays(40);
        var visit=context.visits.createVisit(actor,id,date,LocalTime.of(9,0),60,1);
        assertThrows(BusinessRuleException.class,()->context.clients.deleteClient(id));
        context.visits.updateVisit(visit.getId(),id,date,LocalTime.of(10,0),90,2);
        assertEquals(90,context.visits.getVisitById(visit.getId()).getDurationMinutes());
        context.visits.cancelVisit(visit.getId());assertEquals(VisitStatus.CANCELLED,context.visits.getVisitById(visit.getId()).getStatus());
        context.visits.deleteVisit(visit.getId());context.clients.deleteClient(id);
        assertThrows(EntityNotFoundException.class,()->context.clients.getClientById(id));
    }
    @Test @Order(3) void databasePreventsOverlapEvenWhenServiceIsBypassed() {
        var repository=new JdbcVisitRepository(database);var date=LocalDate.now().plusDays(60);
        var first=repository.save(new Visit(1L,date,LocalTime.of(10,0),60,1,VisitStatus.PLANNED));
        assertThrows(DatabaseException.class,()->repository.save(new Visit(2L,date,LocalTime.of(10,30),60,1,VisitStatus.PLANNED)));
        assertThrows(DatabaseException.class,()->repository.save(new Visit(1L,date,LocalTime.of(10,30),60,2,VisitStatus.PLANNED)));
        var adjacent=repository.save(new Visit(2L,date,LocalTime.of(11,0),60,1,VisitStatus.PLANNED));
        repository.deleteById(first.getId());repository.deleteById(adjacent.getId());
    }
    @Test @Order(4) void refreshAndUserManagementPersist() {
        var repo=new JdbcVisitRepository(database);
        var old=repo.save(new Visit(1L,LocalDate.now().minusDays(100),LocalTime.of(10,0),60,8,VisitStatus.PLANNED));
        context.visits.refreshStatuses();assertEquals(VisitStatus.COMPLETED,repo.findById(old.getId()).orElseThrow().getStatus());repo.deleteById(old.getId());
        var actor=context.auth.login("admin","Admin123".toCharArray());
        var user=context.users.createUser(actor,"new_operator","Password123".toCharArray(),UserRole.OPERATOR);
        context.users.updateUser(actor,user.getId(),"changed_operator",UserRole.OPERATOR,false,new char[0]);
        assertFalse(context.users.getUserById(actor,user.getId()).isActive());
        assertThrows(ru.mirea.pool.application.auth.AuthenticationException.class,()->context.auth.login("changed_operator","Password123".toCharArray()));
    }
}
