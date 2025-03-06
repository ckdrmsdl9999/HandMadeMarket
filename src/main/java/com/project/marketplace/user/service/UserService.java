package com.project.marketplace.user.service;

import com.project.marketplace.user.dto.UserDto;
import com.project.marketplace.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserMapper userMapper;

    public void insertUser(UserDto user) {


        userMapper.insertUser(user);

    }

    public UserDto findUserByUsername(String username) {
        UserDto user = userMapper.findByUsername(username).orElse(null);
        return user;
    }

}
