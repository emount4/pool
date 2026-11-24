package ru.mirea.pool.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Objects;

@Getter
@Setter
public class Client {

    private Long id;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private LocalDate birthDate;

    public Client() {
    }

    public Client(String firstName, String lastName, String phone, String email, LocalDate birthDate) {
        this(null, firstName, lastName, phone, email, birthDate);
    }

    public Client(Long id, String firstName, String lastName, String phone, String email, LocalDate birthDate) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.email = email;
        this.birthDate = birthDate;
    }

    @Override
    public String toString() {
        return "Client{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", birthDate=" + birthDate +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Client client)) {
            return false;
        }
        return Objects.equals(id, client.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
