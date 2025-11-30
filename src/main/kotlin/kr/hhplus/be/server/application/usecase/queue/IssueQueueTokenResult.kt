package kr.hhplus.be.server.application.usecase.queue

import kr.hhplus.be.server.domain.queue.model.QueueStatus
import java.time.LocalDateTime

data class IssueQueueTokenResult(
    val token: String,
    val status: QueueStatus,
    val position: Long,
    val createdAt: LocalDateTime,
)
