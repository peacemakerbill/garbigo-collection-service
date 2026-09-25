package com.garbigo.collection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Entry point for garbigo-collection-service.
 *
 * <p>Owns garbage collection pickup requests, scheduling, collector
 * assignment, and complaint handling. Identity, auth, profiles, and social
 * features live in {@code garbigo-auth-service}; payments are driven by a
 * future {@code garbigo-wallet-service}. See the project README for the
 * full system context and integration contract.
 *
 * <p>{@code @EnableFeignClients} activates the {@code client} package's
 * calls out to auth-service (and, later, wallet-service).
 * {@code @EnableMongoAuditing} is what makes {@code @CreatedDate}/
 * {@code @LastModifiedDate} on the model classes actually get populated.
 */
@SpringBootApplication
@EnableFeignClients
@EnableMongoAuditing
public class GarbigoCollectionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GarbigoCollectionServiceApplication.class, args);
    }
}
