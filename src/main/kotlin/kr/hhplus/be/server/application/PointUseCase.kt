package kr.hhplus.be.server.application

import kr.hhplus.be.server.payment.entity.TransactionType
import kr.hhplus.be.server.interfaces.dto.response.PointResponse
import kr.hhplus.be.server.point.service.PointHistoryService
import kr.hhplus.be.server.point.service.PointService
import kr.hhplus.be.server.user.service.UserService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class PointUseCase(
    private val userService: UserService,
    private val pointService: PointService,
    private val pointHistoryService: PointHistoryService,
) {

    @Transactional(readOnly = true)
    fun getPoints(userId: Long): PointResponse {
        userService.getUser(userId)
        val point = pointService.getPointByUserId(userId)

        return PointResponse.Companion.from(point)
    }

    fun chargePoint(userId: Long, amount: Int): PointResponse {
        userService.getUser(userId)
        val point = pointService.chargePoint(userId, amount)

        pointHistoryService.savePointHistory(userId, amount, TransactionType.CHARGE)
        return PointResponse.Companion.from(point)
    }
}