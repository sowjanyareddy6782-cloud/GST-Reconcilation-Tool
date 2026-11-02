package com.example.gstrecon.config;

import com.example.gstrecon.model.*;
import com.example.gstrecon.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.Locale;

@Component
public class AdminBootstrap implements CommandLineRunner {
    private final AppUserRepository repository; private final PasswordEncoder encoder;
    @Value("${APP_ADMIN_USERNAME:admin}") private String username;
    @Value("${APP_ADMIN_PASSWORD:}") private String password;
    @Value("${APP_ADMIN_RESET_PASSWORD:false}") private boolean resetPassword;
    public AdminBootstrap(AppUserRepository repository,PasswordEncoder encoder){this.repository=repository;this.encoder=encoder;}
    @Override public void run(String... args){
        String normalized=username.trim().toLowerCase(Locale.ROOT);
        if(!normalized.matches("[a-z0-9._-]{3,100}"))throw new IllegalStateException("APP_ADMIN_USERNAME must be 3–100 characters using letters, numbers, dot, underscore, or hyphen.");
        if(repository.count()==0){
            if(!validPassword())throw new IllegalStateException("No users exist. Set APP_ADMIN_PASSWORD to a password of at least 12 characters (and optionally APP_ADMIN_USERNAME) to create the initial admin account.");
            repository.save(new AppUser(normalized,encoder.encode(password),UserRole.ADMIN));
            return;
        }
        if(resetPassword){
            if(!validPassword())throw new IllegalStateException("APP_ADMIN_RESET_PASSWORD=true requires APP_ADMIN_PASSWORD with at least 12 characters.");
            AppUser account=repository.findByUsernameIgnoreCase(normalized).orElseThrow(()->new IllegalStateException("The admin account named by APP_ADMIN_USERNAME was not found."));
            if(account.getRole()!=UserRole.ADMIN)throw new IllegalStateException("APP_ADMIN_USERNAME must identify an existing admin account to reset its password.");
            account.setPasswordHash(encoder.encode(password));repository.save(account);
        }
    }
    private boolean validPassword(){return password!=null&&password.length()>=12;}
}
