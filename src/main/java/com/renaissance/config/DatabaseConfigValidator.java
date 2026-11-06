package com.renaissance.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates and sanitizes MongoDB database configuration at startup.
 * Prevents connection errors by identifying malformed URIs,
 * unresolved template variables, or missing credentials before connection attempts.
 */
@Component
public class DatabaseConfigValidator {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfigValidator.class);
    private static final Pattern MASK_CREDENTIALS_PATTERN = Pattern.compile("^(mongodb(?:\\+srv)?://)([^:]+):([^@]+)@(.*)$");

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
            logger.info("Local/Test profile active. In-memory or local MongoDB in use.");
            return;
        }

        String mongoUri = environment.getProperty("spring.data.mongodb.uri");

        logger.info("================================================================================");
        logger.info("           RENAISSANCE ATTENDANCE SYSTEM - DATABASE VALIDATION");
        logger.info("================================================================================");

        if (mongoUri == null || mongoUri.isBlank()) {
            logger.error("FATAL: 'spring.data.mongodb.uri' or 'MONGODB_URI' is not configured!");
            logger.error("In Render dashboard -> Environment, set MONGODB_URI");
            logger.error("Format: mongodb+srv://<username>:<password>@<cluster>.mongodb.net/<database>?retryWrites=true&w=majority");
            logger.info("================================================================================");
            return;
        }

        // 1. Check for raw template syntax ${{...}} or unresolved ${...}
        if (mongoUri.contains("${{") || mongoUri.contains("}}") || mongoUri.startsWith("${")) {
            logger.error("FATAL CONFIGURATION ERROR:");
            logger.error("  'MONGODB_URI' contains unresolved template syntax:");
            logger.error("  -> Current URI: {}", sanitizeUri(mongoUri));
            logger.error("  CAUSE: The hosting environment did not provide an evaluated MongoDB URI.");
            logger.error("  SOLUTION: In Render Dashboard, set MONGODB_URI to your actual MongoDB Atlas connection string.");
            logger.info("================================================================================");
            throw new IllegalStateException("MONGODB_URI contains unresolved template variables.");
        }

        // 2. Validate prefix
        if (!mongoUri.startsWith("mongodb://") && !mongoUri.startsWith("mongodb+srv://")) {
            logger.error("FATAL: MONGODB_URI must begin with 'mongodb://' or 'mongodb+srv://'");
            logger.error("Current URI: {}", sanitizeUri(mongoUri));
            throw new IllegalStateException("Invalid MongoDB URI protocol. Expected mongodb:// or mongodb+srv://");
        }

        // 3. Log sanitized URI
        String sanitized = sanitizeUri(mongoUri);
        logger.info("MongoDB Connection URI: {}", sanitized);
        logger.info("Connection Mode       : Validated MongoDB URI format");
        logger.info("Credential Security   : Password masked in logs");
        logger.info("================================================================================");
    }

    public static String sanitizeUri(String uri) {
        if (uri == null) return "null";
        Matcher matcher = MASK_CREDENTIALS_PATTERN.matcher(uri);
        if (matcher.matches()) {
            String prefix = matcher.group(1);
            String user = matcher.group(2);
            String remainder = matcher.group(4);
            return prefix + user + ":***@" + remainder;
        }
        return uri.replaceAll("//[^@]+@", "//***:***@");
    }
}
