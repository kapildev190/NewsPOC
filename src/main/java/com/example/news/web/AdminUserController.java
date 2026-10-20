package com.example.news.web;

import com.example.news.dto.Dtos.*;
import com.example.news.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final UserService service;
    public AdminUserController(UserService service) { this.service = service; }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest r) { return service.create(r); }

    @GetMapping
    public List<UserResponse> list() { return service.list(); }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }
}
