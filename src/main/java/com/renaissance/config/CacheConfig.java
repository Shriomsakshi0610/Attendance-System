package com.renaissance.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * In-memory Spring Cache configuration for static catalog metadata:
 * departments, classes, and subjects.
 * Eliminates redundant network calls to MongoDB Atlas across page navigation.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_DEPARTMENTS = "departments";
    public static final String CACHE_CLASSES = "classes";
    public static final String CACHE_SUBJECTS = "subjects";
    public static final String CACHE_TIMETABLE = "timetable";
    public static final String CACHE_DASHBOARD_STATS = "dashboard_stats";

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager manager = new ConcurrentMapCacheManager();
        manager.setCacheNames(List.of(CACHE_DEPARTMENTS, CACHE_CLASSES, CACHE_SUBJECTS, CACHE_TIMETABLE, CACHE_DASHBOARD_STATS));
        return manager;
    }
}
