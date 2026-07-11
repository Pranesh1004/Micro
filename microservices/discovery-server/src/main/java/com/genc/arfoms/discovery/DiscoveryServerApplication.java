package com.genc.arfoms.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Netflix Eureka service registry. Every ARFOMS microservice registers here so
 * the gateway and inter-service clients can discover each other by service id
 * instead of relying on hardcoded host:port values.
 */
@EnableEurekaServer
@SpringBootApplication
public class DiscoveryServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApplication.class, args);
    }
}

