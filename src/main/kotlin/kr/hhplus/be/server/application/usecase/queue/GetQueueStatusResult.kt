package kr.hhplus.be.server.application.usecase.queue

import kr.hhplus.be.server.domain.queue.model.QueueStatus

data class GetQueueStatusResult(
    val token: String,
    val status: QueueStatus,
    val position: Long
)
