package ru.mirea.pool.backend.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import ru.mirea.pool.backend.domain.RefreshSession;
import ru.mirea.pool.backend.domain.WebUser;
import ru.mirea.pool.backend.repository.RefreshSessionRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class AccountAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final RefreshSessionRepository sessions;

    public AccountAuthenticationConverter(RefreshSessionRepository sessions) {
        this.sessions = sessions;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UUID userId;
        UUID sessionId;
        try {
            userId = UUID.fromString(jwt.getSubject());
            sessionId = UUID.fromString(jwt.getClaimAsString("sid"));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidBearerTokenException("Некорректный токен.");
        }

        RefreshSession session = sessions.findForAuthentication(sessionId)
                .filter(value -> value.isUsableAt(Instant.now()))
                .orElseThrow(() -> new InvalidBearerTokenException("Сессия завершена."));
        WebUser user = session.getUser();
        if (!user.isActive() || !user.getId().equals(userId)) {
            throw new InvalidBearerTokenException("Учётная запись недоступна.");
        }

        List<SimpleGrantedAuthority> authorities = user.isPasswordChangeRequired()
                ? List.of() : user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                        .toList();
        return new JwtAuthenticationToken(jwt, authorities, userId.toString());
    }
}
