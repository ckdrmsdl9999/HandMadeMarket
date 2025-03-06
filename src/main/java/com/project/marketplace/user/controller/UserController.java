package com.project.marketplace.user.controller;

import com.project.marketplace.user.dto.UserDto;
import com.project.marketplace.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody  UserDto userDto) {
        System.out.println(userDto);
        userService.insertUser(userDto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/signin")
    public void signin(){
    System.out.print("hi~~");
    }



}
