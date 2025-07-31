//package com.fkadu.Controlling_Tomcat.controller;
//
//import com.fkadu.Controlling_Tomcat.dto.RegisterRequest;
//import com.fkadu.Controlling_Tomcat.service.UserService;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//public class AdminController {
//
//    private final UserService userService;
//
//    public AdminController(UserService userService) {
//        this.userService = userService;
//    }
//
//    @PostMapping("/auth/register-admin")
//    public ResponseEntity<String> registerAdmin(@RequestBody RegisterRequest request) {
//        String result = userService.registerAdmin(request);
//        if (result.startsWith("❌")) {
//            return ResponseEntity.badRequest().body(result);
//        }
//        return ResponseEntity.ok(result);
//    }
//}
