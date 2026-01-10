package com.umc9th.dumMoney;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

//저장(save)할 때 자동으로 created_at과 updated_at이 DB에 기록해줌
@EnableJpaAuditing
@SpringBootApplication
public class DumApplication {

    public static void main(String[] args) {
        SpringApplication.run(DumApplication.class, args);
    }

}