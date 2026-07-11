package com.genc.arfoms.crew;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class CrewServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrewServiceApplication.class, args);
    }
}

