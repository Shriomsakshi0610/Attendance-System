package com.renaissance.config;

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

import java.net.InetSocketAddress;

@TestConfiguration
public class MongoTestConfig {

    @Bean(destroyMethod = "shutdown")
    public MongoServer mongoServer() {
        MongoServer server = new MongoServer(new MemoryBackend());
        server.bind("localhost", 0);
        return server;
    }

    @Bean
    @Primary
    public MongoDatabaseFactory mongoDatabaseFactory(MongoServer mongoServer) {
        InetSocketAddress serverAddress = (InetSocketAddress) mongoServer.getLocalAddress();
        String connectionString = String.format("mongodb://localhost:%d/renaissance_test_db", serverAddress.getPort());
        return new SimpleMongoClientDatabaseFactory(connectionString);
    }
}
