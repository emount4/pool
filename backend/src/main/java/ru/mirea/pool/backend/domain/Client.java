package ru.mirea.pool.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Entity
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(nullable = false)
    private LocalDate birthDate;

    protected Client() {
    }

    public Client(String firstName, String lastName, String phone, String email, LocalDate birthDate) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.email = email;
        this.birthDate = birthDate;
    }
}
