package kr.hhplus.be.server.domain.queue.model

import kr.hhplus.be.server.common.exception.AuthorizationException
import kr.hhplus.be.server.common.exception.ErrorCode
import java.time.LocalDateTime
import java.util.*

class QueueTokenModel private constructor(
    val userId: Long,
    val token: String,
    var queueStatus: QueueStatus,
    var activatedAt: LocalDateTime?,
    var expiresAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    var updatedAt: LocalDateTime,
) {

    val isActive: Boolean
        get() = queueStatus == QueueStatus.ACTIVE

    val isWaiting: Boolean
        get() = queueStatus == QueueStatus.WAITING

    fun activate() {
        this.queueStatus = QueueStatus.ACTIVE
        this.activatedAt = LocalDateTime.now()
        this.expiresAt = LocalDateTime.now().plusHours(1)
        this.updatedAt = LocalDateTime.now()
    }

    fun expire() {
        this.queueStatus = QueueStatus.EXPIRED
        this.updatedAt = LocalDateTime.now()
    }

    fun validateActive() {
        if (!isActive) {
            throw AuthorizationException(ErrorCode.QUEUE_TOKEN_NOT_ACTIVE)
        }

        expiresAt?.let {
            if (LocalDateTime.now().isAfter(it)) {
                expire()
                throw AuthorizationException(ErrorCode.TOKEN_EXPIRED)
            }
        }
    }

    companion object {
        fun create(userId: Long): QueueTokenModel {
            val now = LocalDateTime.now()
            return QueueTokenModel(
                userId = userId,
                token = UUID.randomUUID().toString(),
                queueStatus = QueueStatus.WAITING,
                activatedAt = null,
                expiresAt = null,
                createdAt = now,
                updatedAt = now,
            )
        }

        fun reconstitute(
            userId: Long,
            token: String,
            queueStatus: QueueStatus,
            activatedAt: LocalDateTime?,
            expiresAt: LocalDateTime?,
            createdAt: LocalDateTime,
            updatedAt: LocalDateTime,
        ): QueueTokenModel {
            return QueueTokenModel(
                userId = userId,
                token = token,
                queueStatus = queueStatus,
                activatedAt = activatedAt,
                expiresAt = expiresAt,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )
        }
    }
}
