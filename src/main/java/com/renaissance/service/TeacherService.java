package com.renaissance.service;

import com.renaissance.config.CacheConfig;
import com.renaissance.dto.ClassDto;
import com.renaissance.dto.DepartmentDto;
import com.renaissance.dto.SubjectDto;
import com.renaissance.entity.CollegeClass;
import com.renaissance.entity.Subject;
import com.renaissance.repository.ClassRepository;
import com.renaissance.repository.DepartmentRepository;
import com.renaissance.repository.SubjectRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TeacherService {

    private final DepartmentRepository departmentRepository;
    private final ClassRepository classRepository;
    private final SubjectRepository subjectRepository;

    public TeacherService(DepartmentRepository departmentRepository,
                          ClassRepository classRepository,
                          SubjectRepository subjectRepository) {
        this.departmentRepository = departmentRepository;
        this.classRepository = classRepository;
        this.subjectRepository = subjectRepository;
    }

    @Cacheable(value = CacheConfig.CACHE_DEPARTMENTS, unless = "#result == null")
    public List<DepartmentDto> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(d -> new DepartmentDto(d.getId(), d.getCode(), d.getName()))
                .collect(Collectors.toList());
    }

    @Cacheable(value = CacheConfig.CACHE_CLASSES, key = "#departmentId != null ? #departmentId : 'ALL'", unless = "#result == null")
    public List<ClassDto> getClassesByDepartment(String departmentId) {
        List<CollegeClass> classes = (departmentId != null && !departmentId.isBlank()) ?
                classRepository.findByDepartmentId(departmentId) :
                classRepository.findAll();

        return classes.stream().map(c -> new ClassDto(
                c.getId(),
                c.getDepartment() != null ? c.getDepartment().getId() : null,
                c.getDepartment() != null ? c.getDepartment().getCode() : null,
                c.getDepartment() != null ? c.getDepartment().getName() : null,
                c.getSemester(),
                c.getSection(),
                c.getBatchYear()
        )).collect(Collectors.toList());
    }

    @Cacheable(value = CacheConfig.CACHE_SUBJECTS, key = "(#departmentId != null ? #departmentId : 'ALL') + '_' + (#semester != null ? #semester : 'ALL')", unless = "#result == null")
    public List<SubjectDto> getSubjects(String departmentId, Integer semester) {
        List<Subject> subjects;
        if (departmentId != null && !departmentId.isBlank() && semester != null) {
            subjects = subjectRepository.findByDepartmentIdAndSemester(departmentId, semester);
        } else if (departmentId != null && !departmentId.isBlank()) {
            subjects = subjectRepository.findByDepartmentId(departmentId);
        } else {
            subjects = subjectRepository.findAll();
        }

        return subjects.stream().map(s -> new SubjectDto(
                s.getId(),
                s.getCode(),
                s.getName(),
                s.getDepartment() != null ? s.getDepartment().getId() : null,
                s.getDepartment() != null ? s.getDepartment().getCode() : null,
                s.getSemester()
        )).collect(Collectors.toList());
    }
}
