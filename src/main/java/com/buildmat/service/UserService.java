package com.buildmat.service;

import com.buildmat.model.*;
import com.buildmat.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service @RequiredArgsConstructor
public class UserService {
    private final UserRepository repo;
    private final PasswordEncoder encoder;

    public List<Map<String,Object>> getAll() {
        return repo.findAll().stream().map(this::toMap).collect(Collectors.toList());
    }

    public Map<String,Object> create(Map<String,Object> body, String callerRole) {
        if (repo.existsByUsername((String)body.get("username")))
            throw new RuntimeException("Username already exists");
        String targetRole = (String) body.getOrDefault("role", "USER");
        validateRoleAssignment(callerRole, targetRole);
        UserEntity u = UserEntity.builder()
            .username(((String)body.get("username")).trim().toLowerCase())
            .fullName((String)body.get("fullName"))
            .role(targetRole)
            .active((Boolean)body.getOrDefault("active", true))
            .passwordHash(encoder.encode((String)body.get("password")))
            .createdAt(LocalDateTime.now()).build();
        Map<String,Object> result = toMap(repo.save(u));
        log.info("User created: id={} username='{}' role={} by callerRole={}",
            result.get("id"), result.get("username"), result.get("role"), callerRole);
        return result;
    }

    public Map<String,Object> update(Long id, Map<String,Object> body, String callerRole) {
        UserEntity u = repo.findById(id).orElseThrow();
        String newRole = (String) body.getOrDefault("role", u.getRole());
        validateRoleAssignment(callerRole, newRole);
        u.setFullName((String) body.get("fullName"));
        u.setRole(newRole);
        u.setActive((Boolean) body.getOrDefault("active", u.isActive()));
        Map<String,Object> result = toMap(repo.save(u));
        log.info("User updated: id={} username='{}' role={} active={} by callerRole={}",
            id, result.get("username"), result.get("role"), result.get("active"), callerRole);
        return result;
    }

    public void changePassword(Long id, String newPassword) {
        UserEntity u = repo.findById(id).orElseThrow();
        u.setPasswordHash(encoder.encode(newPassword));
        repo.save(u);
        log.info("Password changed: id={} username='{}'", id, u.getUsername());
    }

    public void delete(Long id) {
        log.info("User deleted: id={}", id);
        repo.deleteById(id);
    }

    private void validateRoleAssignment(String callerRole, String targetRole) {
        if ("SUPER_ADMIN".equals(callerRole)) return;
        if ("ADMIN".equals(callerRole) && "USER".equals(targetRole)) return;
        throw new RuntimeException("Insufficient permissions to assign role: " + targetRole);
    }

    private Map<String,Object> toMap(UserEntity u) {
        return Map.of(
            "id", u.getId(), "username", u.getUsername(),
            "fullName", u.getFullName() != null ? u.getFullName() : "",
            "role", u.getRole(), "active", u.isActive()
        );
    }
}
