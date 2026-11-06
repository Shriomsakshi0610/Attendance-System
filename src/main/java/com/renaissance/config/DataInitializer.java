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
    private final PasswordEncoder passwordEncoder;

    @Value("${renaissance.admin.email:teacher@renaissance.edu}")
    private String adminEmail;

    @Value("${renaissance.admin.password:Admin@1234}")
    private String adminPassword;

    @Value("${renaissance.admin.name:Prof. Charles Babbage}")
    private String adminName;

    @Value("${renaissance.admin.employee-id:EMP001}")
    private String adminEmpId;

    public DataInitializer(TeacherRepository teacherRepository,
                           DepartmentRepository departmentRepository,
                           ClassRepository classRepository,
                           SubjectRepository subjectRepository,
                           StudentRepository studentRepository,
                           EnrollmentRepository enrollmentRepository,
                           PasswordEncoder passwordEncoder) {
        this.teacherRepository = teacherRepository;
        this.departmentRepository = departmentRepository;
        this.classRepository = classRepository;
        this.subjectRepository = subjectRepository;
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // 1. Seed Departments if missing
        Department cse = departmentRepository.findByCodeIgnoreCase("CSE")
                .orElseGet(() -> departmentRepository.save(new Department("CSE", "Computer Science & Engineering")));

        Department ise = departmentRepository.findByCodeIgnoreCase("ISE")
                .orElseGet(() -> departmentRepository.save(new Department("ISE", "Information Science & Engineering")));

        Department ece = departmentRepository.findByCodeIgnoreCase("ECE")
                .orElseGet(() -> departmentRepository.save(new Department("ECE", "Electronics & Communication Engineering")));

        Department mech = departmentRepository.findByCodeIgnoreCase("MECH")
                .orElseGet(() -> departmentRepository.save(new Department("MECH", "Mechanical Engineering")));

        // 2. Provision Admin Teacher Account if none exists
        if (!teacherRepository.existsByEmailIgnoreCase(adminEmail)) {
            Teacher adminTeacher = new Teacher(
                    adminName,
                    adminEmail,
                    passwordEncoder.encode(adminPassword),
                    adminEmpId,
                    cse
            );
            adminTeacher.setRole("ROLE_TEACHER");
            teacherRepository.save(adminTeacher);
        }

        // 3. Seed College Classes if missing
        CollegeClass cse5A = classRepository.findByDepartmentAndSemesterAndSection(cse, 5, "A")
                .orElseGet(() -> classRepository.save(new CollegeClass(cse, 5, "A", "2024-2028")));

        CollegeClass cse5B = classRepository.findByDepartmentAndSemesterAndSection(cse, 5, "B")
                .orElseGet(() -> classRepository.save(new CollegeClass(cse, 5, "B", "2024-2028")));

        CollegeClass ise5A = classRepository.findByDepartmentAndSemesterAndSection(ise, 5, "A")
                .orElseGet(() -> classRepository.save(new CollegeClass(ise, 5, "A", "2024-2028")));

        CollegeClass ece3A = classRepository.findByDepartmentAndSemesterAndSection(ece, 3, "A")
                .orElseGet(() -> classRepository.save(new CollegeClass(ece, 3, "A", "2025-2029")));

        // 4. Seed Subjects if missing
        Subject dbms = subjectRepository.findByCodeIgnoreCase("21CS51")
                .orElseGet(() -> subjectRepository.save(new Subject("21CS51", "Database Management Systems", cse, 5)));

        Subject cn = subjectRepository.findByCodeIgnoreCase("21CS52")
                .orElseGet(() -> subjectRepository.save(new Subject("21CS52", "Computer Networks", cse, 5)));

        Subject se = subjectRepository.findByCodeIgnoreCase("21CS53")
                .orElseGet(() -> subjectRepository.save(new Subject("21CS53", "Software Engineering", cse, 5)));

        Subject atc = subjectRepository.findByCodeIgnoreCase("21CS54")
                .orElseGet(() -> subjectRepository.save(new Subject("21CS54", "Automata Theory & Computability", cse, 5)));

        // 5. Seed Students if empty
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
    }
}
