package com.renaissance.service;

import com.renaissance.config.CacheConfig;
import com.renaissance.controller.AdminController;
import com.renaissance.dto.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CaffeineCacheOptimizationTest {

    @Test
    @DisplayName("CacheConfig properly initializes all 10 tiered Caffeine caches")
    void testCacheConfigInitializesAllCaches() {
        CacheConfig config = new CacheConfig();
        CaffeineCacheManager manager = (CaffeineCacheManager) config.cacheManager();

        assertNotNull(manager);
        assertTrue(manager.getCacheNames().contains(CacheConfig.CACHE_DEPARTMENTS));
        assertTrue(manager.getCacheNames().contains(CacheConfig.CACHE_CLASSES));
        assertTrue(manager.getCacheNames().contains(CacheConfig.CACHE_SUBJECTS));
        assertTrue(manager.getCacheNames().contains(CacheConfig.CACHE_TEACHER_CLASSES));
        assertTrue(manager.getCacheNames().contains(CacheConfig.CACHE_TEACHER_SUBJECTS));
        assertTrue(manager.getCacheNames().contains(CacheConfig.CACHE_TEACHER_PROFILES));
        assertTrue(manager.getCacheNames().contains(CacheConfig.CACHE_TIMETABLE));
        assertTrue(manager.getCacheNames().contains(CacheConfig.CACHE_CLASS_STUDENT_COUNTS));
        assertTrue(manager.getCacheNames().contains(CacheConfig.CACHE_DASHBOARD_STATS));
        assertTrue(manager.getCacheNames().contains(CacheConfig.CACHE_STUDENT_LOOKUPS));
    }

    @Test
    @DisplayName("Caffeine cache puts, retrieves, and evicts entries successfully")
    void testCachePutGetEvict() {
        CacheConfig config = new CacheConfig();
        CaffeineCacheManager manager = (CaffeineCacheManager) config.cacheManager();

        Cache deptCache = manager.getCache(CacheConfig.CACHE_DEPARTMENTS);
        assertNotNull(deptCache);

        // Put into cache
        deptCache.put("dept_cse", "Computer Science and Engineering");
        assertEquals("Computer Science and Engineering", deptCache.get("dept_cse", String.class));

        // Evict
        deptCache.evict("dept_cse");
        assertNull(deptCache.get("dept_cse"));
    }

    @Test
    @DisplayName("AdminController cache metrics endpoint returns telemetry statistics")
    void testCacheMetricsEndpoint() {
        CacheConfig config = new CacheConfig();
        CaffeineCacheManager manager = (CaffeineCacheManager) config.cacheManager();

        // Exercise the cache to generate metrics
        Cache classCache = manager.getCache(CacheConfig.CACHE_CLASSES);
        assertNotNull(classCache);
        classCache.put("c1", "Class 5A");
        classCache.get("c1"); // hit
        classCache.get("c2"); // miss

        AdminController controller = new AdminController(null, null, null, null, null, manager);

        ResponseEntity<ApiResponse<Map<String, Object>>> response = controller.getCacheMetrics();
        assertNotNull(response);
        assertTrue(response.getStatusCode().is2xxSuccessful());

        ApiResponse<Map<String, Object>> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.isSuccess());

        Map<String, Object> data = body.getData();
        assertEquals("Caffeine (In-Process L1 RAM)", data.get("cacheType"));
        assertTrue((int) data.get("totalCaches") >= 10);
        assertNotNull(data.get("overallHitRate"));
        assertNotNull(data.get("caches"));

        @SuppressWarnings("unchecked")
        Map<String, Object> caches = (Map<String, Object>) data.get("caches");
        assertTrue(caches.containsKey(CacheConfig.CACHE_CLASSES));
    }
}
