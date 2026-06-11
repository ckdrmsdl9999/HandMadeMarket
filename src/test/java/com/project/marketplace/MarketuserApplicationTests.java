package com.project.marketplace;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// CI에서 운영 설정 대신 테스트 전용 H2 설정으로 컨텍스트를 검증하도록 추가함
@ActiveProfiles("test")
@SpringBootTest
class MarketuserApplicationTests {

	@Test
	void contextLoads() {
	}

}
