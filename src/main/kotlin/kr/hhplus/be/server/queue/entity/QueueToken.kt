package kr.hhplus.be.server.queue.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import kr.hhplus.be.server.common.BaseEntity
import kr.hhplus.be.server.common.exception.AuthorizationException
import kr.hhplus.be.server.common.exception.ErrorCode
import kr.hhplus.be.server.user.entity.User
import java.time.LocalDateTime
import java.util.*

@Entity
class QueueToken(
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false, unique = true)
    val token: String = UUID.randomUUID().toString(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var queueStatus: QueueStatus = QueueStatus.WAITING,

    @Column(nullable = false)
    var queuePosition: Int = 0,

    @Column
    var activatedAt: LocalDateTime? = null,

    @Column
    var expiresAt: LocalDateTime? = null,
) : BaseEntity() {

    fun activate() {
        this.queueStatus = QueueStatus.ACTIVE
        this.queuePosition = 0
        this.activatedAt = LocalDateTime.now()
        // 활성화 후 1시간 동안 유효
        this.expiresAt = LocalDateTime.now().plusHours(1)
    }

    fun expire() {
        this.queueStatus = QueueStatus.EXPIRED
    }

    fun validateActive() {
        if (queueStatus != QueueStatus.ACTIVE) {
            throw AuthorizationException(ErrorCode.QUEUE_TOKEN_NOT_ACTIVE)
        }

        // 만료 시간 체크
        expiresAt?.let {
            if (LocalDateTime.now().isAfter(it)) {
                expire()
                throw AuthorizationException(ErrorCode.TOKEN_EXPIRED)
            }
        }
    }

    fun updatePosition(newPosition: Int) {
        if (queueStatus == QueueStatus.WAITING) {
            this.queuePosition = newPosition
        }
    }

    companion object {
        fun of(user: User, position: Int): QueueToken {
            return QueueToken(
                user = user,
                queuePosition = position
            )
        }
    }
}
