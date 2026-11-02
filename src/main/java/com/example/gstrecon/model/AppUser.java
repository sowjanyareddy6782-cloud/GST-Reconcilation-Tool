package com.example.gstrecon.model;

import jakarta.persistence.*;

@Entity
@Table(name="app_user",indexes=@Index(name="idx_user_username",columnList="username",unique=true))
public class AppUser {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=100,unique=true) private String username;
    @Column(nullable=false,length=100) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private UserRole role;
    @Column(nullable=false) private boolean enabled=true;
    protected AppUser() { }
    public AppUser(String username,String passwordHash,UserRole role){this.username=username;this.passwordHash=passwordHash;this.role=role;}
    public Long getId(){return id;} public String getUsername(){return username;} public String getPasswordHash(){return passwordHash;} public UserRole getRole(){return role;} public boolean isEnabled(){return enabled;}
    public void setPasswordHash(String passwordHash){this.passwordHash=passwordHash;}
}
