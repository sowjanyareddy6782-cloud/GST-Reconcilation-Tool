package com.example.gstrecon.config;

import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http)throws Exception{
        http.authorizeHttpRequests(auth->auth
                .requestMatchers("/login","/styles.css","/error","/favicon.ico").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST,"/reconcile").hasAnyRole("ACCOUNTANT","ADMIN")
                .anyRequest().authenticated())
            .formLogin(form->form.loginPage("/login").defaultSuccessUrl("/dashboard",true).permitAll())
            .logout(logout->logout.logoutSuccessUrl("/login?logout").permitAll());
        return http.build();
    }
    @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(12);}
}
