package ru.mirea.pool.presentation.console;

import ru.mirea.pool.domain.model.UserRole;
import ru.mirea.pool.domain.model.VisitStatus;

import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public final class InputReader {

    private final Scanner scanner;

    public InputReader() {
        Charset inputCharset = Charset.forName(System.getProperty(
                "stdin.encoding", Charset.defaultCharset().name()
        ));
        this.scanner = new Scanner(System.in, inputCharset);
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

    public String readCancellableString(String prompt) {
        String value = readString(prompt);
        throwIfCancelled(value);
        return value;
    }

    public String readCancellableNonEmptyString(String prompt) {
        while (true) {
            String value = readCancellableString(prompt).trim();
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("Значение не может быть пустым.");
        }
    }

    public String readNonEmptyStringWithDefault(String prompt, String defaultValue) {
        String value = readCancellableString(prompt).trim();
        return value.isEmpty() ? defaultValue : value;
    }

    public String readOptionalStringWithDefault(String prompt, String defaultValue) {
        String value = readCancellableString(prompt).trim();
        if (value.isEmpty()) {
            return defaultValue;
        }
        return "-".equals(value) ? "" : value;
    }

    public char[] readCancellablePassword(String prompt) {
        return readCancellableNonEmptyString(prompt).toCharArray();
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

    public long readCancellableLong(String prompt) {
        while (true) {
            String value = readCancellableString(prompt).trim();
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

    public LocalDate readCancellableDate(String prompt) {
        while (true) {
            String value = readCancellableString(prompt + " (ГГГГ-ММ-ДД): ").trim();
            try {
                return LocalDate.parse(value);
            } catch (DateTimeParseException e) {
                System.out.println("Введите дату в формате ГГГГ-ММ-ДД.");
            }
        }
    }

    public LocalDate readDateWithDefault(String prompt, LocalDate defaultValue) {
        while (true) {
            String value = readCancellableString(
                    prompt + " [" + defaultValue + "] (ГГГГ-ММ-ДД): "
            ).trim();
            if (value.isEmpty()) {
                return defaultValue;
            }
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

    public LocalTime readCancellableTime(String prompt) {
        while (true) {
            String value = readCancellableString(prompt + " (ЧЧ:ММ): ").trim();
            try {
                return LocalTime.parse(value);
            } catch (DateTimeParseException e) {
                System.out.println("Введите время в формате ЧЧ:ММ.");
            }
        }
    }

    public LocalTime readTimeWithDefault(String prompt, LocalTime defaultValue) {
        while (true) {
            String value = readCancellableString(
                    prompt + " [" + defaultValue + "] (ЧЧ:ММ): "
            ).trim();
            if (value.isEmpty()) {
                return defaultValue;
            }
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

    public int readCancellableIntInRange(String prompt, int min, int max) {
        while (true) {
            String value = readCancellableString(prompt).trim();
            try {
                int number = Integer.parseInt(value);
                if (number >= min && number <= max) {
                    return number;
                }
            } catch (NumberFormatException ignored) {
                // A single range message is clearer for both invalid cases.
            }
            System.out.printf("Введите число от %d до %d.%n", min, max);
        }
    }

    public int readIntInRangeWithDefault(
            String prompt,
            int min,
            int max,
            int defaultValue
    ) {
        while (true) {
            String value = readCancellableString(
                    prompt + " [" + defaultValue + "]: "
            ).trim();
            if (value.isEmpty()) {
                return defaultValue;
            }
            try {
                int number = Integer.parseInt(value);
                if (number >= min && number <= max) {
                    return number;
                }
            } catch (NumberFormatException ignored) {
                // A single range message is clearer for both invalid cases.
            }
            System.out.printf("Введите число от %d до %d.%n", min, max);
        }
    }

    public UserRole readRole(String prompt) {
        while (true) {
            String value = readCancellableNonEmptyString(prompt).toUpperCase();
            if ("1".equals(value) || "ADMIN".equals(value)) {
                return UserRole.ADMIN;
            }
            if ("2".equals(value) || "OPERATOR".equals(value)) {
                return UserRole.OPERATOR;
            }
            System.out.println("Введите 1/ADMIN или 2/OPERATOR.");
        }
    }

    public UserRole readRoleWithDefault(String prompt, UserRole defaultValue) {
        while (true) {
            String value = readCancellableString(prompt).trim().toUpperCase();
            if (value.isEmpty()) {
                return defaultValue;
            }
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
        System.out.println("1. Запланировано");
        System.out.println("2. В процессе");
        System.out.println("3. Завершено");
        System.out.println("4. Отменено");
        while (true) {
            String value = readCancellableNonEmptyString(prompt).toUpperCase();
            VisitStatus status = switch (value) {
                case "1" -> VisitStatus.PLANNED;
                case "2" -> VisitStatus.IN_PROGRESS;
                case "3" -> VisitStatus.COMPLETED;
                case "4" -> VisitStatus.CANCELLED;
                default -> null;
            };
            if (status != null) {
                return status;
            }
            try {
                return VisitStatus.valueOf(value);
            } catch (IllegalArgumentException e) {
                System.out.println(
                        "Введите число от 1 до 4."
                );
            }
        }
    }

    public boolean readConfirmation(String prompt, boolean defaultValue) {
        while (true) {
            String value = readCancellableString(prompt).trim().toLowerCase();
            if (value.isEmpty()) {
                return defaultValue;
            }
            if ("да".equals(value) || "д".equals(value) || "yes".equals(value)
                    || "y".equals(value)) {
                return true;
            }
            if ("нет".equals(value) || "н".equals(value) || "no".equals(value)
                    || "n".equals(value)) {
                return false;
            }
            System.out.println("Введите «да» или «нет».");
        }
    }

    private void throwIfCancelled(String value) {
        String normalized = value.trim();
        if ("0".equals(normalized) || "назад".equalsIgnoreCase(normalized)) {
            throw new OperationCancelledException();
        }
    }
}
