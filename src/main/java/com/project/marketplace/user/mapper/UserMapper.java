package com.project.marketplace.user.mapper;

import com.project.marketplace.user.dto.UserDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;


@Mapper
public interface UserMapper {

    Long insertUser(UserDto user);

    Optional<UserDto> findByUsername(String userName);

    // ID로 사용자 찾기
    Optional<UserDto> findById(Long userId);

    // 모든 사용자 목록 조회
    List<UserDto> findAll();

    // 사용자 정보 업데이트
    int updateUser(UserDto user);

    // 사용자 역할 업데이트
    int updateRole(@Param("userId") Long userId, @Param("role") String role);

    // 사용자 삭제
    int deleteUser(Long userId);

}
