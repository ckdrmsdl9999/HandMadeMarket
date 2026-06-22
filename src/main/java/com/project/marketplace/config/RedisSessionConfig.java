package com.project.marketplace.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.session.MapSession;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.SessionRepository;
import org.springframework.session.data.redis.config.ConfigureRedisAction;

import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RedisSessionConfig {

    @Bean
    public ConfigureRedisAction configureRedisAction() {
        // 운영 Redis에서 CONFIG 명령이 막혀 있어도 세션 저장소 초기화가 실패하지 않게 함
        return ConfigureRedisAction.NO_OP;
    }

    @Bean
    @Profile("test")
    public SessionRepository<MapSession> sessionRepository() {
        // 테스트에서는 외부 Redis 없이 메모리 세션으로 OAuth 흐름을 검증함
        return new MapSessionRepository(new ConcurrentHashMap<>());
    }
}
