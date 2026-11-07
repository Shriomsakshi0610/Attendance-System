package com.renaissance.service;

import com.renaissance.config.CacheConfig;
import com.renaissance.dto.*;
import com.renaissance.entity.*;
import com.renaissance.exception.ResourceNotFoundException;
import com.renaissance.repository.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final DepartmentRepository departmentRepository;
    private final ClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final TimetableScheduleRepository timetableRepository;
    private final StudentService studentService;
    private final SessionService sessionService;
    private final PasswordEncoder passwordEncoder;

    public AdminService(DepartmentRepository departmentRepository,
                        ClassRepository classRepository,
                        SubjectRepository subjectRepository,
                        TeacherRepository teacherRepository,
                        StudentRepository studentRepository,
                        EnrollmentRepository enrollmentRepository,
                        AttendanceSessionRepository sessionRepository,
                        AttendanceRecordRepository recordRepository,
                        TimetableScheduleRepository timetableRepository,
                        StudentService studentService,
                        SessionService sessionService,
                        PasswordEncoder passwordEncoder) {
        this.departmentRepository = departmentRepository;
        this.classRepository = classRepository;
        this.subjectRepository = subjectRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.sessionRepository = sessionRepository;
        this.recordRepository = recordRepository;
        this.timetableRepository = timetableRepository;
        this.studentService = studentService;
        this.sessionService = sessionService;
        this.passwordEncoder = passwordEncoder;
    }

    // ----------------------------------------------------
    // DASHBOARD OVERVIEW STATS
    // ----------------------------------------------------

    public AdminDashboardStatsDto getAdminDashboardStats() {
        AdminDashboardStatsDto stats = new AdminDashboardStatsDto();
        stats.setTotalDepartments(departmentRepository.count());
        stats.setTotalClasses(classRepository.count());
        stats.setTotalSubjects(subjectRepository.count());
        stats.setTotalStudents(studentRepository.count());
        stats.setTotalTeachers(teacherRepository.countByRole("ROLE_TEACHER"));

        List<SessionDto> activeSessions = sessionService.getActiveSessions();
        stats.setActiveSessions(activeSessions);
        stats.setActiveSessionsCount(activeSessions.size());

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(java.time.LocalTime.MAX);
        long todayCount = recordRepository.countRecordsToday(startOfDay, endOfDay);
        stats.setTodayAttendanceCount(todayCount);

        return stats;
    }

    // ----------------------------------------------------
    // DEPARTMENT MANAGEMENT
    // ----------------------------------------------------

    public List<DepartmentDto> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(d -> new DepartmentDto(d.getId(), d.getCode(), d.getName(), d.getDescription(), d.getStatus()))
                .collect(Collectors.toList());
    }

    public DepartmentDto getDepartmentById(String id) {
        Department d = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));
        return new DepartmentDto(d.getId(), d.getCode(), d.getName(), d.getDescription(), d.getStatus());
    }

    @CacheEvict(value = CacheConfig.CACHE_DEPARTMENTS, allEntries = true)
    public DepartmentDto createDepartment(CreateDepartmentRequest req) {
        String code = req.getCode().trim().toUpperCase();
        if (departmentRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("Department with code '" + code + "' already exists.");
        }
        Department dept = new Department(code, req.getName().trim(), req.getDescription());
        dept.setStatus("ACTIVE");
        Department saved = departmentRepository.save(dept);
        return new DepartmentDto(saved.getId(), saved.getCode(), saved.getName(), saved.getDescription(), saved.getStatus());
    }

    @CacheEvict(value = CacheConfig.CACHE_DEPARTMENTS, allEntries = true)
    public DepartmentDto updateDepartment(String id, CreateDepartmentRequest req) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));

        String code = req.getCode().trim().toUpperCase();
        if (!dept.getCode().equalsIgnoreCase(code) && departmentRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("Department with code '" + code + "' already exists.");
        }
        dept.setCode(code);
        dept.setName(req.getName().trim());
        dept.setDescription(req.getDescription());

        Department updated = departmentRepository.save(dept);
        return new DepartmentDto(updated.getId(), updated.getCode(), updated.getName(), updated.getDescription(), updated.getStatus());
    }

    @CacheEvict(value = CacheConfig.CACHE_DEPARTMENTS, allEntries = true)
    public DepartmentDto toggleDepartmentStatus(String id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));
        dept.setStatus("ACTIVE".equalsIgnoreCase(dept.getStatus()) ? "INACTIVE" : "ACTIVE");
        Department updated = departmentRepository.save(dept);
        return new DepartmentDto(updated.getId(), updated.getCode(), updated.getName(), updated.getDescription(), updated.getStatus());
    }

    @CacheEvict(value = CacheConfig.CACHE_DEPARTMENTS, allEntries = true)
    public void deleteDepartment(String id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));

        long classCount = classRepository.countByDepartment_Id(id);
        long subjectCount = subjectRepository.countByDepartment_Id(id);
        long teacherCount = teacherRepository.countByDepartment_Id(id);
        long studentCount = studentRepository.countByDepartment_Id(id);

        if (classCount > 0 || subjectCount > 0 || teacherCount > 0 || studentCount > 0) {
            throw new IllegalStateException("Cannot delete Department '" + dept.getCode() +
                    "'. It is currently associated with " + classCount + " class(es), " +
                    subjectCount + " subject(s), " + teacherCount + " teacher(s), and " +
                    studentCount + " student(s). Please reassign or deactivate instead.");
        }
        departmentRepository.deleteById(id);
    }

    // ----------------------------------------------------
    // CLASS & SECTION MANAGEMENT
    // ----------------------------------------------------

    public List<ClassDto> getAllClasses(String departmentId) {
        List<CollegeClass> classes = (departmentId != null && !departmentId.isBlank()) ?
                classRepository.findByDepartment_Id(departmentId) :
                classRepository.findAll();

        return classes.stream().map(this::toClassDto).collect(Collectors.toList());
    }

    public ClassDto getClassById(String id) {
        CollegeClass c = classRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with ID: " + id));
        return toClassDto(c);
    }

    @CacheEvict(value = {CacheConfig.CACHE_CLASSES, CacheConfig.CACHE_DEPARTMENTS}, allEntries = true)
    public ClassDto createClass(CreateClassRequest req) {
        Department dept = departmentRepository.findById(req.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + req.getDepartmentId()));

        String sec = req.getSection().trim().toUpperCase();
        if (classRepository.existsByDepartment_IdAndSemesterAndSectionIgnoreCase(dept.getId(), req.getSemester(), sec)) {
            throw new IllegalArgumentException("Class for " + dept.getCode() + " Semester " +
                    req.getSemester() + " Section " + sec + " already exists.");
        }

        Teacher classTeacher = null;
        if (req.getClassTeacherId() != null && !req.getClassTeacherId().isBlank()) {
            classTeacher = teacherRepository.findById(req.getClassTeacherId()).orElse(null);
        }

        CollegeClass c = new CollegeClass();
        c.setDepartment(dept);
        c.setSemester(req.getSemester());
        c.setSection(sec);
        c.setAcademicYear(req.getAcademicYear() != null ? req.getAcademicYear().trim() : "2026-2027");
        c.setBatchYear(c.getAcademicYear());
        c.setClassName(req.getClassName() != null && !req.getClassName().isBlank() ?
                req.getClassName().trim() : (dept.getCode() + " Sem " + req.getSemester()));
        c.setClassTeacher(classTeacher);
        c.setStatus("ACTIVE");

        CollegeClass saved = classRepository.save(c);
        return toClassDto(saved);
    }

    @CacheEvict(value = {CacheConfig.CACHE_CLASSES, CacheConfig.CACHE_DEPARTMENTS}, allEntries = true)
    public ClassDto updateClass(String id, CreateClassRequest req) {
        CollegeClass c = classRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with ID: " + id));

        Department dept = departmentRepository.findById(req.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + req.getDepartmentId()));

        String sec = req.getSection().trim().toUpperCase();
        c.setDepartment(dept);
        c.setSemester(req.getSemester());
        c.setSection(sec);
        if (req.getAcademicYear() != null && !req.getAcademicYear().isBlank()) {
            c.setAcademicYear(req.getAcademicYear().trim());
            c.setBatchYear(c.getAcademicYear());
        }
        if (req.getClassName() != null && !req.getClassName().isBlank()) {
            c.setClassName(req.getClassName().trim());
        }
        if (req.getClassTeacherId() != null && !req.getClassTeacherId().isBlank()) {
            Teacher ct = teacherRepository.findById(req.getClassTeacherId()).orElse(null);
            c.setClassTeacher(ct);
        } else {
            c.setClassTeacher(null);
        }

        CollegeClass updated = classRepository.save(c);
        return toClassDto(updated);
    }

    @CacheEvict(value = {CacheConfig.CACHE_CLASSES, CacheConfig.CACHE_DEPARTMENTS}, allEntries = true)
    public ClassDto toggleClassStatus(String id) {
        CollegeClass c = classRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with ID: " + id));
        c.setStatus("ACTIVE".equalsIgnoreCase(c.getStatus()) ? "INACTIVE" : "ACTIVE");
        CollegeClass updated = classRepository.save(c);
        return toClassDto(updated);
    }

    @CacheEvict(value = {CacheConfig.CACHE_CLASSES, CacheConfig.CACHE_DEPARTMENTS}, allEntries = true)
    public void deleteClass(String id) {
        CollegeClass c = classRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with ID: " + id));

        long studentCount = studentRepository.countByCollegeClass_Id(id);
        long subjectCount = subjectRepository.countByCollegeClass_Id(id);
        long scheduleCount = timetableRepository.countByCollegeClass_Id(id);
        long sessionCount = sessionRepository.countByCollegeClass_Id(id);

        if (studentCount > 0 || subjectCount > 0 || scheduleCount > 0 || sessionCount > 0) {
            throw new IllegalStateException("Cannot delete class '" + c.getDisplayName() +
                    "'. It has " + studentCount + " enrolled student(s), " +
                    subjectCount + " mapped subject(s), " + scheduleCount + " timetable schedule(s), and " +
                    sessionCount + " attendance session(s). Please deactivate instead.");
        }
        classRepository.deleteById(id);
    }

    public List<StudentDto> getClassStudents(String classId) {
        return studentService.getAllStudents(null, classId, null);
    }

    private ClassDto toClassDto(CollegeClass c) {
        if (c == null) return null;
        ClassDto dto = new ClassDto(
                c.getId(),
                c.getDepartment() != null ? c.getDepartment().getId() : null,
                c.getDepartment() != null ? c.getDepartment().getCode() : null,
                c.getDepartment() != null ? c.getDepartment().getName() : null,
                c.getSemester(),
                c.getSection(),
                c.getBatchYear()
        );
        dto.setAcademicYear(c.getAcademicYear());
        dto.setClassName(c.getClassName());
        dto.setStatus(c.getStatus());
        dto.setDisplayName(c.getDisplayName());
        if (c.getClassTeacher() != null) {
            dto.setClassTeacherId(c.getClassTeacher().getId());
            dto.setClassTeacherName(c.getClassTeacher().getName());
        }
        return dto;
    }

    // ----------------------------------------------------
    // SUBJECT MANAGEMENT
    // ----------------------------------------------------

    public List<SubjectDto> getAllSubjects(String departmentId, Integer semester) {
        List<Subject> subjects;
        if (departmentId != null && !departmentId.isBlank() && semester != null) {
            subjects = subjectRepository.findByDepartment_IdAndSemester(departmentId, semester);
        } else if (departmentId != null && !departmentId.isBlank()) {
            subjects = subjectRepository.findByDepartment_Id(departmentId);
        } else {
            subjects = subjectRepository.findAll();
        }
        return subjects.stream().map(this::toSubjectDto).collect(Collectors.toList());
    }

    public SubjectDto getSubjectById(String id) {
        Subject s = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + id));
        return toSubjectDto(s);
    }

    @CacheEvict(value = CacheConfig.CACHE_SUBJECTS, allEntries = true)
    public SubjectDto createSubject(CreateSubjectRequest req) {
        String code = req.getCode().trim().toUpperCase();
        if (subjectRepository.findByCodeIgnoreCase(code).isPresent()) {
            throw new IllegalArgumentException("Subject with code '" + code + "' already exists.");
        }

        Department dept = departmentRepository.findById(req.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + req.getDepartmentId()));

        CollegeClass collegeClass = null;
        if (req.getClassId() != null && !req.getClassId().isBlank()) {
            collegeClass = classRepository.findById(req.getClassId()).orElse(null);
        }

        Teacher assignedTeacher = null;
        if (req.getAssignedTeacherId() != null && !req.getAssignedTeacherId().isBlank()) {
            assignedTeacher = teacherRepository.findById(req.getAssignedTeacherId()).orElse(null);
        }

        Subject s = new Subject(code, req.getName().trim(), dept, req.getSemester());
        s.setCollegeClass(collegeClass);
        s.setAssignedTeacher(assignedTeacher);
        s.setWeeklyHours(req.getWeeklyHours() != null ? req.getWeeklyHours() : 4);
        s.setStatus("ACTIVE");

        Subject saved = subjectRepository.save(s);
        return toSubjectDto(saved);
    }

    @CacheEvict(value = CacheConfig.CACHE_SUBJECTS, allEntries = true)
    public SubjectDto updateSubject(String id, CreateSubjectRequest req) {
        Subject s = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + id));

        String code = req.getCode().trim().toUpperCase();
        if (!s.getCode().equalsIgnoreCase(code) && subjectRepository.findByCodeIgnoreCase(code).isPresent()) {
            throw new IllegalArgumentException("Subject with code '" + code + "' already exists.");
        }

        Department dept = departmentRepository.findById(req.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + req.getDepartmentId()));

        s.setCode(code);
        s.setName(req.getName().trim());
        s.setDepartment(dept);
        s.setSemester(req.getSemester());
        s.setWeeklyHours(req.getWeeklyHours() != null ? req.getWeeklyHours() : 4);

        if (req.getClassId() != null && !req.getClassId().isBlank()) {
            s.setCollegeClass(classRepository.findById(req.getClassId()).orElse(null));
        } else {
            s.setCollegeClass(null);
        }

        if (req.getAssignedTeacherId() != null && !req.getAssignedTeacherId().isBlank()) {
            s.setAssignedTeacher(teacherRepository.findById(req.getAssignedTeacherId()).orElse(null));
        } else {
            s.setAssignedTeacher(null);
        }

        Subject updated = subjectRepository.save(s);
        return toSubjectDto(updated);
    }

    @CacheEvict(value = CacheConfig.CACHE_SUBJECTS, allEntries = true)
    public SubjectDto toggleSubjectStatus(String id) {
        Subject s = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + id));
        s.setStatus("ACTIVE".equalsIgnoreCase(s.getStatus()) ? "INACTIVE" : "ACTIVE");
        Subject updated = subjectRepository.save(s);
        return toSubjectDto(updated);
    }

    @CacheEvict(value = CacheConfig.CACHE_SUBJECTS, allEntries = true)
    public void deleteSubject(String id) {
        Subject s = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + id));

        long scheduleCount = timetableRepository.countBySubject_Id(id);
        long sessionCount = sessionRepository.countBySubject_Id(id);
        long enrollmentCount = enrollmentRepository.countBySubject_Id(id);

        if (scheduleCount > 0 || sessionCount > 0 || enrollmentCount > 0) {
            throw new IllegalStateException("Cannot delete subject '" + s.getCode() + " - " + s.getName() +
                    "'. It is referenced by " + scheduleCount + " timetable schedule(s), " +
                    sessionCount + " attendance session(s), and " + enrollmentCount +
                    " student enrollment(s). Please mark as INACTIVE instead.");
        }
        subjectRepository.deleteById(id);
    }

    private SubjectDto toSubjectDto(Subject s) {
        if (s == null) return null;
        SubjectDto dto = new SubjectDto(
                s.getId(),
                s.getCode(),
                s.getName(),
                s.getDepartment() != null ? s.getDepartment().getId() : null,
                s.getDepartment() != null ? s.getDepartment().getCode() : null,
                s.getSemester()
        );
        if (s.getDepartment() != null) {
            dto.setDepartmentName(s.getDepartment().getName());
        }
        if (s.getCollegeClass() != null) {
            dto.setClassId(s.getCollegeClass().getId());
            dto.setClassName(s.getCollegeClass().getDisplayName());
        }
        if (s.getAssignedTeacher() != null) {
            dto.setAssignedTeacherId(s.getAssignedTeacher().getId());
            dto.setAssignedTeacherName(s.getAssignedTeacher().getName());
        }
        dto.setWeeklyHours(s.getWeeklyHours());
        dto.setStatus(s.getStatus());
        return dto;
    }

    // ----------------------------------------------------
    // FACULTY / TEACHER MANAGEMENT
    // ----------------------------------------------------

    public List<TeacherDto> getAllTeachers() {
        return teacherRepository.findAll().stream()
                .filter(t -> !"ROLE_ADMIN".equalsIgnoreCase(t.getRole()))
                .map(this::toTeacherDto)
                .collect(Collectors.toList());
    }

    public TeacherDto getTeacherById(String id) {
        Teacher t = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with ID: " + id));
        return toTeacherDto(t);
    }

    public TeacherDto createTeacher(CreateTeacherRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        if (teacherRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Teacher with email '" + email + "' already exists.");
        }

        Department dept = departmentRepository.findById(req.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + req.getDepartmentId()));

        String rawPassword = req.getInitialPassword() != null && !req.getInitialPassword().isBlank() ?
                req.getInitialPassword().trim() : "Teacher@123";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        Teacher t = new Teacher(req.getName().trim(), email, encodedPassword, req.getEmployeeId().trim(), dept, "ROLE_TEACHER");
        t.setPhone(req.getPhone() != null ? req.getPhone().trim() : null);

        if (req.getAssignedClassIds() != null && !req.getAssignedClassIds().isEmpty()) {
            List<CollegeClass> classes = classRepository.findAllById(req.getAssignedClassIds());
            t.setAssignedClasses(classes);
        }

        if (req.getAssignedSubjectIds() != null && !req.getAssignedSubjectIds().isEmpty()) {
            List<Subject> subjects = subjectRepository.findAllById(req.getAssignedSubjectIds());
            t.setAssignedSubjects(subjects);
        }

        Teacher saved = teacherRepository.save(t);
        return toTeacherDto(saved);
    }

    public TeacherDto updateTeacher(String id, CreateTeacherRequest req) {
        Teacher t = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with ID: " + id));

        String email = req.getEmail().trim().toLowerCase();
        if (!t.getEmail().equalsIgnoreCase(email) && teacherRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Another teacher with email '" + email + "' already exists.");
        }

        Department dept = departmentRepository.findById(req.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + req.getDepartmentId()));

        t.setName(req.getName().trim());
        t.setEmail(email);
        t.setEmployeeId(req.getEmployeeId().trim());
        t.setDepartment(dept);
        t.setPhone(req.getPhone() != null ? req.getPhone().trim() : null);

        if (req.getAssignedClassIds() != null) {
            List<CollegeClass> classes = classRepository.findAllById(req.getAssignedClassIds());
            t.setAssignedClasses(classes);
        }

        if (req.getAssignedSubjectIds() != null) {
            List<Subject> subjects = subjectRepository.findAllById(req.getAssignedSubjectIds());
            t.setAssignedSubjects(subjects);
        }

        Teacher updated = teacherRepository.save(t);
        return toTeacherDto(updated);
    }

    public TeacherDto toggleTeacherStatus(String id) {
        Teacher t = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with ID: " + id));
        t.setActive(!t.isActive());
        Teacher updated = teacherRepository.save(t);
        return toTeacherDto(updated);
    }

    public void resetTeacherPassword(String id, String newPassword) {
        Teacher t = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with ID: " + id));

        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }
        t.setPassword(passwordEncoder.encode(newPassword.trim()));
        teacherRepository.save(t);
    }

    public void deleteTeacher(String id) {
        Teacher t = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with ID: " + id));

        long sessionCount = sessionRepository.countByTeacher_Id(id);
        long scheduleCount = timetableRepository.countByTeacher_Id(id);
        long classTeacherCount = classRepository.countByClassTeacher_Id(id);

        if (sessionCount > 0 || scheduleCount > 0 || classTeacherCount > 0) {
            throw new IllegalStateException("Cannot delete teacher '" + t.getName() +
                    "'. Teacher is associated with " + sessionCount + " attendance session(s), " +
                    scheduleCount + " timetable schedule(s), and " + classTeacherCount +
                    " class mentorship(s). Please deactivate the teacher instead.");
        }
        teacherRepository.deleteById(id);
    }

    private TeacherDto toTeacherDto(Teacher t) {
        if (t == null) return null;
        TeacherDto dto = new TeacherDto(
                t.getId(),
                t.getName(),
                t.getEmail(),
                t.getEmployeeId(),
                t.getDepartment() != null ? t.getDepartment().getName() : "General",
                t.getDepartment() != null ? t.getDepartment().getCode() : "GEN",
                t.getRole()
        );
        if (t.getDepartment() != null) {
            dto.setDepartmentId(t.getDepartment().getId());
        }
        dto.setPhone(t.getPhone());
        dto.setStatus(t.getStatus());

        if (t.getAssignedClasses() != null) {
            dto.setAssignedClassIds(t.getAssignedClasses().stream().map(CollegeClass::getId).collect(Collectors.toList()));
            dto.setAssignedClassNames(t.getAssignedClasses().stream().map(CollegeClass::getDisplayName).collect(Collectors.toList()));
        }
        if (t.getAssignedSubjects() != null) {
            dto.setAssignedSubjectIds(t.getAssignedSubjects().stream().map(Subject::getId).collect(Collectors.toList()));
            dto.setAssignedSubjectNames(t.getAssignedSubjects().stream().map(Subject::getName).collect(Collectors.toList()));
        }
        return dto;
    }

    // ----------------------------------------------------
    // STUDENT MANAGEMENT
    // ----------------------------------------------------

    public List<StudentDto> getAllStudents(String departmentId, String classId, String keyword) {
        return studentService.getAllStudents(departmentId, classId, keyword);
    }

    public StudentDto getStudentById(String id) {
        return studentService.getStudentById(id);
    }

    public StudentDto createStudent(StudentDto dto) {
        return studentService.createStudent(dto);
    }

    public StudentDto updateStudent(String id, StudentDto dto) {
        return studentService.updateStudent(id, dto);
    }

    public StudentDto toggleStudentStatus(String id) {
        Student s = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + id));
        s.setStatus("ACTIVE".equalsIgnoreCase(s.getStatus()) ? "INACTIVE" : "ACTIVE");
        Student updated = studentRepository.save(s);
        return studentService.toDto(updated);
    }

    public void deleteStudent(String id) {
        Student s = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + id));

        long recordCount = recordRepository.countByStudent_Id(id);
        if (recordCount > 0) {
            throw new IllegalStateException("Cannot delete student '" + s.getUsn() + " - " + s.getName() +
                    "' because they have " + recordCount + " attendance record(s) on file. " +
                    "Please set the student status to INACTIVE instead to preserve academic integrity.");
        }
        studentService.deleteStudent(id);
    }

    public Map<String, Object> importStudentsFromCsv(MultipartFile file) {
        return studentService.importStudentsFromCsv(file);
    }

    // ----------------------------------------------------
    // ACTIVE SESSIONS & CAMPUS-WIDE MONITORING
    // ----------------------------------------------------

    public List<SessionDto> getActiveSessions() {
        return sessionService.getActiveSessions();
    }

    public SessionDto terminateSession(String sessionId) {
        return sessionService.endSession(sessionId);
    }
}
