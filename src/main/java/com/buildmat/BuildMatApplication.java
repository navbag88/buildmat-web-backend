package com.buildmat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // activates SessionService.cleanExpiredSessions cron expression
public class BuildMatApplication {
    public static void main(String[] args) {
        SpringApplication.run(BuildMatApplication.class, args);
    }
}
