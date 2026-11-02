package com.example.gstrecon.controller;

import com.example.gstrecon.model.UserRole;
import com.example.gstrecon.service.UserManagementService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/users")
public class AdminController {
    private final UserManagementService users;
    public AdminController(UserManagementService users){this.users=users;}
    @GetMapping public String users(Model model){model.addAttribute("users",users.allUsers());model.addAttribute("roles",UserRole.values());return "users";}
    @PostMapping public String create(@RequestParam String username,@RequestParam String password,@RequestParam UserRole role,Model model){
        try{users.createUser(username,password,role);model.addAttribute("success","Account created.");}
        catch(IllegalArgumentException ex){model.addAttribute("error",ex.getMessage());}
        model.addAttribute("users",users.allUsers());model.addAttribute("roles",UserRole.values());return "users";
    }
}
