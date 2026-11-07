package com.renaissance.service;

import com.renaissance.config.CacheConfig;
import com.renaissance.dto.CreateTimetableScheduleRequest;
import com.renaissance.dto.TimetableScheduleDto;
import com.renaissance.entity.CollegeClass;
import com.renaissance.entity.Department;
import com.renaissance.entity.Subject;
import com.renaissance.entity.Teacher;
import com.renaissance.entity.TimetableSchedule;
import com.renaissance.exception.ResourceNotFoundException;
import com.renaissance.repository.ClassRepository;
import com.renaissance.repository.SubjectRepository;
import com.renaissance.repository.TeacherRepository;
import com.renaissance.repository.TimetableScheduleRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.core.convert.LazyLoadingProxy;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TimetableService {

    private final TimetableScheduleRepository timetableRepository;
    private final ClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;

    public TimetableService(TimetableScheduleRepository timetableRepository,
                            ClassRepository classRepository,
                            SubjectRepository subjectRepository,
                            TeacherRepository teacherRepository) {
        this.timetableRepository = timetableRepository;
        this.classRepository = classRepository;
        this.subjectRepository = subjectRepository;
        this.teacherRepository = teacherRepository;
    }

    public List<TimetableScheduleDto> getAllSchedules() {
        return timetableRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Cacheable(value = CacheConfig.CACHE_TIMETABLE, key = "'CLASS_' + #classId", unless = "#result == null")
    public List<TimetableScheduleDto> getSchedulesByClass(String classId) {
        return timetableRepository.findByCollegeClass_Id(classId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Cacheable(value = CacheConfig.CACHE_TIMETABLE, key = "'TEACHER_' + #teacherId", unless = "#result == null")
    public List<TimetableScheduleDto> getSchedulesByTeacher(String teacherId) {
        if (teacherId == null || teacherId.isBlank()) {
            return Collections.emptyList();
        }

        List<TimetableSchedule> schedules = timetableRepository.findByTeacher_Id(teacherId);

        // Robust DBRef fallback: in case Mongo DBRef representation requires proxy extraction
        if (schedules == null || schedules.isEmpty()) {
            List<TimetableSchedule> all = timetableRepository.findAll();
            schedules = all.stream()
                    .filter(s -> s.getTeacher() != null && teacherId.equals(extractTeacherId(s.getTeacher())))
                    .collect(Collectors.toList());
        }

        return schedules.stream()
                .filter(TimetableSchedule::isActive)
                .sorted(Comparator.comparing(this::dayOrder)
                        .thenComparing(s -> s.getStartTime() != null ? s.getStartTime() : LocalTime.MIN))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<TimetableScheduleDto> getSchedulesByDepartment(String departmentId) {
        return timetableRepository.findByDepartment_Id(departmentId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @CacheEvict(value = CacheConfig.CACHE_TIMETABLE, allEntries = true)
    public TimetableScheduleDto createSchedule(CreateTimetableScheduleRequest request) {
        CollegeClass collegeClass = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with ID: " + request.getClassId()));

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + request.getSubjectId()));

        Teacher teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with ID: " + request.getTeacherId()));

        Department department = collegeClass.getDepartment();

        LocalTime startTime = parseTime(request.getStartTime());
        LocalTime endTime = parseTime(request.getEndTime());

        validateTimeAndConflicts(null, collegeClass, teacher, request.getDayOfWeek(), startTime, endTime, request.getRoomNumber());

        Integer periodNumber = request.getPeriodNumber() != null ?
                request.getPeriodNumber() : derivePeriodNumber(startTime);

        TimetableSchedule schedule = new TimetableSchedule(
                department,
                collegeClass,
                subject,
                teacher,
                request.getDayOfWeek().trim().toUpperCase(),
                periodNumber,
                startTime,
                endTime,
                request.getRoomNumber()
        );

        TimetableSchedule saved = timetableRepository.save(schedule);
        return toDto(saved);
    }

    @CacheEvict(value = CacheConfig.CACHE_TIMETABLE, allEntries = true)
    public TimetableScheduleDto updateSchedule(String id, CreateTimetableScheduleRequest request) {
        TimetableSchedule schedule = timetableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with ID: " + id));

        CollegeClass collegeClass = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with ID: " + request.getClassId()));

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + request.getSubjectId()));

        Teacher teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with ID: " + request.getTeacherId()));

        LocalTime startTime = parseTime(request.getStartTime());
        LocalTime endTime = parseTime(request.getEndTime());

        validateTimeAndConflicts(id, collegeClass, teacher, request.getDayOfWeek(), startTime, endTime, request.getRoomNumber());

        schedule.setCollegeClass(collegeClass);
        schedule.setDepartment(collegeClass.getDepartment());
        schedule.setSubject(subject);
        schedule.setTeacher(teacher);
        schedule.setDayOfWeek(request.getDayOfWeek().trim().toUpperCase());
        schedule.setStartTime(startTime);
        schedule.setEndTime(endTime);
        schedule.setRoomNumber(request.getRoomNumber() != null ? request.getRoomNumber().trim() : null);

        if (request.getPeriodNumber() != null) {
            schedule.setPeriodNumber(request.getPeriodNumber());
        } else if (schedule.getPeriodNumber() == null) {
            schedule.setPeriodNumber(derivePeriodNumber(startTime));
        }

        TimetableSchedule updated = timetableRepository.save(schedule);
        return toDto(updated);
    }

    @CacheEvict(value = CacheConfig.CACHE_TIMETABLE, allEntries = true)
    public void deleteSchedule(String id) {
        if (!timetableRepository.existsById(id)) {
            throw new ResourceNotFoundException("Schedule not found with ID: " + id);
        }
        timetableRepository.deleteById(id);
    }

    private void validateTimeAndConflicts(String currentScheduleId, CollegeClass collegeClass,
                                          Teacher teacher, String dayOfWeekStr,
                                          LocalTime startTime, LocalTime endTime, String roomNumber) {
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Schedule start time (" + startTime + ") must be before end time (" + endTime + ").");
        }

        String dayUpper = dayOfWeekStr != null ? dayOfWeekStr.trim().toUpperCase() : "";
        if (dayUpper.isEmpty()) {
            throw new IllegalArgumentException("Day of week is required.");
        }

        List<TimetableSchedule> existingDaySchedules = timetableRepository.findByDayOfWeek(dayUpper);

        for (TimetableSchedule existing : existingDaySchedules) {
            // Ignore self if updating
            if (currentScheduleId != null && currentScheduleId.equals(existing.getId())) {
                continue;
            }

            // Check if time intervals overlap: start1 < end2 && end1 > start2
            boolean overlaps = startTime.isBefore(existing.getEndTime()) && endTime.isAfter(existing.getStartTime());
            if (!overlaps) {
                continue;
            }

            // Conflict 1: Teacher overlap
            if (existing.getTeacher() != null && teacher.getId().equals(existing.getTeacher().getId())) {
                throw new IllegalArgumentException("Scheduling Conflict: Teacher " + teacher.getName() +
                        " is already scheduled for " +
                        (existing.getSubject() != null ? existing.getSubject().getName() : "another subject") +
                        " on " + dayUpper + " from " + existing.getStartTime() + " to " + existing.getEndTime() + ".");
            }

            // Conflict 2: Class/Section overlap
            if (existing.getCollegeClass() != null && collegeClass.getId().equals(existing.getCollegeClass().getId())) {
                throw new IllegalArgumentException("Scheduling Conflict: Class " + collegeClass.getDisplayName() +
                        " already has " +
                        (existing.getSubject() != null ? existing.getSubject().getName() : "another class") +
                        " scheduled on " + dayUpper + " from " + existing.getStartTime() + " to " + existing.getEndTime() + ".");
            }

            // Conflict 3: Room overlap
            if (roomNumber != null && !roomNumber.isBlank() &&
                    roomNumber.trim().equalsIgnoreCase(existing.getRoomNumber())) {
                throw new IllegalArgumentException("Scheduling Conflict: Room " + roomNumber.trim() +
                        " is already occupied on " + dayUpper + " from " + existing.getStartTime() +
                        " to " + existing.getEndTime() + ".");
            }
        }
    }

    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) {
            throw new IllegalArgumentException("Time string is required.");
        }
        try {
            timeStr = timeStr.trim();
            if (timeStr.length() == 5) { // "HH:mm"
                return LocalTime.parse(timeStr);
            } else if (timeStr.length() == 8) { // "HH:mm:ss"
                return LocalTime.parse(timeStr);
            }
            return LocalTime.parse(timeStr);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid time format: '" + timeStr + "'. Expected format: HH:mm (e.g. 09:30)");
        }
    }

    public TimetableScheduleDto toDto(TimetableSchedule schedule) {
        if (schedule == null) return null;
        TimetableScheduleDto dto = new TimetableScheduleDto();
        dto.setId(schedule.getId());
        if (schedule.getDepartment() != null) {
            dto.setDepartmentId(schedule.getDepartment().getId());
            dto.setDepartmentName(schedule.getDepartment().getName());
        }
        if (schedule.getCollegeClass() != null) {
            dto.setClassId(schedule.getCollegeClass().getId());
            dto.setClassName(schedule.getCollegeClass().getDisplayName());
            dto.setSemester(schedule.getCollegeClass().getSemester());
            dto.setSection(schedule.getCollegeClass().getSection());
        }
        if (schedule.getSubject() != null) {
            dto.setSubjectId(schedule.getSubject().getId());
            dto.setSubjectCode(schedule.getSubject().getCode());
            dto.setSubjectName(schedule.getSubject().getName());
        }
        if (schedule.getTeacher() != null) {
            dto.setTeacherId(schedule.getTeacher().getId());
            dto.setTeacherName(schedule.getTeacher().getName());
        }
        String dayStr = schedule.getDayOfWeek() != null ? schedule.getDayOfWeek().trim().toUpperCase() : "MONDAY";
        try {
            dto.setDayOfWeek(DayOfWeek.valueOf(dayStr));
        } catch (Exception ignored) {}
        dto.setDayName(dayStr);

        dto.setStartTime(schedule.getStartTime());
        dto.setEndTime(schedule.getEndTime());
        dto.setRoomNumber(schedule.getRoomNumber());
        dto.setStatus(schedule.getStatus());

        if (schedule.getPeriodNumber() != null) {
            dto.setPeriodNumber(schedule.getPeriodNumber());
        } else {
            dto.setPeriodNumber(derivePeriodNumber(schedule.getStartTime()));
        }

        return dto;
    }

    public static int derivePeriodNumber(LocalTime time) {
        if (time == null) return 1;
        int hour = time.getHour();
        if (hour <= 9) return 1;
        if (hour == 10) return 2;
        if (hour == 11) return 3;
        if (hour == 12) return 4;
        if (hour == 13 || hour == 14) return 5;
        if (hour == 15) return 6;
        return 7;
    }

    private int dayOrder(TimetableSchedule s) {
        if (s == null || s.getDayOfWeek() == null) return 7;
        return switch (s.getDayOfWeek().trim().toUpperCase()) {
            case "MONDAY" -> 1;
            case "TUESDAY" -> 2;
            case "WEDNESDAY" -> 3;
            case "THURSDAY" -> 4;
            case "FRIDAY" -> 5;
            case "SATURDAY" -> 6;
            case "SUNDAY" -> 7;
            default -> 8;
        };
    }

    private String extractTeacherId(Teacher teacher) {
        if (teacher == null) return null;
        if (teacher instanceof LazyLoadingProxy proxy) {
            try {
                com.mongodb.DBRef dbRef = proxy.toDBRef();
                if (dbRef != null && dbRef.getId() != null) {
                    return dbRef.getId().toString();
                }
            } catch (Exception ignored) {}
        }
        return teacher.getId();
    }
}
