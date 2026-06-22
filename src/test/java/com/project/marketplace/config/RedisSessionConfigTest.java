package com.project.marketplace.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.SessionRepository;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Redis 의존성이 있어도 테스트 프로필에서는 외부 Redis 없이 컨텍스트가 떠야 함
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class RedisSessionConfigTest {

    @Autowired
    private ConfigureRedisAction configureRedisAction;

    // 테스트 요청이 Redis가 아니라 메모리 세션 저장소를 쓰는지 직접 검증함
    @Autowired
    private SessionRepository<?> sessionRepository;

    // Redis 세션 설정을 붙여도 OAuth 클라이언트 등록은 테스트 프로필에서 같이 검증되게 함
    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    // 네이버 OAuth2 인증 시작 URL이 실제 provider redirect까지 이어지는지 확인함
    @Autowired
    private MockMvc mockMvc;

    @Test
    void testProfileUsesInMemorySessionRepository() {
        assertThat(sessionRepository).isInstanceOf(MapSessionRepository.class);
        assertThat(configureRedisAction).isEqualTo(ConfigureRedisAction.NO_OP);
    }

    @Test
    void oauthClientRegistrationsAreLoadedForLoginFlow() {
        // 네이버/구글 로그인 설정이 테스트 프로필에서도 누락되지 않았는지 확인함
        ClientRegistration naver = clientRegistrationRepository.findByRegistrationId("naver");
        ClientRegistration google = clientRegistrationRepository.findByRegistrationId("google");

        assertThat(naver).isNotNull();
        assertThat(google).isNotNull();
        assertThat(naver.getScopes()).contains("name", "email");
        assertThat(google.getScopes()).contains("openid", "profile", "email");
    }

    @Test
    void naverOAuthAuthorizationEndpointRedirectsToProvider() throws Exception {
        // 실제 네이버 인증 성공 대신 우리 서버의 OAuth2 진입점이 정상 redirect되는지 검증함
        mockMvc.perform(get("/oauth2/authorization/naver"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("https://nid.naver.com/oauth2.0/authorize")));
    }
}
