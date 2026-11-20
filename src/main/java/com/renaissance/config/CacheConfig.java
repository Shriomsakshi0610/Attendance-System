package com.renaissance.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;

/**
 * High-Performance In-Memory Caffeine Cache Configuration.
 * Replaces un-bounded ConcurrentMapCacheManager with bounded, TTL-expired,
 * LRU-evicting caches with Micrometer stats recording.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_DEPARTMENTS = "departments";
    public static final String CACHE_CLASSES = "classes";
    public static final String CACHE_SUBJECTS = "subjects";
    public static final String CACHE_TEACHER_CLASSES = "teacher_classes";
    public static final String CACHE_TEACHER_SUBJECTS = "teacher_subjects";
    public static final String CACHE_TEACHER_PROFILES = "teacher_profiles";
    public static final String CACHE_TIMETABLE = "timetable";
    public static final String CACHE_CLASS_STUDENT_COUNTS = "class_student_counts";
    public static final String CACHE_DASHBOARD_STATS = "dashboard_stats";
    public static final String CACHE_STUDENT_LOOKUPS = "student_lookups";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        // 1. Static Catalog Metadata: Long TTL (6 hours), low mutation rate
        manager.registerCustomCache(CACHE_DEPARTMENTS,
                Caffeine.newBuilder()
                        .maximumSize(100)
                        .expireAfterWrite(Duration.ofHours(6))
                        .recordStats()
                        .build());

        manager.registerCustomCache(CACHE_CLASSES,
                Caffeine.newBuilder()
                        .maximumSize(200)
                        .expireAfterWrite(Duration.ofHours(6))
                        .recordStats()
                        .build());

        manager.registerCustomCache(CACHE_SUBJECTS,
                Caffeine.newBuilder()
                        .maximumSize(500)
                        .expireAfterWrite(Duration.ofHours(6))
                        .recordStats()
                        .build());

        // 2. Teacher Assignments & Profiles: Medium TTL (15 minutes), evict on admin edits
        manager.registerCustomCache(CACHE_TEACHER_CLASSES,
                Caffeine.newBuilder()
                        .maximumSize(300)
                        .expireAfterWrite(Duration.ofMinutes(15))
                        .recordStats()
                        .build());

        manager.registerCustomCache(CACHE_TEACHER_SUBJECTS,
                Caffeine.newBuilder()
                        .maximumSize(300)
                        .expireAfterWrite(Duration.ofMinutes(15))
                        .recordStats()
                        .build());

        manager.registerCustomCache(CACHE_TEACHER_PROFILES,
                Caffeine.newBuilder()
                        .maximumSize(300)
                        .expireAfterWrite(Duration.ofMinutes(15))
                        .recordStats()
                        .build());

        // 3. Timetable Schedules: Medium TTL (2 hours)
        manager.registerCustomCache(CACHE_TIMETABLE,
                Caffeine.newBuilder()
                        .maximumSize(500)
                        .expireAfterWrite(Duration.ofHours(2))
                        .recordStats()
                        .build());

        // 4. Class Student Enrollment Counts: Medium TTL (2 hours)
        manager.registerCustomCache(CACHE_CLASS_STUDENT_COUNTS,
                Caffeine.newBuilder()
                        .maximumSize(200)
                        .expireAfterWrite(Duration.ofHours(2))
                        .recordStats()
                        .build());

        // 5. Dashboard Summary Statistics: 15-minute TTL in RAM, event-driven eviction on attendance/session changes
        manager.registerCustomCache(CACHE_DASHBOARD_STATS,
                Caffeine.newBuilder()
                        .maximumSize(500)
                        .expireAfterWrite(Duration.ofMinutes(15))
                        .recordStats()
                        .build());

        // 6. Student USN / Academic Lookups: Medium TTL (15 minutes)
        manager.registerCustomCache(CACHE_STUDENT_LOOKUPS,
                Caffeine.newBuilder()
                        .maximumSize(1000)
                        .expireAfterWrite(Duration.ofMinutes(15))
                        .recordStats()
                        .build());

        return manager;
    }
}
