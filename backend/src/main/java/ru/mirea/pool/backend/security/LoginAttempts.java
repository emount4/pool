package ru.mirea.pool.backend.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class LoginAttempts {

    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_TRACKED = 10_000;
    private static final Duration WINDOW = Duration.ofMinutes(10);

    private final Map<String, Attempt> attempts = new HashMap<>();

    public synchronized void register(String username, String address) {
        Instant now = Instant.now();
        attempts.values().removeIf(attempt -> !attempt.expiresAt().isAfter(now));
        String accountKey = "account:" + username.toLowerCase(Locale.ROOT);
        String addressKey = "address:" + address;
        Attempt account = attempts.get(accountKey);
        Attempt source = attempts.get(addressKey);
        if (attempts.size() >= MAX_TRACKED || blocked(account, MAX_ATTEMPTS) || blocked(source, 30)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Слишком много попыток входа. Попробуйте позже.");
        }
        increment(accountKey, account, now);
        increment(addressKey, source, now);
    }

    public synchronized void clear(String username, String address) {
        attempts.remove("account:" + username.toLowerCase(Locale.ROOT));
        attempts.remove("address:" + address);
    }

    private boolean blocked(Attempt attempt, int limit) {
        return attempt != null && attempt.count() >= limit;
    }

    private void increment(String key, Attempt attempt, Instant now) {
        attempts.put(key, attempt == null ? new Attempt(1, now.plus(WINDOW))
                : new Attempt(attempt.count() + 1, attempt.expiresAt()));
    }

    private record Attempt(int count, Instant expiresAt) {
    }
}
