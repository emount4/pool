package ru.mirea.pool.presentation.console;

import ru.mirea.pool.domain.model.UserRole;
import ru.mirea.pool.domain.model.VisitStatus;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public final class InputReader {

    private final Scanner scanner;

    public InputReader() {
        this.scanner = new Scanner(System.in, StandardCharsets.UTF_8);
    }

    public String readString(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            throw new IllegalStateException("Поток ввода закрыт.");
        }
        return scanner.nextLine();
    }

    public String readNonEmptyString(String prompt) {
        while (true) {
            String value = readString(prompt).trim();
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("Значение не может быть пустым.");
        }
    }

    public char[] readPassword(String prompt) {
        return readNonEmptyString(prompt).toCharArray();
    }

    public int readInt(String prompt) {
        while (true) {
            String value = readString(prompt).trim();
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.out.println("Введите целое число.");
            }
        }
    }

    public long readLong(String prompt) {
        while (true) {
            String value = readString(prompt).trim();
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                System.out.println("Введите целое число.");
            }
        }
    }

    public LocalDate readDate(String prompt) {
        while (true) {
            String value = readString(prompt + " (ГГГГ-ММ-ДД): ").trim();
            try {
                return LocalDate.parse(value);
            } catch (DateTimeParseException e) {
                System.out.println("Введите дату в формате ГГГГ-ММ-ДД.");
            }
        }
    }

    public LocalTime readTime(String prompt) {
        while (true) {
            String value = readString(prompt + " (ЧЧ:ММ): ").trim();
            try {
                return LocalTime.parse(value);
            } catch (DateTimeParseException e) {
                System.out.println("Введите время в формате ЧЧ:ММ.");
            }
        }
    }

    public int readIntInRange(String prompt, int min, int max) {
        while (true) {
            int value = readInt(prompt);
            if (value >= min && value <= max) {
                return value;
            }
            System.out.printf("Введите число от %d до %d.%n", min, max);
        }
    }

    public UserRole readRole(String prompt) {
        while (true) {
            String value = readNonEmptyString(prompt).toUpperCase();
            if ("1".equals(value) || "ADMIN".equals(value)) {
                return UserRole.ADMIN;
            }
            if ("2".equals(value) || "OPERATOR".equals(value)) {
                return UserRole.OPERATOR;
            }
            System.out.println("Введите 1/ADMIN или 2/OPERATOR.");
        }
    }

    public VisitStatus readVisitStatus(String prompt) {
        while (true) {
            String value = readNonEmptyString(prompt).toUpperCase();
            try {
                return VisitStatus.valueOf(value);
            } catch (IllegalArgumentException e) {
                System.out.println(
                        "Допустимые статусы: PLANNED, IN_PROGRESS, COMPLETED, CANCELLED."
                );
            }
        }
    }
}
