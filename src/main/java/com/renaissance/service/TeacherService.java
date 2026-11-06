package com.renaissance.service;

import com.renaissance.dto.ClassDto;
import com.renaissance.dto.DepartmentDto;
import com.renaissance.dto.SubjectDto;
import com.renaissance.entity.CollegeClass;
import com.renaissance.entity.Department;
import com.renaissance.entity.Subject;
import com.renaissance.repository.ClassRepository;
import com.renaissance.repository.DepartmentRepository;
import com.renaissance.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public List<DepartmentDto> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(d -> new DepartmentDto(d.getId(), d.getCode(), d.getName()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ClassDto> getClassesByDepartment(Long departmentId) {
        List<CollegeClass> classes = departmentId != null ?
                classRepository.findByDepartmentId(departmentId) :
                classRepository.findAll();

        return classes.stream().map(c -> new ClassDto(
                c.getId(),
                c.getDepartment().getId(),
                c.getDepartment().getCode(),
                c.getDepartment().getName(),
                c.getSemester(),
                c.getSection(),
                c.getBatchYear()
        )).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SubjectDto> getSubjects(Long departmentId, Integer semester) {
        List<Subject> subjects;
        if (departmentId != null && semester != null) {
            subjects = subjectRepository.findByDepartmentIdAndSemester(departmentId, semester);
        } else if (departmentId != null) {
            subjects = subjectRepository.findByDepartmentId(departmentId);
        } else {
            subjects = subjectRepository.findAll();
        }

        return subjects.stream().map(s -> new SubjectDto(
                s.getId(),
                s.getCode(),
                s.getName(),
                s.getDepartment().getId(),
                s.getDepartment().getCode(),
                s.getSemester()
        )).collect(Collectors.toList());
    }
}
