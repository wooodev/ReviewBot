package com.reviewbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
// @EnableJpaAuditing은 JpaAuditingConfig에서 분리 선언 (테스트 슬라이스 충돌 방지)
public class ReviewBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReviewBotApplication.class, args);
    }
}
