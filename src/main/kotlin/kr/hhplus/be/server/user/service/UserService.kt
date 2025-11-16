package kr.hhplus.be.server.user.service

import kr.hhplus.be.server.common.util.findByIdOrThrow
import kr.hhplus.be.server.user.entity.User
import kr.hhplus.be.server.user.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
) {
    fun getUser(userId: Long): User {
        return userRepository.findByIdOrThrow(userId)
    }
}