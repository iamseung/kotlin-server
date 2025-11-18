package kr.hhplus.be.server.point.service

import kr.hhplus.be.server.payment.entity.TransactionType
import kr.hhplus.be.server.point.entity.PointHistory
import kr.hhplus.be.server.point.repository.PointHistoryRepository
import kr.hhplus.be.server.user.service.UserService
import org.springframework.stereotype.Service

@Service
class PointHistoryService(
    private val pointHistoryRepository: PointHistoryRepository,
    private val userService: UserService,
) {

    fun savePointHistory(userId: Long, amount: Int, transactionType: TransactionType) {
        val user = userService.getUserEntity(userId)
        PointHistory.of(user, amount, transactionType).let {
            pointHistoryRepository.save(it)
        }
    }
}
