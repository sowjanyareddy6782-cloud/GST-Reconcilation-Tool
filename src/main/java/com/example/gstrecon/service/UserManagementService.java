package com.example.gstrecon.service;

import com.example.gstrecon.model.*;
import com.example.gstrecon.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;

@Service
public class UserManagementService {
    private final AppUserRepository repository; private final PasswordEncoder passwordEncoder;
    public UserManagementService(AppUserRepository repository,PasswordEncoder passwordEncoder){this.repository=repository;this.passwordEncoder=passwordEncoder;}
    @Transactional(readOnly=true) public List<AppUser> allUsers(){return repository.findAll(org.springframework.data.domain.Sort.by("username"));}
    @Transactional public void createUser(String username,String password,UserRole role){
        String normalized=username==null?"":username.trim().toLowerCase(Locale.ROOT);
        if(!normalized.matches("[a-z0-9._-]{3,100}"))throw new IllegalArgumentException("Username must be 3–100 characters using letters, numbers, dot, underscore, or hyphen.");
        if(password==null||password.length()<12)throw new IllegalArgumentException("Password must be at least 12 characters.");
        if(role==null)throw new IllegalArgumentException("Choose an account role.");
        if(repository.findByUsernameIgnoreCase(normalized).isPresent())throw new IllegalArgumentException("That username already exists.");
        repository.save(new AppUser(normalized,passwordEncoder.encode(password),role));
    }
}
