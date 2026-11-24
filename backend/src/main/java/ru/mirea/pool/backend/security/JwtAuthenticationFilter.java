package ru.mirea.pool.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String SWAGGER_COOKIE = "pool_docs";

    private final DefaultBearerTokenResolver bearerTokens = new DefaultBearerTokenResolver();
    private final JwtDecoder decoder;
    private final AccountAuthenticationConverter accounts;
    private final SecurityErrors errors;

    public JwtAuthenticationFilter(JwtDecoder decoder, AccountAuthenticationConverter accounts, SecurityErrors errors) {
        this.decoder = decoder;
        this.accounts = accounts;
        this.errors = errors;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String token = bearerTokens.resolve(request);
            if (token == null) {
                token = swaggerToken(request);
            }
            if (token != null) {
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(accounts.convert(decoder.decode(token)));
                SecurityContextHolder.setContext(context);
            }
        } catch (JwtException exception) {
            SecurityContextHolder.clearContext();
            errors.commence(request, response, new InvalidBearerTokenException("Некорректный токен.", exception));
            return;
        } catch (AuthenticationException exception) {
            SecurityContextHolder.clearContext();
            errors.commence(request, response, exception);
            return;
        }
        chain.doFilter(request, response);
    }

    private String swaggerToken(HttpServletRequest request) {
        String path = request.getServletPath();
        boolean swagger = path.equals("/swagger-ui.html") || path.startsWith("/swagger-ui/")
                || path.equals("/v3/api-docs") || path.startsWith("/v3/api-docs/");
        Cookie[] cookies = request.getCookies();
        if (swagger && cookies != null) {
            for (Cookie cookie : cookies) {
                if (SWAGGER_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
