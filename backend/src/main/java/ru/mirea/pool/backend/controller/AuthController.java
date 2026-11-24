package ru.mirea.pool.backend.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ru.mirea.pool.backend.domain.UserRole;
import ru.mirea.pool.backend.dto.UserResponse;
import ru.mirea.pool.backend.security.JwtAuthenticationFilter;
import ru.mirea.pool.backend.service.AuthService;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthService.Tokens> login(@Valid @RequestBody Login request, HttpServletRequest servletRequest,
            HttpServletResponse response, @RequestHeader(value = "X-Pool-Client", defaultValue = "") String client) {
        return tokenResponse(auth.login(request.username(), request.password(), servletRequest.getRemoteAddr()),
                client, servletRequest, response);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthService.Tokens> register(
            @Valid @RequestBody Register request, HttpServletRequest servletRequest,
            HttpServletResponse response, @RequestHeader(value = "X-Pool-Client", defaultValue = "") String client
    ) {
        ResponseEntity<AuthService.Tokens> tokens = tokenResponse(auth.register(request.username(),
                request.displayName(), request.password(), request.passwordConfirmation()), client, servletRequest, response);
        return ResponseEntity.status(HttpStatus.CREATED).headers(tokens.getHeaders()).body(tokens.getBody());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthService.Tokens> refresh(
            @Valid @RequestBody(required = false) Refresh request,
            @CookieValue(value = "pool_refresh", defaultValue = "") String cookie,
            @RequestHeader(value = "X-Pool-Client", defaultValue = "") String client,
            HttpServletRequest servletRequest, HttpServletResponse response) {
        return tokenResponse(auth.refresh(refreshToken(request, cookie, client)), client, servletRequest, response);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody(required = false) Refresh request,
                       @CookieValue(value = "pool_refresh", defaultValue = "") String cookie,
                       @RequestHeader(value = "X-Pool-Client", defaultValue = "") String client,
                       HttpServletRequest servletRequest, HttpServletResponse response) {
        auth.logout(refreshToken(request, cookie, client));
        response.addHeader("Set-Cookie", refreshCookie("", servletRequest.isSecure(), 0).toString());
        response.addHeader("Set-Cookie", docsCookie("", servletRequest.isSecure(), 0).toString());
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public UserResponse current(@AuthenticationPrincipal Jwt jwt) {
        return auth.current(UUID.fromString(jwt.getSubject()));
    }

    @PostMapping("/change-password")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<AuthService.Tokens> changePassword(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Password request,
            @RequestHeader(value = "X-Pool-Client", defaultValue = "") String client,
            HttpServletRequest servletRequest, HttpServletResponse response
    ) {
        return tokenResponse(auth.changePassword(
                UUID.fromString(jwt.getSubject()), request.oldPassword(), request.newPassword()),
                client, servletRequest, response);
    }

    private ResponseEntity<AuthService.Tokens> tokenResponse(AuthService.Tokens tokens, String client,
            HttpServletRequest request, HttpServletResponse response) {
        if ("web".equals(client)) {
            response.addHeader("Set-Cookie", refreshCookie(tokens.refreshToken(), request.isSecure(), 14 * 86400).toString());
            boolean swaggerAccess = !tokens.user().passwordChangeRequired()
                    && tokens.user().roles().contains(UserRole.ADMIN);
            ResponseCookie swagger = swaggerAccess
                    ? docsCookie(tokens.accessToken(), request.isSecure(), tokens.expiresIn())
                    : docsCookie("", request.isSecure(), 0);
            response.addHeader("Set-Cookie", swagger.toString());
            tokens = new AuthService.Tokens(tokens.accessToken(), null, tokens.expiresIn(), tokens.user());
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(tokens);
    }

    private String refreshToken(Refresh request, String cookie, String client) {
        if (request != null) {
            return request.refreshToken();
        }
        if (!"web".equals(client) || !cookie.matches("[A-Za-z0-9_-]{43}")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Войдите в систему.");
        }
        return cookie;
    }

    private ResponseCookie refreshCookie(String value, boolean secure, long seconds) {
        return ResponseCookie.from("pool_refresh", value).httpOnly(true).secure(secure)
                .sameSite("Strict").path("/api/auth").maxAge(seconds).build();
    }

    private ResponseCookie docsCookie(String value, boolean secure, long seconds) {
        return ResponseCookie.from(JwtAuthenticationFilter.SWAGGER_COOKIE, value).httpOnly(true).secure(secure)
                .sameSite("Strict").path("/").maxAge(seconds).build();
    }

    public record Login(
            @NotBlank @Size(max = 50) String username,
            @NotBlank @Size(max = 72) String password
    ) {
    }

    public record Register(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Size(max = 100) String displayName,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(max = 72) String passwordConfirmation
    ) {
    }

    public record Refresh(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String refreshToken) {
    }

    public record Password(
            @NotBlank @Size(max = 72) String oldPassword,
            @NotBlank @Size(max = 72) String newPassword
    ) {
    }
}
