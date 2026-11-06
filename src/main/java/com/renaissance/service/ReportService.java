package com.renaissance.service;

import com.renaissance.dto.AttendanceSummaryDto;
import com.renaissance.dto.DashboardStatsDto;
import com.renaissance.dto.SessionDto;
import com.renaissance.dto.StudentProfileDto;
import com.renaissance.entity.AttendanceRecord;
import com.renaissance.entity.AttendanceSession;
import com.renaissance.entity.CollegeClass;
import com.renaissance.entity.Student;
import com.renaissance.exception.ResourceNotFoundException;
import com.renaissance.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final SessionService sessionService;

    public ReportService(StudentRepository studentRepository,
                         AttendanceSessionRepository sessionRepository,
                         AttendanceRecordRepository recordRepository,
                         ClassRepository classRepository,
                         SessionService sessionService) {
        this.studentRepository = studentRepository;
        this.sessionRepository = sessionRepository;
        this.recordRepository = recordRepository;
        this.classRepository = classRepository;
        this.sessionService = sessionService;
    }

    @Transactional(readOnly = true)
    public List<AttendanceSummaryDto> getAttendanceSummary(Long classId, Long subjectId) {
        List<Student> students;
        if (classId != null) {
            students = studentRepository.findByCollegeClassId(classId);
        } else {
            students = studentRepository.findAll();
        }

        // Conducted sessions for this class and optional subject
        List<AttendanceSession> conductedSessions;
        if (classId != null && subjectId != null) {
            conductedSessions = sessionRepository.findByCollegeClassIdAndSubjectIdOrderByStartTimeAsc(classId, subjectId);
        } else if (classId != null) {
            conductedSessions = sessionRepository.findByCollegeClassIdOrderByStartTimeDesc(classId);
        } else {
            conductedSessions = sessionRepository.findAll();
        }

        long totalConducted = conductedSessions.stream()
                .filter(s -> s.getStatus() != AttendanceSession.SessionStatus.EXPIRED || recordRepository.countBySessionId(s.getId()) > 0)
                .count();

        List<AttendanceSummaryDto> summaries = new ArrayList<>();
        for (Student student : students) {
            long attended;
            if (subjectId != null) {
                attended = recordRepository.countPresentByStudentIdAndSubjectId(student.getId(), subjectId);
            } else {
                attended = recordRepository.countPresentByStudentId(student.getId());
            }

            long absent = Math.max(0, totalConducted - attended);
            double percentage = totalConducted > 0 ? ((double) attended / totalConducted) * 100.0 : 100.0;

            CollegeClass c = student.getCollegeClass();
            summaries.add(new AttendanceSummaryDto(
                    student.getId(),
                    student.getUsn(),
                    student.getName(),
                    student.getDepartment().getCode(),
                    c != null ? c.getSemester() : 0,
                    c != null ? c.getSection() : "-",
                    (subjectId != null && !conductedSessions.isEmpty()) ? conductedSessions.get(0).getSubject().getName() : "All Subjects",
                    totalConducted,
                    attended,
                    absent,
                    percentage
            ));
        }

        return summaries;
    }

    @Transactional(readOnly = true)
    public StudentProfileDto getStudentProfile(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + studentId));

        List<AttendanceRecord> records = recordRepository.findByStudentIdOrderByMarkedAtDesc(studentId);
        List<AttendanceSession> classSessions = sessionRepository.findByCollegeClassIdOrderByStartTimeDesc(student.getCollegeClass().getId());

        long totalConducted = classSessions.size();
        long totalPresent = records.stream().filter(r -> r.getStatus() == AttendanceRecord.AttendanceStatus.PRESENT).count();
        long totalAbsent = Math.max(0, totalConducted - totalPresent);
        double overallPercentage = totalConducted > 0 ? ((double) totalPresent / totalConducted) * 100.0 : 100.0;

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("hh:mm a");

        List<StudentProfileDto.HistoryRecordDto> history = records.stream().map(r -> {
            AttendanceSession s = r.getSession();
            return new StudentProfileDto.HistoryRecordDto(
                    s.getId(),
                    s.getSessionCode(),
                    s.getSubject().getCode(),
                    s.getSubject().getName(),
                    s.getStartTime().format(dateFmt),
                    s.getStartTime().format(timeFmt),
                    r.getStatus().name(),
                    r.getMarkedAt().format(timeFmt),
                    r.getDistanceFromTeacher() != null ? String.format("%.1f m", r.getDistanceFromTeacher()) : "-"
            );
        }).collect(Collectors.toList());

        StudentProfileDto profile = new StudentProfileDto();
        profile.setId(student.getId());
        profile.setUsn(student.getUsn());
        profile.setName(student.getName());
        profile.setEmail(student.getEmail());
        profile.setDepartment(student.getDepartment().getName());
        profile.setSemester(student.getCollegeClass().getSemester());
        profile.setSection(student.getCollegeClass().getSection());
        profile.setTotalConducted(totalConducted);
        profile.setTotalPresent(totalPresent);
        profile.setTotalAbsent(totalAbsent);
        profile.setOverallPercentage(Math.round(overallPercentage * 100.0) / 100.0);
        profile.setAttendanceHistory(history);

        return profile;
    }

    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats() {
        long totalStudents = studentRepository.count();
        long totalClassesConducted = sessionRepository.count();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        long todayAttendance = recordRepository.countRecordsToday(startOfDay, endOfDay);
        long activeSessions = sessionRepository.countActiveSessions(now);

        // Calculate Average Attendance Percentage across all students
        List<Student> allStudents = studentRepository.findAll();
        double avgPercentage = 85.0; // default baseline if no sessions yet
        if (!allStudents.isEmpty() && totalClassesConducted > 0) {
            double sumPercentage = 0.0;
            for (Student s : allStudents) {
                long attended = recordRepository.countPresentByStudentId(s.getId());
                long conducted = sessionRepository.findByCollegeClassIdOrderByStartTimeDesc(s.getCollegeClass().getId()).size();
                double pct = conducted > 0 ? ((double) attended / conducted) * 100.0 : 100.0;
                sumPercentage += pct;
            }
            avgPercentage = sumPercentage / allStudents.size();
        }

        DashboardStatsDto stats = new DashboardStatsDto(
                totalStudents,
                totalClassesConducted,
                avgPercentage,
                todayAttendance,
                activeSessions
        );

        // Fetch recent active sessions
        List<SessionDto> activeSessionDtos = sessionService.getActiveSessions();
        stats.setRecentActiveSessions(activeSessionDtos);

        // Weekly attendance counts (last 7 days)
        Map<String, Long> weeklyCounts = new LinkedHashMap<>();
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("EEE");
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            long count = recordRepository.countRecordsToday(date.atStartOfDay(), date.atTime(LocalTime.MAX));
            weeklyCounts.put(date.format(dayFormatter), count);
        }
        stats.setWeeklyAttendanceCounts(weeklyCounts);

        // Subject attendance trends
        Map<String, Double> subjectTrends = new HashMap<>();
        List<AttendanceSession> sessions = sessionRepository.findAll();
        Map<String, List<AttendanceSession>> bySubject = sessions.stream()
                .collect(Collectors.groupingBy(s -> s.getSubject().getName()));

        for (Map.Entry<String, List<AttendanceSession>> entry : bySubject.entrySet()) {
            long totalRecs = 0;
            long totalEnroll = 0;
            for (AttendanceSession s : entry.getValue()) {
                totalRecs += recordRepository.countBySessionId(s.getId());
                totalEnroll += studentRepository.findByCollegeClassId(s.getCollegeClass().getId()).size();
            }
            double pct = totalEnroll > 0 ? ((double) totalRecs / totalEnroll) * 100.0 : 80.0;
            subjectTrends.put(entry.getKey(), Math.round(pct * 10.0) / 10.0);
        }
        stats.setSubjectAttendanceTrends(subjectTrends);

        return stats;
    }
}
