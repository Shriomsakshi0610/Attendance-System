package com.renaissance.repository;

import com.renaissance.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
}
