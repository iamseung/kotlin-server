package kr.hhplus.be.server.api.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * OpenAPI/Swagger Configuration
 *
 * 컨트롤러의 어노테이션을 기반으로 API 명세를 자동 생성합니다.
 * Swagger UI는 http://localhost:8080/swagger-ui.html 에서 확인 가능합니다.
 */
@Configuration
class OpenAPIConfig {

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("HangHae Plus Concert Reservation API")
                    .description("콘서트 예약 시스템 API")
                    .version("1.0.0"),
            )
    }
}
