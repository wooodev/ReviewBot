package com.reviewbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ReviewBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReviewBotApplication.class, args);
    }
}
