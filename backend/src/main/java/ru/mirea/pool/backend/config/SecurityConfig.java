package ru.mirea.pool.backend.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import ru.mirea.pool.backend.security.AccountAuthenticationConverter;
import ru.mirea.pool.backend.security.JwtAuthenticationFilter;
import ru.mirea.pool.backend.security.SecurityErrors;

import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    public static final String ISSUER = "pool-backend";

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtDecoder decoder,
            AccountAuthenticationConverter converter,
            SecurityErrors errors
    ) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(errors)
                        .accessDeniedHandler(errors))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/actuator/health", "/", "/index.html", "/assets/**", "/favicon.svg").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register",
                                "/api/auth/refresh", "/api/auth/logout").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .addFilterBefore(new JwtAuthenticationFilter(decoder, converter, errors),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    SecretKey jwtKey(@Value("${app.auth.jwt-secret-base64}") String secret) {
        byte[] key = Base64.getDecoder().decode(secret);
        if (key.length < 32) {
            throw new IllegalArgumentException("Ключ JWT должен содержать не менее 32 байт.");
        }
        return new SecretKeySpec(key, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtKey).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(ISSUER),
                jwt -> jwt.getExpiresAt() != null
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"))
        ));
        return decoder;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
