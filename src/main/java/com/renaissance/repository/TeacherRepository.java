package com.renaissance.repository;

import com.renaissance.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Optional<Teacher> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}
