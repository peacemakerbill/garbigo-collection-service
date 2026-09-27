package com.garbigo.collection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@EnableMongoAuditing
@EnableScheduling
public class GarbigoCollectionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GarbigoCollectionServiceApplication.class, args);
    }
}