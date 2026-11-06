package com.renaissance.security;

import com.renaissance.entity.Teacher;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class SecurityUser implements UserDetails {

    private final Teacher teacher;

    public SecurityUser(Teacher teacher) {
        this.teacher = teacher;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String role = teacher.getRole();
        if (role == null || role.isBlank()) {
            role = "ROLE_TEACHER";
        }
        if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
        }
        return Collections.singletonList(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return teacher.getPassword();
    }

    @Override
    public String getUsername() {
        return teacher.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return teacher.isActive();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return teacher.isActive();
    }
}
