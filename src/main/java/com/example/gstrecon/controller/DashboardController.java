package com.example.gstrecon.controller;

import com.example.gstrecon.service.ReconciliationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    private final ReconciliationService service;
    public DashboardController(ReconciliationService service){this.service=service;}
    @GetMapping("/dashboard") public String dashboard(Authentication auth,Model model){
        boolean canViewAll=has(auth,"ROLE_ADMIN")||has(auth,"ROLE_MANAGER");
        model.addAttribute("dashboard",service.dashboard(auth.getName(),canViewAll));
        model.addAttribute("canUpload",has(auth,"ROLE_ADMIN")||has(auth,"ROLE_ACCOUNTANT"));
        model.addAttribute("canManageUsers",has(auth,"ROLE_ADMIN"));
        model.addAttribute("canViewAll",canViewAll);
        return "dashboard";
    }
    private boolean has(Authentication auth,String role){return auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals(role));}
}
