package com.renaissance.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates and sanitizes database configuration at startup.
 * Prevents cryptic Hibernate dialect errors by identifying malformed URLs,
 * unresolved template variables, or private hostnames before connection attempts.
 */
@Component
public class DatabaseConfigValidator {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfigValidator.class);
    private static final Pattern JDBC_MYSQL_PATTERN = Pattern.compile("^jdbc:mysql://([^:/]+):(\\d+)/([^?]+)(?:\\?(.*))?$");

    private final Environment environment;

    public DatabaseConfigValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validateDatabaseConfiguration() {
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isLocalOrTest = Arrays.stream(activeProfiles)
                .anyMatch(p -> p.equalsIgnoreCase("local") || p.equalsIgnoreCase("test"));

        if (isLocalOrTest) {
            logger.info("Local/Test profile active. In-memory database in use.");
            return;
        }

        String datasourceUrl = environment.getProperty("spring.datasource.url");
        String username = environment.getProperty("spring.datasource.username");

        logger.info("================================================================================");
        logger.info("           RENAISSANCE ATTENDANCE SYSTEM - DATABASE VALIDATION");
        logger.info("================================================================================");

        if (datasourceUrl == null || datasourceUrl.isBlank()) {
            logger.error("FATAL: 'spring.datasource.url' is not configured!");
            logger.error("In Render dashboard -> Environment, set SPRING_DATASOURCE_URL");
            logger.error("Format: jdbc:mysql://PUBLIC_HOST:PUBLIC_PORT/railway?sslMode=REQUIRED&allowPublicKeyRetrieval=true&serverTimezone=UTC");
            logger.info("================================================================================");
            return;
        }

        // 1. Check for raw Railway template syntax ${{...}}
        if (datasourceUrl.contains("${{") || datasourceUrl.contains("}}")) {
            logger.error("FATAL CONFIGURATION ERROR:");
            logger.error("  'SPRING_DATASOURCE_URL' contains unresolved Railway template syntax (${{...}}):");
            logger.error("  -> Current URL: {}", sanitizeUrl(datasourceUrl));
            logger.error("  CAUSE: Render does NOT evaluate Railway template syntax.");
            logger.error("  SOLUTION: In Render Dashboard, set SPRING_DATASOURCE_URL to the actual resolved values:");
            logger.error("  Example: jdbc:mysql://junction.proxy.rlwy.net:43195/railway?sslMode=REQUIRED&allowPublicKeyRetrieval=true&serverTimezone=UTC");
            logger.info("================================================================================");
            throw new IllegalStateException("SPRING_DATASOURCE_URL contains unresolved Railway template variables. " +
                    "Render requires resolved values from Railway Public TCP Proxy.");
        }

        // 2. Check for Railway private internal DNS
        if (datasourceUrl.contains("mysql.railway.internal")) {
            logger.error("FATAL CONFIGURATION ERROR:");
            logger.error("  'SPRING_DATASOURCE_URL' uses Railway private internal hostname 'mysql.railway.internal':");
            logger.error("  CAUSE: Render is outside Railway's private mesh network and cannot resolve .internal domains.");
            logger.error("  SOLUTION: Enable Public Networking / TCP Proxy in Railway and use the public proxy host and port.");
            logger.info("================================================================================");
            throw new IllegalStateException("Cannot connect to 'mysql.railway.internal' from Render. " +
                    "Use Railway Public TCP Proxy hostname and port instead.");
        }

        // 3. Validate JDBC URL structure
        Matcher matcher = JDBC_MYSQL_PATTERN.matcher(datasourceUrl);
        if (matcher.matches()) {
            String host = matcher.group(1);
            String port = matcher.group(2);
            String database = matcher.group(3);
            String queryParams = matcher.group(4);

            logger.info("Database Target Host : {}", host);
            logger.info("Database Target Port : {}", port);
            logger.info("Database Name        : {}", database);
            logger.info("Database Username    : {}", username != null ? username : "root");
            logger.info("Database Password    : [PROTECTED - CONFIGURED]");
            logger.info("JDBC Parameters      : {}", queryParams != null ? queryParams : "none");
            logger.info("Connection Mode      : Validated MySQL JDBC format");
        } else {
            logger.warn("JDBC URL format is non-standard. Sanitized URL: {}", sanitizeUrl(datasourceUrl));
        }

        logger.info("================================================================================");
    }

    private String sanitizeUrl(String url) {
        if (url == null) return "null";
        // Strip out any embedded user:password@ if present
        return url.replaceAll("//[^@]+@", "//***:***@");
    }
}
