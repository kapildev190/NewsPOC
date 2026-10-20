package com.example.news.config;

import com.example.news.entity.AppUser;
import com.example.news.entity.Role;
import com.example.news.repo.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Bootstrap: someone must exist to create the first editors/journalists. Change this password! */
@Configuration
public class AdminSeeder {
    @Bean
    CommandLineRunner seedAdmin(UserRepository users, PasswordEncoder encoder) {
        return args -> {
            if (!users.existsByUsername("admin"))
                users.save(new AppUser("admin", encoder.encode("admin123"), "System Admin", Role.ADMIN));
        };
    }
}
