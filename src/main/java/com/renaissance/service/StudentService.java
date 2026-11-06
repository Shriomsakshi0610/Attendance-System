package com.renaissance.service;

import com.renaissance.dto.StudentDto;
import com.renaissance.entity.CollegeClass;
import com.renaissance.entity.Department;
import com.renaissance.entity.Student;
import com.renaissance.exception.ResourceNotFoundException;
import com.renaissance.repository.ClassRepository;
import com.renaissance.repository.DepartmentRepository;
import com.renaissance.repository.StudentRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;
    private final ClassRepository classRepository;

    public StudentService(StudentRepository studentRepository,
                          DepartmentRepository departmentRepository,
                          ClassRepository classRepository) {
        this.studentRepository = studentRepository;
        this.departmentRepository = departmentRepository;
        this.classRepository = classRepository;
    }

    public List<StudentDto> getAllStudents(String departmentId, String classId, String keyword) {
        List<Student> students = studentRepository.searchStudents(departmentId, classId,
                (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null);
        return students.stream().map(this::toDto).collect(Collectors.toList());
    }

    public StudentDto getStudentById(String id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + id));
        return toDto(student);
    }

    public StudentDto getStudentByUsn(String usn) {
        Student student = studentRepository.findByUsnIgnoreCase(usn.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with USN: " + usn));
        return toDto(student);
    }

    public StudentDto createStudent(StudentDto dto) {
        String cleanUsn = dto.getUsn().trim().toUpperCase();
        if (studentRepository.existsByUsnIgnoreCase(cleanUsn)) {
            throw new IllegalArgumentException("Student with USN '" + cleanUsn + "' already exists.");
        }

        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + dto.getDepartmentId()));

        CollegeClass collegeClass = classRepository.findById(dto.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with ID: " + dto.getClassId()));

        Student student = new Student(cleanUsn, dto.getName(), dto.getEmail(), department, collegeClass);
        if (dto.getStatus() != null) {
            student.setStatus(dto.getStatus());
        }

        Student saved = studentRepository.save(student);
        return toDto(saved);
    }

    public StudentDto updateStudent(String id, StudentDto dto) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + id));

        String cleanUsn = dto.getUsn().trim().toUpperCase();
        if (!student.getUsn().equalsIgnoreCase(cleanUsn) && studentRepository.existsByUsnIgnoreCase(cleanUsn)) {
            throw new IllegalArgumentException("Another student with USN '" + cleanUsn + "' already exists.");
        }

        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + dto.getDepartmentId()));

        CollegeClass collegeClass = classRepository.findById(dto.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with ID: " + dto.getClassId()));

        student.setUsn(cleanUsn);
        student.setName(dto.getName().trim());
        student.setEmail(dto.getEmail() != null ? dto.getEmail().trim() : null);
        student.setDepartment(department);
        student.setCollegeClass(collegeClass);
        if (dto.getStatus() != null) {
            student.setStatus(dto.getStatus());
        }

        Student updated = studentRepository.save(student);
        return toDto(updated);
    }

    public void deleteStudent(String id) {
        if (!studentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Student not found with ID: " + id);
        }
        studentRepository.deleteById(id);
    }

    public Map<String, Object> importStudentsFromCsv(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded CSV file is empty");
        }

        int importedCount = 0;
        int skippedCount = 0;
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build())) {

            for (CSVRecord record : csvParser) {
                try {
                    String usn = record.get("USN").trim().toUpperCase();
                    String name = record.get("Name").trim();
                    String email = record.isMapped("Email") ? record.get("Email").trim() : null;
                    String deptCode = record.get("Department").trim().toUpperCase();
                    int semester = Integer.parseInt(record.get("Semester").trim());
                    String section = record.get("Section").trim().toUpperCase();

                    if (studentRepository.existsByUsnIgnoreCase(usn)) {
                        skippedCount++;
                        errors.add("Row " + record.getRecordNumber() + ": USN " + usn + " already exists. Skipped.");
                        continue;
                    }

                    Department department = departmentRepository.findByCodeIgnoreCase(deptCode)
                            .orElseGet(() -> departmentRepository.save(new Department(deptCode, deptCode + " Department")));

                    CollegeClass collegeClass = classRepository.findByDepartmentAndSemesterAndSection(department, semester, section)
                            .orElseGet(() -> classRepository.save(new CollegeClass(department, semester, section, "2024-2028")));

                    Student student = new Student(usn, name, email, department, collegeClass);
                    studentRepository.save(student);
                    importedCount++;

                } catch (Exception ex) {
                    skippedCount++;
                    errors.add("Row " + record.getRecordNumber() + " error: " + ex.getMessage());
                }
            }

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse CSV file: " + e.getMessage(), e);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("importedCount", importedCount);
        result.put("skippedCount", skippedCount);
        result.put("errors", errors);
        return result;
    }

    public StudentDto toDto(Student student) {
        if (student == null) return null;
        CollegeClass c = student.getCollegeClass();
        Department d = student.getDepartment();

        return new StudentDto(
                student.getId(),
                student.getUsn(),
                student.getName(),
                student.getEmail(),
                d != null ? d.getId() : null,
                d != null ? d.getCode() : null,
                d != null ? d.getName() : null,
                c != null ? c.getId() : null,
                c != null ? c.getDisplayName() : null,
                c != null ? c.getSemester() : null,
                c != null ? c.getSection() : null,
                student.getStatus()
        );
    }
}
