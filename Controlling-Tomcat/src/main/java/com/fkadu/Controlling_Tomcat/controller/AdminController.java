package com.fkadu.Controlling_Tomcat.controller;

import com.fkadu.Controlling_Tomcat.dto.RegisterRequest;
import com.fkadu.Controlling_Tomcat.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController {

    private final AuthService authService;

    public AdminController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/register-admin")
    public ResponseEntity<String> registerAdmin(@RequestBody RegisterRequest request) {
        String result = authService.adminRegister(request);
        if (result.startsWith("❌")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

}
