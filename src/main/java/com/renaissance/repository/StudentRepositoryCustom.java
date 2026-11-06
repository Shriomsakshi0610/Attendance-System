package com.renaissance.repository;

import com.renaissance.entity.Student;

import java.util.List;

public interface StudentRepositoryCustom {
    List<Student> searchStudents(String deptId, String classId, String keyword);
}
