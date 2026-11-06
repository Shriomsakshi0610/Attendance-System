package com.renaissance.security;

import com.renaissance.entity.Teacher;
import com.renaissance.repository.TeacherRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final TeacherRepository teacherRepository;

    public CustomUserDetailsService(TeacherRepository teacherRepository) {
        this.teacherRepository = teacherRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        String trimmed = identifier != null ? identifier.trim() : "";
        Teacher teacher = teacherRepository.findByEmailIgnoreCase(trimmed)
                .or(() -> teacherRepository.findByEmployeeIdIgnoreCase(trimmed))
                .orElseThrow(() -> new UsernameNotFoundException("No teacher account found with email or employee ID: " + identifier));
        return new SecurityUser(teacher);
    }
}
