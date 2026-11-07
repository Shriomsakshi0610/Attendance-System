package com.renaissance.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.TimeUnit;

/**
 * Production and runtime MongoDB Connection Pool Configuration.
 * Maintains pre-warmed connection pool and optimizes timeouts to mitigate
 * Atlas WAN latency and eliminate cold-start handshake delays.
 */
@Configuration
@Profile("!test")
public class MongoConfig {

    @Value("${spring.data.mongodb.uri:mongodb://localhost:27017/renaissance_db}")
    private String mongoUri;

    @Bean
    public MongoClient mongoClient() {
        ConnectionString connectionString = new ConnectionString(mongoUri);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .applyToConnectionPoolSettings(builder -> builder
                        .minSize(5)
                        .maxSize(50)
                        .maxWaitTime(5000, TimeUnit.MILLISECONDS)
                        .maxConnectionIdleTime(60000, TimeUnit.MILLISECONDS)
                        .maxConnectionLifeTime(30, TimeUnit.MINUTES)
                )
                .applyToSocketSettings(builder -> builder
                        .connectTimeout(5000, TimeUnit.MILLISECONDS)
                        .readTimeout(15000, TimeUnit.MILLISECONDS)
                )
                .applyToClusterSettings(builder -> builder
                        .serverSelectionTimeout(5000, TimeUnit.MILLISECONDS)
                )
                .build();

        return MongoClients.create(settings);
    }
}
