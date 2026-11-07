package com.renaissance.service;

import com.renaissance.config.CacheConfig;
import com.renaissance.dto.ClassDto;
import com.renaissance.dto.DepartmentDto;
import com.renaissance.dto.SubjectDto;
import com.renaissance.entity.CollegeClass;
import com.renaissance.entity.Subject;
import com.renaissance.entity.Teacher;
import com.renaissance.repository.ClassRepository;
import com.renaissance.repository.DepartmentRepository;
import com.renaissance.repository.SubjectRepository;
import com.renaissance.repository.TeacherRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.core.convert.LazyLoadingProxy;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TeacherService {

    private final DepartmentRepository departmentRepository;
    private final ClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;

    public TeacherService(DepartmentRepository departmentRepository,
                          ClassRepository classRepository,
                          SubjectRepository subjectRepository,
                          TeacherRepository teacherRepository) {
        this.departmentRepository = departmentRepository;
        this.classRepository = classRepository;
        this.subjectRepository = subjectRepository;
        this.teacherRepository = teacherRepository;
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

    private String extractDbRefId(Object entityOrProxy) {
        if (entityOrProxy == null) return null;
        if (entityOrProxy instanceof LazyLoadingProxy proxy) {
            try {
                com.mongodb.DBRef dbRef = proxy.toDBRef();
                if (dbRef != null && dbRef.getId() != null) {
                    return dbRef.getId().toString();
                }
            } catch (Exception ignored) {}
        }
        if (entityOrProxy instanceof CollegeClass c) {
            return c.getId();
        }
        if (entityOrProxy instanceof Subject s) {
            return s.getId();
        }
        return null;
    }

    /**
     * User-Specific: Returns ONLY classes assigned to the specified faculty member.
     * Evaluates direct assignments (assignedClasses) and class mentorships (classTeacher).
     */
    public List<ClassDto> getClassesForTeacher(Teacher teacher, String departmentId) {
        if (teacher == null) {
            return Collections.emptyList();
        }

        // Always query fresh from repository using unique teacher ID for 100% data consistency
        Teacher freshTeacher = teacherRepository.findById(teacher.getId()).orElse(teacher);

        List<CollegeClass> assignedClasses = new ArrayList<>();
        Set<String> seenClassIds = new HashSet<>();

        // 1. Explicitly assigned classes in teacher.assignedClasses
        if (freshTeacher.getAssignedClasses() != null) {
            for (CollegeClass c : freshTeacher.getAssignedClasses()) {
                try {
                    String classId = extractDbRefId(c);
                    if (classId != null) {
                        CollegeClass realClass = classRepository.findById(classId).orElse(c);
                        if (realClass != null && realClass.getId() != null && seenClassIds.add(realClass.getId())) {
                            assignedClasses.add(realClass);
                        }
                    } else if (c != null && c.getId() != null && seenClassIds.add(c.getId())) {
                        assignedClasses.add(c);
                    }
                } catch (Exception ignored) {}
            }
        }

        // 2. Classes where this teacher is set as class mentor
        List<CollegeClass> mentored = classRepository.findByClassTeacher_Id(freshTeacher.getId());
        if (mentored != null) {
            for (CollegeClass c : mentored) {
                if (c != null && c.getId() != null && seenClassIds.add(c.getId())) {
                    assignedClasses.add(c);
                }
            }
        }

        // 3. Fallback for admin with no specific class assignments: allow all
        if (freshTeacher.isAdmin() && assignedClasses.isEmpty()) {
            assignedClasses = (departmentId != null && !departmentId.isBlank()) ?
                    classRepository.findByDepartmentId(departmentId) :
                    classRepository.findAll();
        } else if (departmentId != null && !departmentId.isBlank()) {
            assignedClasses = assignedClasses.stream()
                    .filter(c -> c.getDepartment() != null && departmentId.equals(c.getDepartment().getId()))
                    .collect(Collectors.toList());
        }

        return assignedClasses.stream().map(c -> {
            ClassDto dto = new ClassDto(
                    c.getId(),
                    c.getDepartment() != null ? c.getDepartment().getId() : null,
                    c.getDepartment() != null ? c.getDepartment().getCode() : null,
                    c.getDepartment() != null ? c.getDepartment().getName() : null,
                    c.getSemester(),
                    c.getSection(),
                    c.getBatchYear()
            );
            if (c.getDisplayName() != null) {
                dto.setDisplayName(c.getDisplayName());
            }
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * User-Specific: Returns ONLY subjects assigned to the specified faculty member.
     * Evaluates direct assignments (assignedSubjects) and course instructor mappings (assignedTeacher).
     */
    public List<SubjectDto> getSubjectsForTeacher(Teacher teacher, String departmentId, Integer semester) {
        if (teacher == null) {
            return Collections.emptyList();
        }

        Teacher freshTeacher = teacherRepository.findById(teacher.getId()).orElse(teacher);

        List<Subject> assignedSubjects = new ArrayList<>();
        Set<String> seenSubjectIds = new HashSet<>();

        // 1. Explicitly assigned subjects in teacher.assignedSubjects
        if (freshTeacher.getAssignedSubjects() != null) {
            for (Subject s : freshTeacher.getAssignedSubjects()) {
                try {
                    String subjectId = extractDbRefId(s);
                    if (subjectId != null) {
                        Subject realSubj = subjectRepository.findById(subjectId).orElse(s);
                        if (realSubj != null && realSubj.getId() != null && seenSubjectIds.add(realSubj.getId())) {
                            assignedSubjects.add(realSubj);
                        }
                    } else if (s != null && s.getId() != null && seenSubjectIds.add(s.getId())) {
                        assignedSubjects.add(s);
                    }
                } catch (Exception ignored) {}
            }
        }

        // 2. Subjects where this teacher is set as assignedTeacher
        List<Subject> instructed = subjectRepository.findByAssignedTeacher_Id(freshTeacher.getId());
        if (instructed != null) {
            for (Subject s : instructed) {
                if (s != null && s.getId() != null && seenSubjectIds.add(s.getId())) {
                    assignedSubjects.add(s);
                }
            }
        }

        // 3. Fallback for admin with no specific subject assignments
        if (freshTeacher.isAdmin() && assignedSubjects.isEmpty()) {
            if (departmentId != null && !departmentId.isBlank() && semester != null) {
                assignedSubjects = subjectRepository.findByDepartmentIdAndSemester(departmentId, semester);
            } else if (departmentId != null && !departmentId.isBlank()) {
                assignedSubjects = subjectRepository.findByDepartmentId(departmentId);
            } else {
                assignedSubjects = subjectRepository.findAll();
            }
        } else {
            if (departmentId != null && !departmentId.isBlank()) {
                assignedSubjects = assignedSubjects.stream()
                        .filter(s -> s.getDepartment() != null && departmentId.equals(s.getDepartment().getId()))
                        .collect(Collectors.toList());
            }
            if (semester != null) {
                assignedSubjects = assignedSubjects.stream()
                        .filter(s -> s.getSemester() != null && semester.equals(s.getSemester()))
                        .collect(Collectors.toList());
            }
        }

        return assignedSubjects.stream().map(s -> new SubjectDto(
                s.getId(),
                s.getCode(),
                s.getName(),
                s.getDepartment() != null ? s.getDepartment().getId() : null,
                s.getDepartment() != null ? s.getDepartment().getCode() : null,
                s.getSemester()
        )).collect(Collectors.toList());
    }
}
