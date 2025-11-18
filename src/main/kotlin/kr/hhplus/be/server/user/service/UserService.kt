package kr.hhplus.be.server.user.service

import kr.hhplus.be.server.user.domain.model.User
import kr.hhplus.be.server.user.domain.repository.UserRepository
import kr.hhplus.be.server.user.entity.User as UserEntity
import kr.hhplus.be.server.user.repository.UserJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userJpaRepository: UserJpaRepository,
) {
    fun getUser(userId: Long): User {
        return userRepository.findByIdOrThrow(userId)
    }

    fun getUserEntity(userId: Long): UserEntity {
        return userJpaRepository.findByIdOrNull(userId)
            ?: throw kr.hhplus.be.server.common.exception.BusinessException(
                kr.hhplus.be.server.common.exception.ErrorCode.USER_NOT_FOUND
            )
    }
}
