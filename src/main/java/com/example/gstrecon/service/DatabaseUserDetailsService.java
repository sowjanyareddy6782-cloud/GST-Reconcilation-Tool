package com.example.gstrecon.service;

import com.example.gstrecon.repository.AppUserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final AppUserRepository repository;
    public DatabaseUserDetailsService(AppUserRepository repository){this.repository=repository;}
    @Override public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var account=repository.findByUsernameIgnoreCase(username).orElseThrow(()->new UsernameNotFoundException("User not found"));
        return User.withUsername(account.getUsername()).password(account.getPasswordHash()).roles(account.getRole().name()).disabled(!account.isEnabled()).build();
    }
}
