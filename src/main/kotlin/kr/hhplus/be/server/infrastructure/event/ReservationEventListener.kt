package kr.hhplus.be.server.infrastructure.event

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator
import io.github.resilience4j.reactor.retry.RetryOperator
import io.github.resilience4j.retry.Retry
import kr.hhplus.be.server.domain.reservation.event.ReservationConfirmedEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * 예약 확정 이벤트 리스너
 *
 * 역할:
 * 1. 결제 완료 후 실시간으로 예약 정보를 외부 API에 전송
 * 2. 비동기 처리 (@Async) - 메인 플로우(결제)에 영향 없음
 * 3. 트랜잭션 커밋 후 실행 - 데이터 일관성 보장
 *
 * 동시성 처리:
 * - @Async 스레드풀에서 비동기 실행
 * - WebClient 논블로킹 I/O로 리소스 효율적
 * - Resilience4j 재시도 + 서킷브레이커로 장애 격리
 */
@Component
class ReservationEventListener(
    @Qualifier("externalApiWebClient")
    private val webClient: WebClient,

    @Qualifier("dataPlatformRetry")
    private val retry: Retry,

    @Qualifier("dataPlatformCircuitBreaker")
    private val circuitBreaker: CircuitBreaker,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 예약 확정 이벤트 처리
     *
     * 실행 흐름:
     * 1. 트랜잭션 커밋 후 비동기 실행
     * 2. WebClient로 Mock API 호출
     * 3. 재시도 (최대 3회, 지수 백오프: 100ms → 200ms → 400ms)
     * 4. 서킷브레이커 (실패율 50% 시 30초간 차단)
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onReservation(event: ReservationConfirmedEvent) {
        logger.info(
            "예약 확정 이벤트 수신 - reservationId={}, concertId={}, userId={}",
            event.reservationId,
            event.concertId,
            event.userId,
        )

        val idempotencyKey = "reservation-${event.reservationId}-${System.currentTimeMillis()}"
        val payload = createPayload(event)

        sendToExternalApi(idempotencyKey, payload)
            .doOnSuccess { response ->
                logger.info(
                    "외부 API 전송 성공 - reservationId={}, response={}",
                    event.reservationId,
                    response,
                )
            }
            .doOnError { error ->
                logger.error(
                    "외부 API 전송 실패 - reservationId={}, error={}",
                    event.reservationId,
                    error.message,
                    error,
                )
            }
            .subscribe()
    }

    /**
     * 외부 API로 전송
     *
     * - Resilience4j 재시도 + 서킷브레이커 적용
     * - 타임아웃 3초
     */
    private fun sendToExternalApi(
        idempotencyKey: String,
        payload: Map<String, Any>,
    ): Mono<String> {
        return webClient.post()
            .uri("http://localhost:8080/api/mock/reservation")
            .header("X-Idempotency-Key", idempotencyKey)
            .bodyValue(payload)
            .retrieve()
            .bodyToMono(String::class.java)
            .timeout(Duration.ofSeconds(3))
            .transformDeferred(RetryOperator.of(retry))
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
    }

    /**
     * 전송 페이로드 생성
     */
    private fun createPayload(event: ReservationConfirmedEvent): Map<String, Any> {
        return mapOf(
            "eventType" to "RESERVATION_CONFIRMED",
            "reservationId" to event.reservationId,
            "concertId" to event.concertId,
            "concertTitle" to event.concertTitle,
            "userId" to event.userId,
            "timestamp" to System.currentTimeMillis(),
        )
    }
}
