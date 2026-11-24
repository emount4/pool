package ru.mirea.pool.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PoolBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PoolBackendApplication.class, args);
    }
}
