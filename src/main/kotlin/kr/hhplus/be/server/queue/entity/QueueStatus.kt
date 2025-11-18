package kr.hhplus.be.server.queue.entity

/**
 * 대기열 상태
 */
enum class QueueStatus {
    /**
     * 대기 중 - 아직 서비스 이용 불가
     */
    WAITING,

    /**
     * 활성화 - 서비스 이용 가능
     */
    ACTIVE,

    /**
     * 만료됨 - 토큰 기간 만료 또는 사용 완료
     */
    EXPIRED,
}
