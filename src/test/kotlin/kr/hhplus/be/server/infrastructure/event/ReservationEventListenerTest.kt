package kr.hhplus.be.server.infrastructure.event

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.retry.Retry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kr.hhplus.be.server.domain.reservation.event.ReservationConfirmedEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@DisplayName("ReservationEventListener 테스트")
class ReservationEventListenerTest {

    private lateinit var listener: ReservationEventListener
    private lateinit var webClient: WebClient
    private lateinit var requestBodyUriSpec: WebClient.RequestBodyUriSpec
    private lateinit var requestHeadersSpec: WebClient.RequestHeadersSpec<*>
    private lateinit var responseSpec: WebClient.ResponseSpec
    private lateinit var retry: Retry
    private lateinit var circuitBreaker: CircuitBreaker

    @BeforeEach
    fun setUp() {
        webClient = mockk()
        requestBodyUriSpec = mockk()
        requestHeadersSpec = mockk()
        responseSpec = mockk()
        retry = mockk()
        circuitBreaker = mockk()

        // Mock WebClient 체인
        every { webClient.post() } returns requestBodyUriSpec
        every { requestBodyUriSpec.uri(any<String>()) } returns requestBodyUriSpec
        every { requestBodyUriSpec.header(any(), any()) } returns requestBodyUriSpec
        every { requestBodyUriSpec.bodyValue(any()) } returns requestHeadersSpec
        every { requestHeadersSpec.retrieve() } returns responseSpec
        every { responseSpec.bodyToMono(String::class.java) } returns Mono.just("{\"status\":\"success\"}")

        listener = ReservationEventListener(webClient, retry, circuitBreaker)
    }

    @Test
    @DisplayName("예약 확정 이벤트 수신 시 외부 API 호출")
    fun `should call external api when reservation confirmed event received`() {
        // Given
        val event = ReservationConfirmedEvent(
            reservationId = 1L,
            concertId = 1L,
            concertTitle = "Test Concert",
            userId = 1L,
        )

        // When
        listener.onReservation(event)

        // Then
        Thread.sleep(1000) // 비동기 처리 대기

        verify(exactly = 1) { webClient.post() }
        verify(exactly = 1) { requestBodyUriSpec.uri("http://localhost:8080/api/mock/reservation") }
        verify(exactly = 1) { requestBodyUriSpec.header("X-Idempotency-Key", any()) }
    }
}
