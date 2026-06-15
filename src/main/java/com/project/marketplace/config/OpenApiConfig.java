package com.project.marketplace.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    // Swagger UI 상단에 표시할 프로젝트 API 기본 정보를 설정함
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("HandMade Market API")
                        .description("수제품 마켓의 사용자, 상품, 장바구니, 주문, 배송 API 문서")
                        .version("v1"));
    }
}
