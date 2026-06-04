package com.campushub;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.campushub.mapper")
@EnableScheduling
public class CampusHubApplication {
    public static void main(String[] args) {
        SpringApplication.run(CampusHubApplication.class, args);
    }
}
