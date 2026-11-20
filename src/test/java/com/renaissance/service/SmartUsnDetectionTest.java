package com.renaissance.service;

import com.renaissance.dto.StudentDetectionResponse;
import com.renaissance.entity.CollegeClass;
import com.renaissance.entity.Department;
import com.renaissance.entity.Student;
import com.renaissance.repository.ClassRepository;
import com.renaissance.repository.DepartmentRepository;
import com.renaissance.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmartUsnDetectionTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private ClassRepository classRepository;

    private StudentService studentService;

    private CollegeClass testClass;
    private Department testDept;
    private Student student1;
    private Student student2;
    private Student inactiveStudent;

    @BeforeEach
    void setUp() {
        studentService = new StudentService(studentRepository, departmentRepository, classRepository);

        testDept = new Department("CSE", "Computer Science", "CSE Department");
        testDept.setId("dept-cse");

        testClass = new CollegeClass(testDept, 5, "A", "2022-2026");
        testClass.setId("class-5a");

        student1 = new Student("2GI24IS042", "Aarav Sharma", "aarav@renaissance.edu", testDept, testClass);
        student1.setId("stud-042");
        student1.setStatus("ACTIVE");

        student2 = new Student("2GI24CS042", "Ananya Rao", "ananya@renaissance.edu", testDept, testClass);
        student2.setId("stud-042b");
        student2.setStatus("ACTIVE");

        inactiveStudent = new Student("2GI24IS099", "Inactive Student", "inactive@renaissance.edu", testDept, testClass);
        inactiveStudent.setId("stud-099");
        inactiveStudent.setStatus("INACTIVE");
    }

    @Test
    @DisplayName("Single student match returns exact resolved USN and full details")
    void testSingleMatchReturnsResolvedUsn() {
        when(classRepository.findById("class-5a")).thenReturn(Optional.of(testClass));
        when(studentRepository.findByCollegeClassId("class-5a")).thenReturn(List.of(student1));

        StudentDetectionResponse response = studentService.detectStudentByLastThreeDigits("class-5a", "042");

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("EXACT_MATCH", response.getCode());
        assertEquals("2GI24IS042", response.getUsn());
        assertEquals("Aarav Sharma", response.getStudentName());
        assertTrue(response.getMessage().contains("Aarav Sharma"));
    }

    @Test
    @DisplayName("Multiple students matching same 3 digits requires disambiguation")
    void testMultipleMatchesRequireDisambiguation() {
        when(classRepository.findById("class-5a")).thenReturn(Optional.of(testClass));
        when(studentRepository.findByCollegeClassId("class-5a")).thenReturn(List.of(student1, student2));

        StudentDetectionResponse response = studentService.detectStudentByLastThreeDigits("class-5a", "042");

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("MULTIPLE_MATCHES", response.getCode());
        assertNotNull(response.getCandidateUsns());
        assertEquals(2, response.getCandidateUsns().size());
        assertTrue(response.getMessage().contains("Multiple students"));
    }

    @Test
    @DisplayName("No matching student returns not found status")
    void testNoMatchingStudentReturnsNotFound() {
        when(classRepository.findById("class-5a")).thenReturn(Optional.of(testClass));
        when(studentRepository.findByCollegeClassId("class-5a")).thenReturn(Collections.emptyList());

        StudentDetectionResponse response = studentService.detectStudentByLastThreeDigits("class-5a", "999");

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("NOT_FOUND", response.getCode());
        assertTrue(response.getMessage().contains("999"));
    }

    @Test
    @DisplayName("Inactive student with matching 3 digits is excluded from detection")
    void testInactiveStudentIsExcluded() {
        when(classRepository.findById("class-5a")).thenReturn(Optional.of(testClass));
        when(studentRepository.findByCollegeClassId("class-5a")).thenReturn(List.of(inactiveStudent));

        StudentDetectionResponse response = studentService.detectStudentByLastThreeDigits("class-5a", "099");

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("NOT_FOUND", response.getCode());
    }

    @Test
    @DisplayName("Invalid 3-digit inputs (non-digits, too short, too long) return invalid input error")
    void testInvalidDigitInputReturnsError() {
        StudentDetectionResponse r1 = studentService.detectStudentByLastThreeDigits("class-5a", "42");
        assertFalse(r1.isSuccess());
        assertEquals("INVALID_INPUT", r1.getCode());

        StudentDetectionResponse r2 = studentService.detectStudentByLastThreeDigits("class-5a", "042A");
        assertFalse(r2.isSuccess());
        assertEquals("INVALID_INPUT", r2.getCode());

        StudentDetectionResponse r3 = studentService.detectStudentByLastThreeDigits("class-5a", null);
        assertFalse(r3.isSuccess());
        assertEquals("INVALID_INPUT", r3.getCode());
    }

    @Test
    @DisplayName("Missing classId returns invalid input error")
    void testMissingClassIdReturnsError() {
        StudentDetectionResponse response = studentService.detectStudentByLastThreeDigits(null, "042");
        assertFalse(response.isSuccess());
        assertEquals("INVALID_INPUT", response.getCode());
    }
}
