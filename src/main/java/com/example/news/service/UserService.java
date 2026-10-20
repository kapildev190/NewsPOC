package com.example.news.service;

import com.example.news.dto.Dtos.*;
import com.example.news.entity.AppUser;
import com.example.news.entity.Role;
import com.example.news.repo.UserRepository;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository repo;
    private final PasswordEncoder encoder;
    public UserService(UserRepository repo, PasswordEncoder encoder) { this.repo = repo; this.encoder = encoder; }

    @Transactional
    public UserResponse create(CreateUserRequest r) {
        if (r.role() == Role.ADMIN) throw new IllegalStateException("Admin users cannot be created via API");
        if (repo.existsByUsername(r.username())) throw new IllegalStateException("Username already exists");
        return UserResponse.of(repo.save(new AppUser(r.username(), encoder.encode(r.password()), r.fullName(), r.role())));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() { return repo.findAll().stream().map(UserResponse::of).toList(); }

    /** Soft delete: account is disabled (login blocked) but articles keep their author. */
    @Transactional
    public void delete(Long id) {
        AppUser u = repo.findById(id).orElseThrow(() -> new NotFoundException("User " + id));
        if (u.getRole() == Role.ADMIN) throw new IllegalStateException("Admin cannot be deleted");
        u.disable();
    }
}
