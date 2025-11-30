package kr.hhplus.be.server.infrastructure.persistence.queue.entity

import kr.hhplus.be.server.domain.queue.model.QueueStatus
import kr.hhplus.be.server.domain.queue.model.QueueTokenModel
import java.time.LocalDateTime

/**
 * Redis Hash로 저장되는 QueueToken Entity
 */
data class QueueTokenRedisEntity(
    val userId: Long,
    val token: String,
    val queueStatus: QueueStatus,
    val activatedAt: LocalDateTime?,
    val expiresAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {

    fun toModel(): QueueTokenModel {
        return QueueTokenModel.reconstitute(
            userId = userId,
            token = token,
            queueStatus = queueStatus,
            activatedAt = activatedAt,
            expiresAt = expiresAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    /**
     * Redis Hash에 저장할 Map 형태로 변환
     */
    fun toHash(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to userId,
            "token" to token,
            "status" to queueStatus.name,
            "createdAt" to createdAt.toString(),
            "updatedAt" to updatedAt.toString(),
        )

        activatedAt?.let { map["activatedAt"] = it.toString() }
        expiresAt?.let { map["expiresAt"] = it.toString() }

        return map
    }

    companion object {
        fun fromDomain(queueTokenModel: QueueTokenModel): QueueTokenRedisEntity {
            return QueueTokenRedisEntity(
                userId = queueTokenModel.userId,
                token = queueTokenModel.token,
                queueStatus = queueTokenModel.queueStatus,
                activatedAt = queueTokenModel.activatedAt,
                expiresAt = queueTokenModel.expiresAt,
                createdAt = queueTokenModel.createdAt,
                updatedAt = queueTokenModel.updatedAt,
            )
        }

        /**
         * Redis Hash에서 가져온 Map을 Entity로 변환
         */
        fun fromHash(hash: Map<String, Any>): QueueTokenRedisEntity {
            return QueueTokenRedisEntity(
                userId = hash["userId"].toString().toLong(),
                token = hash["token"].toString(),
                queueStatus = QueueStatus.valueOf(hash["status"].toString()),
                activatedAt = hash["activatedAt"]?.toString()?.let { LocalDateTime.parse(it) },
                expiresAt = hash["expiresAt"]?.toString()?.let { LocalDateTime.parse(it) },
                createdAt = LocalDateTime.parse(hash["createdAt"].toString()),
                updatedAt = LocalDateTime.parse(hash["updatedAt"].toString()),
            )
        }
    }
}
