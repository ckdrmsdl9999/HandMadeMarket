package com.project.marketplace.user.mapper;

import com.project.marketplace.user.dto.UserDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface UserMapper {

    Long insertUser(UserDto user);

    Optional<UserDto> findByUsername(String userName);


}
