package kr.hhplus.be.server.user.infrastructure

import kr.hhplus.be.server.common.exception.BusinessException
import kr.hhplus.be.server.common.exception.ErrorCode
import kr.hhplus.be.server.user.domain.model.User
import kr.hhplus.be.server.user.domain.repository.UserRepository
import kr.hhplus.be.server.user.entity.User as UserEntity
import kr.hhplus.be.server.user.repository.UserJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryAdapter(
    private val userJpaRepository: UserJpaRepository,
) : UserRepository {

    override fun save(user: User): User {
        val entity = toEntity(user)
        val saved = userJpaRepository.save(entity)
        return toDomain(saved)
    }

    override fun findById(id: Long): User? {
        return userJpaRepository.findByIdOrNull(id)?.let { toDomain(it) }
    }

    override fun findByIdOrThrow(id: Long): User {
        return findById(id) ?: throw BusinessException(ErrorCode.USER_NOT_FOUND)
    }

    override fun findByEmail(email: String): User? {
        return userJpaRepository.findByEmail(email)?.let { toDomain(it) }
    }

    override fun existsByEmail(email: String): Boolean {
        return userJpaRepository.existsByEmail(email)
    }

    private fun toDomain(entity: UserEntity): User {
        return User.reconstitute(
            id = entity.id!!,
            userName = entity.userName,
            email = entity.email,
            password = entity.password,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }

    private fun toEntity(domain: User): UserEntity {
        val entity = UserEntity(
            userName = domain.userName,
            email = domain.email,
            password = domain.password,
        )
        domain.getId()?.let { entity.id = it }
        return entity
    }
}
