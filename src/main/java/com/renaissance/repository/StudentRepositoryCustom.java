package com.renaissance.repository;

import com.renaissance.entity.Student;

import java.util.List;

public interface StudentRepositoryCustom {
    List<Student> searchStudents(String deptId, String classId, String keyword);

    List<Student> searchStudents(String deptId, String classId, String keyword, Integer page, Integer size);

    java.util.Map<String, Long> countStudentsByClassIds(java.util.Collection<String> classIds);
}
