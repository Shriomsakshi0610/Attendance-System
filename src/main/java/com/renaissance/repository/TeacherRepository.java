package com.renaissance.repository;

import com.renaissance.entity.Teacher;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherRepository extends MongoRepository<Teacher, String> {

    Optional<Teacher> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<Teacher> findByDepartment_Id(String departmentId);

    default List<Teacher> findByDepartmentId(String departmentId) {
        return findByDepartment_Id(departmentId);
    }
}
