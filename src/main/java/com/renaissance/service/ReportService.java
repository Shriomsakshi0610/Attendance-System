package com.renaissance.service;

import com.renaissance.dto.AttendanceSummaryDto;
import com.renaissance.dto.ClassDto;
import com.renaissance.dto.DashboardStatsDto;
import com.renaissance.dto.DateWiseAttendanceRecordDto;
import com.renaissance.dto.SessionDto;
import com.renaissance.dto.StudentProfileDto;
import com.renaissance.dto.SubjectDto;
import com.renaissance.entity.AttendanceRecord;
import com.renaissance.entity.AttendanceSession;
import com.renaissance.entity.CollegeClass;
import com.renaissance.entity.Department;
import com.renaissance.entity.Student;
import com.renaissance.entity.Subject;
import com.renaissance.entity.Teacher;
import com.renaissance.exception.ResourceNotFoundException;
import com.renaissance.exception.UnauthorizedException;
import com.renaissance.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final StudentRepository studentRepository;
    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final ClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final SessionService sessionService;
    private final AuthService authService;
    private final TeacherService teacherService;
    private final TeacherRepository teacherRepository;

    public ReportService(StudentRepository studentRepository,
                         AttendanceSessionRepository sessionRepository,
                         AttendanceRecordRepository recordRepository,
                         ClassRepository classRepository,
                         SubjectRepository subjectRepository,
                         SessionService sessionService,
                         AuthService authService,
                         TeacherService teacherService,
                         TeacherRepository teacherRepository) {
        this.studentRepository = studentRepository;
        this.sessionRepository = sessionRepository;
        this.recordRepository = recordRepository;
        this.classRepository = classRepository;
        this.subjectRepository = subjectRepository;
        this.sessionService = sessionService;
        this.authService = authService;
        this.teacherService = teacherService;
        this.teacherRepository = teacherRepository;
    }

    private Teacher getAuthorizedTeacherOrNull() {
        try {
            return authService.getCurrentTeacher();
        } catch (Exception e) {
            return null;
        }
    }

    public List<AttendanceSummaryDto> getAttendanceSummary(String classId, String subjectId) {
        return getAttendanceSummary(classId, subjectId, null, null);
    }

    public List<AttendanceSummaryDto> getAttendanceSummary(String classId, String subjectId, LocalDate startDate, LocalDate endDate) {
        Teacher teacher = getAuthorizedTeacherOrNull();
        Set<String> assignedClassIds = (teacher != null && !teacher.isAdmin() && teacher.getAssignedClasses() != null && !teacher.getAssignedClasses().isEmpty())
                ? teacher.getAssignedClasses().stream().map(CollegeClass::getId).collect(Collectors.toSet())
                : null;

        if (assignedClassIds != null && classId != null && !classId.isBlank()) {
            if (!assignedClassIds.contains(classId)) {
                throw new UnauthorizedException("Access Denied: You are not assigned to class " + classId);
            }
        }

        List<Student> students;
        if (classId != null && !classId.isBlank()) {
            students = studentRepository.findByCollegeClassId(classId);
        } else if (assignedClassIds != null && !assignedClassIds.isEmpty()) {
            students = studentRepository.findByCollegeClassIdIn(assignedClassIds);
        } else {
            students = studentRepository.findAll();
        }

        // Conducted sessions for this class and optional subject
        List<AttendanceSession> conductedSessions;
        if (classId != null && !classId.isBlank() && subjectId != null && !subjectId.isBlank()) {
            conductedSessions = sessionRepository.findByCollegeClassIdAndSubjectIdOrderByStartTimeAsc(classId, subjectId);
        } else if (classId != null && !classId.isBlank()) {
            conductedSessions = sessionRepository.findByCollegeClassIdOrderByStartTimeDesc(classId);
        } else if (assignedClassIds != null && !assignedClassIds.isEmpty()) {
            conductedSessions = sessionRepository.findByCollegeClassIdIn(assignedClassIds);
            if (subjectId != null && !subjectId.isBlank()) {
                conductedSessions = conductedSessions.stream()
                        .filter(s -> s.getSubject() != null && subjectId.equals(s.getSubject().getId()))
                        .collect(Collectors.toList());
            }
        } else {
            conductedSessions = sessionRepository.findAll();
            if (subjectId != null && !subjectId.isBlank()) {
                conductedSessions = conductedSessions.stream()
                        .filter(s -> s.getSubject() != null && subjectId.equals(s.getSubject().getId()))
                        .collect(Collectors.toList());
            }
        }

        // Filter by date range if provided
        LocalDateTime startDt = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDt = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        if (startDt != null || endDt != null) {
            conductedSessions = conductedSessions.stream().filter(s -> {
                if (s.getStartTime() == null) return false;
                if (startDt != null && s.getStartTime().isBefore(startDt)) return false;
                if (endDt != null && s.getStartTime().isAfter(endDt)) return false;
                return true;
            }).collect(Collectors.toList());
        }

        List<AttendanceSession> validSessions = conductedSessions.stream()
                .filter(s -> s.getStatus() == AttendanceSession.SessionStatus.COMPLETED || s.getStatus() == AttendanceSession.SessionStatus.ACTIVE)
                .collect(Collectors.toList());

        long totalConducted = validSessions.size();
        Set<String> validSessionIds = validSessions.stream().map(AttendanceSession::getId).collect(Collectors.toSet());

        // Optimized present counts
        Map<String, Long> presentCounts;
        if (startDate == null && endDate == null && classId != null && !classId.isBlank()) {
            presentCounts = recordRepository.countPresentByStudentsInClass(classId, subjectId);
        } else {
            presentCounts = new HashMap<>();
            for (String sessId : validSessionIds) {
                List<AttendanceRecord> recs = recordRepository.findBySessionIdOrderByMarkedAtDesc(sessId);
                for (AttendanceRecord r : recs) {
                    if (r.getStatus() == AttendanceRecord.AttendanceStatus.PRESENT && r.getStudent() != null) {
                        String sId = r.getStudent().getId();
                        presentCounts.put(sId, presentCounts.getOrDefault(sId, 0L) + 1L);
                    }
                }
            }
        }

        String subjectName = (subjectId != null && !conductedSessions.isEmpty() && conductedSessions.get(0).getSubject() != null)
                ? conductedSessions.get(0).getSubject().getName() : "All Subjects";

        CollegeClass cachedClass = (classId != null && !classId.isBlank()) ? classRepository.findById(classId).orElse(null) : null;
        Department cachedDept = (cachedClass != null && cachedClass.getDepartment() != null) ? cachedClass.getDepartment() : null;
        int defaultSemester = cachedClass != null && cachedClass.getSemester() != null ? cachedClass.getSemester() : 0;
        String defaultSection = cachedClass != null && cachedClass.getSection() != null ? cachedClass.getSection() : "-";
        String defaultDeptCode = cachedDept != null && cachedDept.getCode() != null ? cachedDept.getCode() : "N/A";

        List<AttendanceSummaryDto> summaries = new ArrayList<>(students.size());
        for (Student student : students) {
            long attended = presentCounts.getOrDefault(student.getId(), 0L);
            long absent = Math.max(0, totalConducted - attended);
            double percentage = totalConducted > 0 ? ((double) attended / totalConducted) * 100.0 : 100.0;

            String deptCode = defaultDeptCode;
            int sem = defaultSemester;
            String sec = defaultSection;

            if (classId == null && student.getCollegeClass() != null) {
                sem = student.getCollegeClass().getSemester() != null ? student.getCollegeClass().getSemester() : 0;
                sec = student.getCollegeClass().getSection() != null ? student.getCollegeClass().getSection() : "-";
            }
            if (classId == null && student.getDepartment() != null && student.getDepartment().getCode() != null) {
                deptCode = student.getDepartment().getCode();
            }

            summaries.add(new AttendanceSummaryDto(
                    student.getId(),
                    student.getUsn(),
                    student.getName(),
                    deptCode,
                    sem,
                    sec,
                    subjectName,
                    totalConducted,
                    attended,
                    absent,
                    percentage
            ));
        }

        return summaries;
    }

    public StudentProfileDto getStudentProfile(String studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + studentId));

        Teacher teacher = getAuthorizedTeacherOrNull();
        if (teacher != null && !teacher.isAdmin() && teacher.getAssignedClasses() != null && !teacher.getAssignedClasses().isEmpty()) {
            Set<String> assignedClassIds = teacher.getAssignedClasses().stream().map(CollegeClass::getId).collect(Collectors.toSet());
            if (student.getCollegeClass() == null || !assignedClassIds.contains(student.getCollegeClass().getId())) {
                throw new UnauthorizedException("Access Denied: You do not have permission to view attendance records for this student.");
            }
        }

        List<AttendanceRecord> records = recordRepository.findByStudentIdOrderByMarkedAtDesc(studentId);
        List<AttendanceSession> classSessions = (student.getCollegeClass() != null)
                ? sessionRepository.findByCollegeClassIdOrderByStartTimeDesc(student.getCollegeClass().getId())
                : Collections.emptyList();

        classSessions = classSessions.stream()
                .filter(s -> s.getStatus() == AttendanceSession.SessionStatus.COMPLETED || s.getStatus() == AttendanceSession.SessionStatus.ACTIVE)
                .collect(Collectors.toList());

        long totalConducted = classSessions.size();
        long totalPresent = records.stream().filter(r -> r.getStatus() == AttendanceRecord.AttendanceStatus.PRESENT).count();
        long totalAbsent = Math.max(0, totalConducted - totalPresent);
        double overallPercentage = totalConducted > 0 ? ((double) totalPresent / totalConducted) * 100.0 : 100.0;

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("hh:mm a");

        List<StudentProfileDto.HistoryRecordDto> history = records.stream().map(r -> {
            AttendanceSession s = r.getSession();
            return new StudentProfileDto.HistoryRecordDto(
                    s != null ? s.getId() : null,
                    s != null ? s.getSessionCode() : "-",
                    (s != null && s.getSubject() != null) ? s.getSubject().getCode() : "-",
                    (s != null && s.getSubject() != null) ? s.getSubject().getName() : "-",
                    (s != null && s.getStartTime() != null) ? s.getStartTime().format(dateFmt) : "-",
                    (s != null && s.getStartTime() != null) ? s.getStartTime().format(timeFmt) : "-",
                    r.getStatus().name(),
                    r.getMarkedAt() != null ? r.getMarkedAt().format(timeFmt) : "-",
                    r.getDistanceFromTeacher() != null ? String.format("%.1f m", r.getDistanceFromTeacher()) : "-"
            );
        }).collect(Collectors.toList());

        // Calculate subject-wise attendance breakdown
        Map<String, List<AttendanceSession>> sessionsBySubject = classSessions.stream()
                .filter(s -> s.getSubject() != null && s.getSubject().getId() != null)
                .collect(Collectors.groupingBy(s -> s.getSubject().getId()));

        Map<String, AttendanceRecord> recordsBySession = new HashMap<>();
        for (AttendanceRecord r : records) {
            if (r.getSession() != null && r.getSession().getId() != null) {
                recordsBySession.put(r.getSession().getId(), r);
            }
        }

        List<StudentProfileDto.SubjectAttendanceDto> subjectAttendanceList = new ArrayList<>();
        for (Map.Entry<String, List<AttendanceSession>> entry : sessionsBySubject.entrySet()) {
            List<AttendanceSession> subSessions = entry.getValue();
            if (subSessions.isEmpty()) continue;
            Subject subj = subSessions.get(0).getSubject();
            long subConducted = subSessions.size();
            long subPresent = subSessions.stream()
                    .map(s -> recordsBySession.get(s.getId()))
                    .filter(r -> r != null && r.getStatus() == AttendanceRecord.AttendanceStatus.PRESENT)
                    .count();
            long subAbsent = Math.max(0, subConducted - subPresent);
            double subPct = subConducted > 0 ? ((double) subPresent / subConducted) * 100.0 : 100.0;

            subjectAttendanceList.add(new StudentProfileDto.SubjectAttendanceDto(
                    subj.getId(),
                    subj.getCode(),
                    subj.getName(),
                    subConducted,
                    subPresent,
                    subAbsent,
                    Math.round(subPct * 100.0) / 100.0
            ));
        }

        StudentProfileDto profile = new StudentProfileDto();
        profile.setId(student.getId());
        profile.setUsn(student.getUsn());
        profile.setName(student.getName());
        profile.setEmail(student.getEmail());
        profile.setDepartment(student.getDepartment() != null ? student.getDepartment().getName() : "General");
        if (student.getCollegeClass() != null) {
            profile.setSemester(student.getCollegeClass().getSemester());
            profile.setSection(student.getCollegeClass().getSection());
        }
        profile.setTotalConducted(totalConducted);
        profile.setTotalPresent(totalPresent);
        profile.setTotalAbsent(totalAbsent);
        profile.setOverallPercentage(Math.round(overallPercentage * 100.0) / 100.0);
        profile.setAttendanceHistory(history);
        profile.setSubjectWiseAttendance(subjectAttendanceList);

        return profile;
    }

    public List<DateWiseAttendanceRecordDto> getDateWiseAttendance(String classId, String subjectId, LocalDate startDate, LocalDate endDate) {
        Teacher teacher = getAuthorizedTeacherOrNull();
        Set<String> assignedClassIds = (teacher != null && !teacher.isAdmin() && teacher.getAssignedClasses() != null && !teacher.getAssignedClasses().isEmpty())
                ? teacher.getAssignedClasses().stream().map(CollegeClass::getId).collect(Collectors.toSet())
                : null;

        if (assignedClassIds != null && classId != null && !classId.isBlank()) {
            if (!assignedClassIds.contains(classId)) {
                throw new UnauthorizedException("Access Denied: You are not assigned to this class.");
            }
        }

        List<AttendanceSession> sessions;
        if (classId != null && !classId.isBlank() && subjectId != null && !subjectId.isBlank()) {
            sessions = sessionRepository.findByCollegeClassIdAndSubjectIdOrderByStartTimeAsc(classId, subjectId);
        } else if (classId != null && !classId.isBlank()) {
            sessions = sessionRepository.findByCollegeClassIdOrderByStartTimeDesc(classId);
        } else if (assignedClassIds != null && !assignedClassIds.isEmpty()) {
            sessions = sessionRepository.findByCollegeClassIdIn(assignedClassIds);
            if (subjectId != null && !subjectId.isBlank()) {
                sessions = sessions.stream().filter(s -> s.getSubject() != null && subjectId.equals(s.getSubject().getId())).collect(Collectors.toList());
            }
        } else {
            sessions = sessionRepository.findAll();
            if (subjectId != null && !subjectId.isBlank()) {
                sessions = sessions.stream().filter(s -> s.getSubject() != null && subjectId.equals(s.getSubject().getId())).collect(Collectors.toList());
            }
        }

        LocalDateTime startDt = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDt = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        if (startDt != null || endDt != null) {
            sessions = sessions.stream().filter(s -> {
                if (s.getStartTime() == null) return false;
                if (startDt != null && s.getStartTime().isBefore(startDt)) return false;
                if (endDt != null && s.getStartTime().isAfter(endDt)) return false;
                return true;
            }).collect(Collectors.toList());
        }

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("hh:mm a");

        Map<String, List<Student>> classStudentsCache = new HashMap<>();
        List<DateWiseAttendanceRecordDto> results = new ArrayList<>();

        for (AttendanceSession s : sessions) {
            if (s.getCollegeClass() == null) continue;
            String cId = s.getCollegeClass().getId();
            List<Student> students = classStudentsCache.computeIfAbsent(cId, id -> studentRepository.findByCollegeClassId(id));

            List<AttendanceRecord> records = recordRepository.findBySessionIdOrderByMarkedAtDesc(s.getId());
            Map<String, AttendanceRecord> recordsByStudent = new HashMap<>();
            for (AttendanceRecord r : records) {
                if (r.getStudent() != null) {
                    recordsByStudent.put(r.getStudent().getId(), r);
                }
            }

            String sessionDateStr = s.getStartTime() != null ? s.getStartTime().format(dateFmt) : "-";
            String subCode = s.getSubject() != null ? s.getSubject().getCode() : "-";
            String subName = s.getSubject() != null ? s.getSubject().getName() : "-";
            String className = s.getCollegeClass().getDisplayName() != null ? s.getCollegeClass().getDisplayName() : "Class";
            String section = s.getCollegeClass().getSection() != null ? s.getCollegeClass().getSection() : "-";

            for (Student student : students) {
                AttendanceRecord r = recordsByStudent.get(student.getId());
                boolean isPresent = (r != null && r.getStatus() == AttendanceRecord.AttendanceStatus.PRESENT);

                results.add(new DateWiseAttendanceRecordDto(
                        sessionDateStr,
                        s.getId(),
                        s.getSessionCode(),
                        student.getId(),
                        student.getName(),
                        student.getUsn(),
                        className,
                        section,
                        subCode,
                        subName,
                        isPresent ? "PRESENT" : "ABSENT",
                        (r != null && r.getMarkedAt() != null) ? r.getMarkedAt().format(timeFmt) : "-",
                        (r != null && r.getDistanceFromTeacher() != null) ? String.format("%.1f m", r.getDistanceFromTeacher()) : "-"
                ));
            }
        }

        results.sort((a, b) -> {
            int dateCmp = b.getDate().compareTo(a.getDate());
            if (dateCmp != 0) return dateCmp;
            return a.getStudentName().compareToIgnoreCase(b.getStudentName());
        });

        return results;
    }

    public DashboardStatsDto getDashboardStats() {
        Teacher teacher = getAuthorizedTeacherOrNull();
        if (teacher == null) {
            return getGlobalDashboardStats();
        }

        Teacher freshTeacher = teacherRepository.findById(teacher.getId()).orElse(teacher);

        List<ClassDto> assignedClasses = teacherService.getClassesForTeacher(freshTeacher, null);
        List<SubjectDto> assignedSubjects = teacherService.getSubjectsForTeacher(freshTeacher, null, null);

        long assignedClassesCount = assignedClasses.size();
        long assignedSubjectsCount = assignedSubjects.size();

        Set<String> classIds = assignedClasses.stream().map(ClassDto::getId).collect(Collectors.toSet());

        long totalStudents = 0;
        if (!classIds.isEmpty()) {
            Set<String> uniqueStudentIds = new HashSet<>();
            for (String cId : classIds) {
                if (cId != null && !cId.isBlank()) {
                    List<Student> classStudents = studentRepository.findByCollegeClassId(cId);
                    if (classStudents != null) {
                        for (Student s : classStudents) {
                            if (s != null && s.getId() != null && uniqueStudentIds.add(s.getId())) {
                                totalStudents++;
                            }
                        }
                    }
                }
            }
        } else if (freshTeacher.isAdmin()) {
            totalStudents = studentRepository.count();
        } else {
            totalStudents = 0;
        }

        List<AttendanceSession> teacherSessions = sessionRepository.findByTeacher_IdOrderByCreatedAtDesc(freshTeacher.getId());
        if (teacherSessions.isEmpty() && freshTeacher.isAdmin()) {
            teacherSessions = sessionRepository.findAll();
        }

        long totalClassesConducted = teacherSessions.size();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        List<AttendanceSession> activeSessionsList = teacherSessions.stream()
                .filter(s -> s.getStatus() == AttendanceSession.SessionStatus.ACTIVE &&
                        (s.getEndTime() == null || s.getEndTime().isAfter(now)))
                .collect(Collectors.toList());
        long activeSessions = activeSessionsList.size();

        List<SessionDto> activeSessionDtos = activeSessionsList.stream()
                .map(sessionService::toDto)
                .collect(Collectors.toList());

        long todayAttendance = 0;
        for (AttendanceSession s : teacherSessions) {
            if (s.getStartTime() != null && !s.getStartTime().isBefore(startOfDay) && !s.getStartTime().isAfter(endOfDay)) {
                todayAttendance += recordRepository.countBySessionId(s.getId());
            }
        }

        double avgPercentage = 100.0;
        if (!teacherSessions.isEmpty()) {
            long totalPresent = 0;
            long totalExpected = 0;
            for (AttendanceSession s : teacherSessions) {
                if (s.getStatus() == AttendanceSession.SessionStatus.COMPLETED || s.getStatus() == AttendanceSession.SessionStatus.ACTIVE) {
                    long presentInSession = recordRepository.countBySessionIdAndStatus(s.getId(), AttendanceRecord.AttendanceStatus.PRESENT);
                    long classStudentCount = (s.getCollegeClass() != null) ? studentRepository.countByCollegeClassId(s.getCollegeClass().getId()) : 0;
                    totalPresent += presentInSession;
                    totalExpected += classStudentCount > 0 ? classStudentCount : Math.max(1, presentInSession);
                }
            }
            if (totalExpected > 0) {
                avgPercentage = Math.min(100.0, Math.max(0.0, ((double) totalPresent / totalExpected) * 100.0));
                avgPercentage = Math.round(avgPercentage * 10.0) / 10.0;
            }
        }

        DashboardStatsDto stats = new DashboardStatsDto(
                totalStudents,
                totalClassesConducted,
                avgPercentage,
                todayAttendance,
                activeSessions
        );
        stats.setRecentActiveSessions(activeSessionDtos);
        stats.setAssignedClasses(assignedClassesCount);
        stats.setAssignedSubjects(assignedSubjectsCount);
        stats.setClassesCount(assignedClassesCount);
        stats.setSubjectsCount(assignedSubjectsCount);

        LocalDateTime sevenDaysAgo = LocalDate.now().minusDays(6).atStartOfDay();
        Map<String, Long> weeklyCounts = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusDays(i);
            weeklyCounts.put(d.format(DateTimeFormatter.ofPattern("EEE")), 0L);
        }
        for (AttendanceSession s : teacherSessions) {
            if (s.getStartTime() != null && !s.getStartTime().isBefore(sevenDaysAgo) && !s.getStartTime().isAfter(endOfDay)) {
                String dayName = s.getStartTime().format(DateTimeFormatter.ofPattern("EEE"));
                long recCount = recordRepository.countBySessionId(s.getId());
                weeklyCounts.put(dayName, weeklyCounts.getOrDefault(dayName, 0L) + recCount);
            }
        }
        stats.setWeeklyAttendanceCounts(weeklyCounts);

        Map<String, Double> subjectTrends = new HashMap<>();
        Map<String, List<AttendanceSession>> bySubject = teacherSessions.stream()
                .filter(s -> s.getSubject() != null && s.getSubject().getName() != null)
                .collect(Collectors.groupingBy(s -> s.getSubject().getName()));

        for (Map.Entry<String, List<AttendanceSession>> entry : bySubject.entrySet()) {
            long totalRecs = 0;
            long totalEnroll = 0;
            for (AttendanceSession s : entry.getValue()) {
                totalRecs += recordRepository.countBySessionId(s.getId());
                if (s.getCollegeClass() != null) {
                    totalEnroll += studentRepository.countByCollegeClassId(s.getCollegeClass().getId());
                }
            }
            double pct = totalEnroll > 0 ? ((double) totalRecs / totalEnroll) * 100.0 : 100.0;
            subjectTrends.put(entry.getKey(), Math.round(pct * 10.0) / 10.0);
        }
        stats.setSubjectAttendanceTrends(subjectTrends);

        return stats;
    }

    private DashboardStatsDto getGlobalDashboardStats() {
        long totalStudents = studentRepository.count();
        long totalClassesConducted = sessionRepository.count();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        long todayAttendance = recordRepository.countRecordsToday(startOfDay, endOfDay);
        long activeSessions = sessionRepository.countActiveSessions(now);

        double avgPercentage = 85.0;
        if (totalStudents > 0 && totalClassesConducted > 0) {
            long totalPresentRecords = recordRepository.countByStatus(AttendanceRecord.AttendanceStatus.PRESENT);
            long estimatedTotalExpectedSlots = totalClassesConducted * Math.max(1, (totalStudents / Math.max(1, classRepository.count())));
            if (estimatedTotalExpectedSlots > 0 && totalPresentRecords > 0) {
                avgPercentage = Math.min(100.0, Math.max(0.0, ((double) totalPresentRecords / estimatedTotalExpectedSlots) * 100.0));
                avgPercentage = Math.round(avgPercentage * 10.0) / 10.0;
            }
        }

        DashboardStatsDto stats = new DashboardStatsDto(
                totalStudents,
                totalClassesConducted,
                avgPercentage,
                todayAttendance,
                activeSessions
        );

        List<SessionDto> activeSessionDtos = sessionService.getActiveSessions();
        stats.setRecentActiveSessions(activeSessionDtos);

        LocalDateTime sevenDaysAgo = LocalDate.now().minusDays(6).atStartOfDay();
        Map<String, Long> weeklyCounts = recordRepository.countRecordsByDay(sevenDaysAgo, endOfDay);
        stats.setWeeklyAttendanceCounts(weeklyCounts);

        Map<String, Double> subjectTrends = new HashMap<>();
        List<AttendanceSession> sessions = sessionRepository.findAll();
        if (!sessions.isEmpty()) {
            Map<String, List<AttendanceSession>> bySubject = sessions.stream()
                    .filter(s -> s.getSubject() != null && s.getSubject().getName() != null)
                    .collect(Collectors.groupingBy(s -> s.getSubject().getName()));

            Map<String, Long> classEnrollmentCache = new HashMap<>();

            for (Map.Entry<String, List<AttendanceSession>> entry : bySubject.entrySet()) {
                long totalRecs = 0;
                long totalEnroll = 0;
                for (AttendanceSession s : entry.getValue()) {
                    totalRecs += recordRepository.countBySessionId(s.getId());
                    if (s.getCollegeClass() != null) {
                        String classId = s.getCollegeClass().getId();
                        long count = classEnrollmentCache.computeIfAbsent(classId, id -> studentRepository.countByCollegeClassId(id));
                        totalEnroll += count;
                    }
                }
                double pct = totalEnroll > 0 ? ((double) totalRecs / totalEnroll) * 100.0 : 85.0;
                subjectTrends.put(entry.getKey(), Math.round(pct * 10.0) / 10.0);
            }
        }
        stats.setSubjectAttendanceTrends(subjectTrends);

        long cCount = classRepository.count();
        long sCount = subjectRepository.count();
        stats.setAssignedClasses(cCount);
        stats.setAssignedSubjects(sCount);
        stats.setClassesCount(cCount);
        stats.setSubjectsCount(sCount);

        return stats;
    }
}
