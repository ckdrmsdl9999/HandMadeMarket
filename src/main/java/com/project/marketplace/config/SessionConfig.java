package com.project.marketplace.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;

@Configuration
@EnableJdbcHttpSession(maxInactiveIntervalInSeconds = 1800) // 세션 만료 시간 30분 설정
public class SessionConfig {
    // 추가 설정이 필요하지 않으면 비워두어도 됩니다
}