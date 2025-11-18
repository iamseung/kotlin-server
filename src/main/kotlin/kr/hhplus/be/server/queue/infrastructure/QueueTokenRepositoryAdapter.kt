package kr.hhplus.be.server.queue.infrastructure

import kr.hhplus.be.server.common.exception.BusinessException
import kr.hhplus.be.server.common.exception.ErrorCode
import kr.hhplus.be.server.queue.domain.model.QueueStatus
import kr.hhplus.be.server.queue.domain.model.QueueToken
import kr.hhplus.be.server.queue.domain.repository.QueueTokenRepository
import kr.hhplus.be.server.queue.entity.QueueToken as QueueTokenEntity
import kr.hhplus.be.server.queue.entity.QueueStatus as QueueStatusEntity
import kr.hhplus.be.server.queue.domain.model.QueueStatus as QueueStatusDomain
import kr.hhplus.be.server.queue.repository.QueueTokenJpaRepository
import kr.hhplus.be.server.user.repository.UserJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class QueueTokenRepositoryAdapter(
    private val queueTokenJpaRepository: QueueTokenJpaRepository,
    private val userJpaRepository: UserJpaRepository,
) : QueueTokenRepository {

    override fun save(queueToken: QueueToken): QueueToken {
        val entity = toEntity(queueToken)
        val saved = queueTokenJpaRepository.save(entity)
        return toDomain(saved)
    }

    override fun findById(id: Long): QueueToken? {
        return queueTokenJpaRepository.findByIdOrNull(id)?.let { toDomain(it) }
    }

    override fun findByIdOrThrow(id: Long): QueueToken {
        return findById(id) ?: throw BusinessException(ErrorCode.QUEUE_TOKEN_NOT_FOUND)
    }

    override fun findByToken(token: String): QueueToken? {
        return queueTokenJpaRepository.findByToken(token)?.let { toDomain(it) }
    }

    override fun findByTokenOrThrow(token: String): QueueToken {
        return findByToken(token) ?: throw BusinessException(ErrorCode.QUEUE_TOKEN_NOT_FOUND)
    }

    override fun findAllByStatus(status: QueueStatus): List<QueueToken> {
        val entityStatus = toEntityStatus(status)
        return queueTokenJpaRepository.findAllByQueueStatusOrderByCreatedAtAsc(entityStatus).map { toDomain(it) }
    }

    override fun findTopWaitingTokens(limit: Int): List<QueueToken> {
        return queueTokenJpaRepository.findTopWaitingTokens(limit).map { toDomain(it) }
    }

    override fun countByStatus(status: QueueStatus): Long {
        val entityStatus = toEntityStatus(status)
        return queueTokenJpaRepository.countByQueueStatus(entityStatus)
    }

    override fun findExpiredTokens(): List<QueueToken> {
        return queueTokenJpaRepository.findExpiredTokens(LocalDateTime.now()).map { toDomain(it) }
    }

    private fun toDomain(entity: QueueTokenEntity): QueueToken {
        return QueueToken.reconstitute(
            id = entity.id!!,
            userId = entity.user.id!!,
            token = entity.token,
            queueStatus = toDomainStatus(entity.queueStatus),
            queuePosition = entity.queuePosition,
            activatedAt = entity.activatedAt,
            expiresAt = entity.expiresAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }

    private fun toEntity(domain: QueueToken): QueueTokenEntity {
        val user = userJpaRepository.findByIdOrNull(domain.userId)
            ?: throw BusinessException(ErrorCode.USER_NOT_FOUND)

        val entity = QueueTokenEntity.of(user, domain.queuePosition)
        domain.getId()?.let { entity.id = it }
        entity.queueStatus = toEntityStatus(domain.queueStatus)
        domain.activatedAt?.let { entity.activatedAt = it }
        domain.expiresAt?.let { entity.expiresAt = it }
        return entity
    }

    private fun toDomainStatus(status: QueueStatusEntity): QueueStatusDomain {
        return when (status) {
            QueueStatusEntity.WAITING -> QueueStatusDomain.WAITING
            QueueStatusEntity.ACTIVE -> QueueStatusDomain.ACTIVE
            QueueStatusEntity.EXPIRED -> QueueStatusDomain.EXPIRED
        }
    }

    private fun toEntityStatus(status: QueueStatusDomain): QueueStatusEntity {
        return when (status) {
            QueueStatusDomain.WAITING -> QueueStatusEntity.WAITING
            QueueStatusDomain.ACTIVE -> QueueStatusEntity.ACTIVE
            QueueStatusDomain.EXPIRED -> QueueStatusEntity.EXPIRED
        }
    }
}
