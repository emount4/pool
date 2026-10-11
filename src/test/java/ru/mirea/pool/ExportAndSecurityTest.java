package ru.mirea.pool;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.mirea.pool.infrastructure.export.TableExporter;
import ru.mirea.pool.infrastructure.security.Pbkdf2PasswordHasher;
import ru.mirea.pool.application.service.UserManagementService;
import ru.mirea.pool.application.auth.UserSession;
import ru.mirea.pool.application.security.PasswordPolicy;
import ru.mirea.pool.domain.repository.SystemUserRepository;
import ru.mirea.pool.domain.model.*;
import ru.mirea.pool.domain.exception.*;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExportAndSecurityTest {
    @TempDir Path dir;
    @Test void csvPreservesFilteredOrderUnicodeQuotesAndNeutralisesFormulas() throws Exception {
        var path=dir.resolve("export.csv");
        new TableExporter().export(path,List.of("Имя","Текст"),List.of(List.of("Пётр","a;\"b\"\nс"),List.of("Анна","=2+2")));
        String csv=Files.readString(path);
        assertTrue(csv.startsWith("\uFEFF"));assertTrue(csv.contains("\"a;\"\"b\"\"\nс\""));assertTrue(csv.contains("'=2+2"));
        assertTrue(csv.indexOf("Пётр")<csv.indexOf("Анна"));
    }
    @Test void xlsxContainsOnlySuppliedRowsAndTextCells() throws Exception {
        var path=dir.resolve("export.xlsx");new TableExporter().export(path,List.of("Имя"),List.of(List.of("=SUM(A1)")));
        try(var workbook=new XSSFWorkbook(Files.newInputStream(path))){
            var sheet=workbook.getSheetAt(0);assertEquals(1,sheet.getLastRowNum());assertEquals("=SUM(A1)",sheet.getRow(1).getCell(0).getStringCellValue());
        }
    }
    @Test void seededPasswordsRemainCompatible() {
        var h=new Pbkdf2PasswordHasher();
        assertTrue(h.matches("Admin123".toCharArray(),"120000:2OjWAxHWMJa/7kO9FeEq3w==:7LBmVkUFrLP4+hj5DEANKqCIkURTueupF+3GAfEIaiU="));
        assertTrue(h.matches("Operator123".toCharArray(),"120000:vCjtAoJ3dbrYo+R7DU6H9Q==:Ctxx1oMZsn2uqWUzFvjD5h89aiBg4DiqVDiwzUe+DnA="));
        assertFalse(h.matches("incorrect".toCharArray(),h.hash("Password123".toCharArray())));
    }
    @Test void operatorCannotManageUsersAndAdminCannotLockSelf() {
        var repo=mock(SystemUserRepository.class);var service=new UserManagementService(repo,new Pbkdf2PasswordHasher(),new PasswordPolicy());
        assertThrows(AccessDeniedException.class,()->service.getAllUsers(new UserSession(2,"operator",UserRole.OPERATOR)));
        var user=new SystemUser("admin","hash",UserRole.ADMIN);user.setId(1L);
        when(repo.findById(1)).thenReturn(Optional.of(user));
        assertThrows(BusinessRuleException.class,()->service.updateUser(new UserSession(1,"admin",UserRole.ADMIN),1,"admin",UserRole.ADMIN,false,new char[0]));
        verify(repo,never()).update(any());
    }
    @Test void badPasswordDoesNotPartiallyUpdateUser() {
        var repo=mock(SystemUserRepository.class);var service=new UserManagementService(repo,new Pbkdf2PasswordHasher(),new PasswordPolicy());
        var target=new SystemUser("operator","hash",UserRole.OPERATOR);target.setId(2L);when(repo.findById(2)).thenReturn(Optional.of(target));
        assertThrows(ValidationException.class,()->service.updateUser(new UserSession(1,"admin",UserRole.ADMIN),2,"changed",UserRole.OPERATOR,true,"x".toCharArray()));
        assertEquals("operator",target.getUsername());verify(repo,never()).update(any());
    }
}
