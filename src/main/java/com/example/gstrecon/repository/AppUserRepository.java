package com.example.gstrecon.repository;

import com.example.gstrecon.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser,Long> {
    Optional<AppUser> findByUsernameIgnoreCase(String username);
}
