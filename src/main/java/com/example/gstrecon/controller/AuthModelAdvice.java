package com.example.gstrecon.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class AuthModelAdvice {
    @ModelAttribute public void addAuthState(Model model,Authentication auth){
        boolean signedIn=auth!=null&&auth.isAuthenticated()&&!(auth instanceof AnonymousAuthenticationToken);
        model.addAttribute("signedIn",signedIn);
        if(!signedIn)return;
        model.addAttribute("currentUser",auth.getName());
        model.addAttribute("isAdmin",has(auth,"ROLE_ADMIN"));
        model.addAttribute("canUpload",has(auth,"ROLE_ACCOUNTANT")||has(auth,"ROLE_ADMIN"));
    }
    private boolean has(Authentication auth,String role){return auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals(role));}
}
