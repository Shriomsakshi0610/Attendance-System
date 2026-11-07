package com.renaissance.config;

import com.renaissance.entity.*;
import com.renaissance.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final TeacherRepository teacherRepository;
    private final DepartmentRepository departmentRepository;
    private final ClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TimetableScheduleRepository timetableRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${renaissance.admin.email:admin@renaissance.edu}")
    private String adminEmail;

    @Value("${renaissance.admin.password:Admin@1234}")
    private String adminPassword;

    @Value("${renaissance.admin.name:System Administrator}")
    private String adminName;

    @Value("${renaissance.admin.employee-id:ADM001}")
    private String adminEmpId;

    @Value("${renaissance.admin.reset-password-on-boot:false}")
    private boolean resetAdminPasswordOnBoot;

    @Value("${renaissance.teacher.email:teacher@renaissance.edu}")
    private String teacherEmail;

    @Value("${renaissance.teacher.password:Teacher@1234}")
    private String teacherPassword;

    public DataInitializer(TeacherRepository teacherRepository,
                           DepartmentRepository departmentRepository,
                           ClassRepository classRepository,
                           SubjectRepository subjectRepository,
                           StudentRepository studentRepository,
                           EnrollmentRepository enrollmentRepository,
                           TimetableScheduleRepository timetableRepository,
                           PasswordEncoder passwordEncoder) {
        this.teacherRepository = teacherRepository;
        this.departmentRepository = departmentRepository;
        this.classRepository = classRepository;
        this.subjectRepository = subjectRepository;
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.timetableRepository = timetableRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // 1. Seed Departments if missing
        Department cse = departmentRepository.findByCodeIgnoreCase("CSE")
                .orElseGet(() -> departmentRepository.save(new Department("CSE", "Computer Science & Engineering", "Department of CSE", "ACTIVE")));

        Department ise = departmentRepository.findByCodeIgnoreCase("ISE")
                .orElseGet(() -> departmentRepository.save(new Department("ISE", "Information Science & Engineering", "Department of ISE", "ACTIVE")));

        Department ece = departmentRepository.findByCodeIgnoreCase("ECE")
                .orElseGet(() -> departmentRepository.save(new Department("ECE", "Electronics & Communication Engineering", "Department of ECE", "ACTIVE")));

        Department mech = departmentRepository.findByCodeIgnoreCase("MECH")
                .orElseGet(() -> departmentRepository.save(new Department("MECH", "Mechanical Engineering", "Department of MECH", "ACTIVE")));

        // 2. Seed Dedicated Admin Account
        Teacher adminUser = teacherRepository.findByEmailIgnoreCase(adminEmail).orElse(null);
        if (adminUser == null) {
            adminUser = new Teacher(
                    adminName,
                    adminEmail,
                    passwordEncoder.encode(adminPassword),
                    adminEmpId,
                    cse,
                    "ROLE_ADMIN"
            );
            adminUser.setPhone("+91 9876543200");
            teacherRepository.save(adminUser);
        } else if (resetAdminPasswordOnBoot) {
            adminUser.setPassword(passwordEncoder.encode(adminPassword));
            teacherRepository.save(adminUser);
        }

        // 3. Seed Faculty/Teacher Account
        final Teacher assignedFaculty = teacherRepository.findByEmailIgnoreCase(teacherEmail)
                .orElseGet(() -> {
                    Teacher t = new Teacher(
                            "Prof. Charles Babbage",
                            teacherEmail,
                            passwordEncoder.encode(teacherPassword),
                            "EMP001",
                            cse,
                            "ROLE_TEACHER"
                    );
                    t.setPhone("+91 9876543210");
                    return teacherRepository.save(t);
                });

        // 4. Seed College Classes if missing
        CollegeClass cse5A = classRepository.findByDepartmentAndSemesterAndSection(cse, 5, "A")
                .orElseGet(() -> {
                    CollegeClass c = new CollegeClass(cse, 5, "A", "2024-2028");
                    c.setClassName("CSE 5th Semester");
                    c.setAcademicYear("2026-2027");
                    c.setClassTeacher(assignedFaculty);
                    return classRepository.save(c);
                });

        CollegeClass cse5B = classRepository.findByDepartmentAndSemesterAndSection(cse, 5, "B")
                .orElseGet(() -> {
                    CollegeClass c = new CollegeClass(cse, 5, "B", "2024-2028");
                    c.setClassName("CSE 5th Semester");
                    c.setAcademicYear("2026-2027");
                    return classRepository.save(c);
                });

        CollegeClass ise5A = classRepository.findByDepartmentAndSemesterAndSection(ise, 5, "A")
                .orElseGet(() -> {
                    CollegeClass c = new CollegeClass(ise, 5, "A", "2024-2028");
                    c.setClassName("ISE 5th Semester");
                    c.setAcademicYear("2026-2027");
                    return classRepository.save(c);
                });

        CollegeClass ece3A = classRepository.findByDepartmentAndSemesterAndSection(ece, 3, "A")
                .orElseGet(() -> {
                    CollegeClass c = new CollegeClass(ece, 3, "A", "2025-2029");
                    c.setClassName("ECE 3rd Semester");
                    c.setAcademicYear("2026-2027");
                    return classRepository.save(c);
                });

        // 5. Seed Subjects if missing
        Subject dbms = subjectRepository.findByCodeIgnoreCase("21CS51")
                .orElseGet(() -> {
                    Subject s = new Subject("21CS51", "Database Management Systems", cse, 5);
                    s.setCollegeClass(cse5A);
                    s.setAssignedTeacher(assignedFaculty);
                    s.setWeeklyHours(4);
                    return subjectRepository.save(s);
                });

        Subject cn = subjectRepository.findByCodeIgnoreCase("21CS52")
                .orElseGet(() -> {
                    Subject s = new Subject("21CS52", "Computer Networks", cse, 5);
                    s.setCollegeClass(cse5A);
                    s.setAssignedTeacher(assignedFaculty);
                    s.setWeeklyHours(4);
                    return subjectRepository.save(s);
                });

        Subject se = subjectRepository.findByCodeIgnoreCase("21CS53")
                .orElseGet(() -> {
                    Subject s = new Subject("21CS53", "Software Engineering", cse, 5);
                    s.setCollegeClass(cse5A);
                    s.setAssignedTeacher(assignedFaculty);
                    s.setWeeklyHours(3);
                    return subjectRepository.save(s);
                });

        Subject atc = subjectRepository.findByCodeIgnoreCase("21CS54")
                .orElseGet(() -> {
                    Subject s = new Subject("21CS54", "Automata Theory & Computability", cse, 5);
                    s.setCollegeClass(cse5A);
                    s.setWeeklyHours(4);
                    return subjectRepository.save(s);
                });

        // Link assigned classes and subjects to default faculty if not linked
        if (assignedFaculty != null) {
            boolean updated = false;
            if (assignedFaculty.getAssignedClasses().isEmpty()) {
                assignedFaculty.setAssignedClasses(List.of(cse5A, cse5B));
                updated = true;
            }
            if (assignedFaculty.getAssignedSubjects().isEmpty()) {
                assignedFaculty.setAssignedSubjects(List.of(dbms, cn, se));
                updated = true;
            }
            if (updated) {
                teacherRepository.save(assignedFaculty);
            }
        }

        // 6. Seed Students if empty
        if (studentRepository.count() == 0) {
            List<Student> sampleStudents = List.of(
                    new Student("1MS21CS001", "Aarav Sharma", "aarav.sharma@example.com", cse, cse5A),
                    new Student("1MS21CS002", "Ananya Rao", "ananya.rao@example.com", cse, cse5A),
                    new Student("1MS21CS003", "Bhavya Patel", "bhavya.patel@example.com", cse, cse5A),
                    new Student("1MS21CS004", "Chetan Kumar", "chetan.kumar@example.com", cse, cse5A),
                    new Student("1MS21CS005", "Divya Nair", "divya.nair@example.com", cse, cse5A),
                    new Student("1MS21CS006", "Eshan Gupta", "eshan.gupta@example.com", cse, cse5A),
                    new Student("1MS21CS050", "Farhan Akhtar", "farhan.akhtar@example.com", cse, cse5B),
                    new Student("1MS21CS051", "Gayatri Joshi", "gayatri.joshi@example.com", cse, cse5B),
                    new Student("1MS21IS001", "Hari Prasad", "hari.prasad@example.com", ise, ise5A),
                    new Student("1MS22EC001", "Ishaan Verma", "ishaan.verma@example.com", ece, ece3A)
            );
            List<Student> savedStudents = studentRepository.saveAll(sampleStudents);

            // Enroll CSE 5A students in DBMS and CN
            for (Student student : savedStudents) {
                if (student.getCollegeClass() != null && student.getCollegeClass().getId().equals(cse5A.getId())) {
                    enrollmentRepository.save(new Enrollment(student, cse5A, dbms, "2026-2027"));
                    enrollmentRepository.save(new Enrollment(student, cse5A, cn, "2026-2027"));
                    enrollmentRepository.save(new Enrollment(student, cse5A, se, "2026-2027"));
                }
            }
        }

        // 7. Seed Sample Timetable Schedules if empty
        if (timetableRepository.count() == 0 && assignedFaculty != null) {
            List<TimetableSchedule> sampleSchedules = new java.util.ArrayList<>(List.of(
                    new TimetableSchedule(cse, cse5A, dbms, assignedFaculty, "MONDAY", 1,
                            java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0), "LH-101"),
                    new TimetableSchedule(cse, cse5A, cn, assignedFaculty, "MONDAY", 2,
                            java.time.LocalTime.of(10, 0), java.time.LocalTime.of(11, 0), "LH-101"),
                    new TimetableSchedule(cse, cse5A, se, assignedFaculty, "TUESDAY", 3,
                            java.time.LocalTime.of(11, 15), java.time.LocalTime.of(12, 15), "LH-102"),
                    new TimetableSchedule(cse, cse5A, dbms, assignedFaculty, "WEDNESDAY", 5,
                            java.time.LocalTime.of(14, 0), java.time.LocalTime.of(15, 0), "LH-101"),
                    new TimetableSchedule(cse, cse5A, cn, assignedFaculty, "THURSDAY", 1,
                            java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0), "LH-101"),
                    new TimetableSchedule(cse, cse5A, se, assignedFaculty, "FRIDAY", 2,
                            java.time.LocalTime.of(10, 0), java.time.LocalTime.of(11, 0), "LH-102")
            ));

            // Seed second faculty (Dr. Ada Lovelace) for multi-faculty timetable verification (Requirement 6)
            Teacher secondFaculty = teacherRepository.findByEmailIgnoreCase("faculty2@renaissance.edu")
                    .orElseGet(() -> {
                        Teacher t = new Teacher(
                                "Dr. Ada Lovelace",
                                "faculty2@renaissance.edu",
                                passwordEncoder.encode("Teacher@1234"),
                                "EMP002",
                                ise,
                                "ROLE_TEACHER"
                        );
                        t.setPhone("+91 9876543222");
                        t.setAssignedClasses(List.of(ise5A));
                        t.setAssignedSubjects(List.of(atc));
                        return teacherRepository.save(t);
                    });

            if (secondFaculty != null) {
                sampleSchedules.add(new TimetableSchedule(ise, ise5A, atc, secondFaculty, "TUESDAY", 1,
                        java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0), "LH-201"));
                sampleSchedules.add(new TimetableSchedule(ise, ise5A, atc, secondFaculty, "THURSDAY", 3,
                        java.time.LocalTime.of(11, 0), java.time.LocalTime.of(12, 0), "LH-201"));
                sampleSchedules.add(new TimetableSchedule(ise, ise5A, atc, secondFaculty, "SATURDAY", 2,
                        java.time.LocalTime.of(10, 0), java.time.LocalTime.of(11, 0), "LH-202"));
            }

            timetableRepository.saveAll(sampleSchedules);
        }
    }
}
