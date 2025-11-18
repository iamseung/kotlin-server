package kr.hhplus.be.server.point.service

import kr.hhplus.be.server.payment.entity.TransactionType
import kr.hhplus.be.server.point.entity.PointHistory
import kr.hhplus.be.server.point.repository.PointHistoryRepository
import kr.hhplus.be.server.user.entity.User
import org.springframework.stereotype.Service

@Service
class PointHistoryService(
    private val pointHistoryRepository: PointHistoryRepository,
) {

    fun savePointHistory(user: User, amount: Int, transactionType: TransactionType) {
        PointHistory.of(user, amount, transactionType).let {
            pointHistoryRepository.save(it)
        }
    }
}
