package de.shopitech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class DailyDevApplication {
    public static void main(String[] args) {
        SpringApplication.run(DailyDevApplication.class, args);
    }
}