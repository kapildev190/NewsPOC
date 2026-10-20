package com.example.news.security;

import com.example.news.repo.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Tells Spring Security how to load a user from our table. */
@Service
public class DbUserDetailsService implements UserDetailsService {
    private final UserRepository users;
    public DbUserDetailsService(UserRepository users) { this.users = users; }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return users.findByUsername(username)
                .map(u -> User.withUsername(u.getUsername()).password(u.getPassword())
                        .roles(u.getRole().name()).disabled(!u.isEnabled()).build())
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }
}
