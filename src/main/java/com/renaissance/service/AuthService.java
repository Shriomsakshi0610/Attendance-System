package com.renaissance.service;

import com.renaissance.dto.LoginRequest;
import com.renaissance.dto.LoginResponse;
import com.renaissance.dto.TeacherDto;
import com.renaissance.entity.Teacher;
import com.renaissance.exception.UnauthorizedException;
import com.renaissance.repository.TeacherRepository;
import com.renaissance.security.SecurityUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final TeacherRepository teacherRepository;

    public AuthService(AuthenticationManager authenticationManager, TeacherRepository teacherRepository) {
        this.authenticationManager = authenticationManager;
        this.teacherRepository = teacherRepository;
    }

    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail().trim(), request.getPassword())
            );

            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);

            HttpSession session = httpRequest.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

            SecurityUser userDetails = (SecurityUser) authentication.getPrincipal();
            Teacher teacher = userDetails.getTeacher();

            TeacherDto dto = toTeacherDto(teacher);
            return new LoginResponse("Login successful", dto, true);
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid email or password");
        }
    }

    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    public Teacher getCurrentTeacher() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new UnauthorizedException("You must be logged in as a teacher to perform this operation.");
        }

        if (auth.getPrincipal() instanceof SecurityUser securityUser) {
            return teacherRepository.findById(securityUser.getTeacher().getId())
                    .orElseThrow(() -> new UnauthorizedException("Teacher profile not found."));
        }

        String email = auth.getName();
        return teacherRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("Teacher not found for current session."));
    }

    public TeacherDto getCurrentTeacherDto() {
        return toTeacherDto(getCurrentTeacher());
    }

    public TeacherDto toTeacherDto(Teacher teacher) {
        if (teacher == null) return null;
        TeacherDto dto = new TeacherDto(
                teacher.getId(),
                teacher.getName(),
                teacher.getEmail(),
                teacher.getEmployeeId(),
                teacher.getDepartment() != null ? teacher.getDepartment().getName() : "General",
                teacher.getDepartment() != null ? teacher.getDepartment().getCode() : "GEN",
                teacher.getRole()
        );
        if (teacher.getDepartment() != null) {
            dto.setDepartmentId(teacher.getDepartment().getId());
        }
        dto.setPhone(teacher.getPhone());
        dto.setStatus(teacher.getStatus() != null ? teacher.getStatus() : "ACTIVE");

        if (teacher.getAssignedClasses() != null) {
            dto.setAssignedClassIds(teacher.getAssignedClasses().stream().map(c -> c.getId()).toList());
            dto.setAssignedClassNames(teacher.getAssignedClasses().stream().map(c -> c.getDisplayName()).toList());
        }
        if (teacher.getAssignedSubjects() != null) {
            dto.setAssignedSubjectIds(teacher.getAssignedSubjects().stream().map(s -> s.getId()).toList());
            dto.setAssignedSubjectNames(teacher.getAssignedSubjects().stream().map(s -> s.getName()).toList());
        }
        return dto;
    }
}
