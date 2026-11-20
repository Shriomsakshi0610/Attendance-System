package com.renaissance.service;

import com.renaissance.entity.Teacher;
import com.renaissance.event.TeacherLoginEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class CacheWarmUpService {

    private static final Logger log = LoggerFactory.getLogger(CacheWarmUpService.class);

    private final TeacherService teacherService;
    private final ReportService reportService;
    private final TimetableService timetableService;
    private final ExecutorService warmUpExecutor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "cache-warmup-worker");
        t.setDaemon(true);
        return t;
    });

    public CacheWarmUpService(TeacherService teacherService,
                              ReportService reportService,
                              TimetableService timetableService) {
        this.teacherService = teacherService;
        this.reportService = reportService;
        this.timetableService = timetableService;
    }

    /**
     * Application startup cache warm-up.
     * Pre-loads base static catalogs (departments, classes, subjects, global stats) into RAM.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("[CACHE-WARMUP] Pre-warming global metadata and catalogs on application startup...");
                long start = System.currentTimeMillis();
                teacherService.getAllDepartments();
                teacherService.getClassesByDepartment(null);
                teacherService.getSubjects(null, null);
                reportService.warmDashboardStats(null);
                long duration = System.currentTimeMillis() - start;
                log.info("[CACHE-WARMUP] Global cache warm-up completed in {} ms.", duration);
            } catch (Exception e) {
                log.warn("[CACHE-WARMUP] Startup cache warm-up encountered non-critical error: {}", e.getMessage());
            }
        }, warmUpExecutor);
    }

    /**
     * Listens for successful teacher login or silent renewal events to proactively warm cache.
     */
    @EventListener
    public void onTeacherLogin(TeacherLoginEvent event) {
        if (event != null && event.getTeacher() != null) {
            warmTeacherDashboard(event.getTeacher());
        }
    }

    /**
     * Teacher-specific cache warm-up upon successful login or silent renewal.
     * Proactively pre-calculates and caches dashboard statistics, assigned classes,
     * assigned subjects, and timetable schedule in application RAM.
     */
    public void warmTeacherDashboard(Teacher teacher) {
        if (teacher == null || teacher.getId() == null) return;

        CompletableFuture.runAsync(() -> {
            try {
                long start = System.currentTimeMillis();
                log.debug("[CACHE-WARMUP] Asynchronously warming cache for teacher: {}", teacher.getEmail());

                // 1. Warm dashboard statistics
                reportService.warmDashboardStats(teacher.getId());

                // 2. Warm assigned classes and subjects
                teacherService.getClassesForTeacher(teacher, null);
                teacherService.getSubjectsForTeacher(teacher, null, null);

                // 3. Warm timetable schedule
                timetableService.getSchedulesByTeacher(teacher.getId());

                long duration = System.currentTimeMillis() - start;
                log.debug("[CACHE-WARMUP] Teacher cache pre-warmed for {} in {} ms.", teacher.getEmail(), duration);
            } catch (Exception e) {
                log.warn("[CACHE-WARMUP] Teacher cache warm-up encountered non-critical error for {}: {}",
                        teacher.getEmail(), e.getMessage());
            }
        }, warmUpExecutor);
    }
}
