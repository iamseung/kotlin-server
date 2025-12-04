package kr.hhplus.be.server.application.scheduler

import kr.hhplus.be.server.domain.concert.service.SeatService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class SeatScheduler(
    private val seatService: SeatService,
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    companion object {
        private const val EXPIRATION_MINUTES = 5L
    }

    /**
     * 만료된 임시 좌석 복원 (5분 경과 후)
     * 매 1분마다 실행
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    fun restoreExpiredTemporarySeats() {
        try {
            val restoredCount = seatService.restoreExpiredTemporaryReservations(EXPIRATION_MINUTES)

            if (restoredCount > 0) {
                log.info("Restored $restoredCount expired temporary seats to AVAILABLE")
            }
        } catch (e: Exception) {
            log.error("Error while restoring expired temporary seats", e)
        }
    }
}
