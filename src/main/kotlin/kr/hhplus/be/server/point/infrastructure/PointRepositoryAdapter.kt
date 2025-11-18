package kr.hhplus.be.server.point.infrastructure

import kr.hhplus.be.server.common.exception.BusinessException
import kr.hhplus.be.server.common.exception.ErrorCode
import kr.hhplus.be.server.point.domain.model.Point
import kr.hhplus.be.server.point.domain.repository.PointRepository
import kr.hhplus.be.server.point.entity.Point as PointEntity
import kr.hhplus.be.server.point.repository.PointJpaRepository
import kr.hhplus.be.server.user.repository.UserJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class PointRepositoryAdapter(
    private val pointJpaRepository: PointJpaRepository,
    private val userJpaRepository: UserJpaRepository,
) : PointRepository {

    override fun save(point: Point): Point {
        val entity = toEntity(point)
        val saved = pointJpaRepository.save(entity)
        return toDomain(saved)
    }

    override fun findById(id: Long): Point? {
        return pointJpaRepository.findByIdOrNull(id)?.let { toDomain(it) }
    }

    override fun findByIdOrThrow(id: Long): Point {
        return findById(id) ?: throw BusinessException(ErrorCode.POINT_NOT_FOUND)
    }

    override fun findByUserId(userId: Long): Point? {
        return pointJpaRepository.findByUserId(userId)?.let { toDomain(it) }
    }

    override fun findByUserIdOrThrow(userId: Long): Point {
        return findByUserId(userId) ?: throw BusinessException(ErrorCode.POINT_NOT_FOUND)
    }

    override fun findByUserIdWithLock(userId: Long): Point? {
        return pointJpaRepository.findByUserIdWithLock(userId)?.let { toDomain(it) }
    }

    private fun toDomain(entity: PointEntity): Point {
        return Point.reconstitute(
            id = entity.id!!,
            userId = entity.user.id!!,
            balance = entity.balance,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }

    private fun toEntity(domain: Point): PointEntity {
        val user = userJpaRepository.findByIdOrNull(domain.userId)
            ?: throw BusinessException(ErrorCode.USER_NOT_FOUND)

        val entity = PointEntity(
            user = user,
            balance = domain.balance,
        )
        domain.getId()?.let { entity.id = it }
        return entity
    }
}
