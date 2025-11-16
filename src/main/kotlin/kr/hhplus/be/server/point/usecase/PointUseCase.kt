package kr.hhplus.be.server.point.usecase

import kr.hhplus.be.server.common.exception.ErrorCode
import kr.hhplus.be.server.common.exception.NotFoundException
import kr.hhplus.be.server.common.util.findByIdOrThrow
import kr.hhplus.be.server.payment.entity.TransactionType
import kr.hhplus.be.server.point.dto.response.PointResponse
import kr.hhplus.be.server.point.repository.PointRepository
import kr.hhplus.be.server.point.service.PointHistoryService
import kr.hhplus.be.server.point.service.PointService
import kr.hhplus.be.server.user.repository.UserRepository
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
        val user = userService.getUser(userId)
        val point = pointService.getPointByUserId(user.id)

        return PointResponse.from(point)
    }

    fun chargePoint(userId: Long, amount: Int): PointResponse {
        val user = userService.getUser(userId)
        val point = pointService.chargePoint(user.id, amount)

        pointHistoryService.savePointHistory(user, amount, TransactionType.CHARGE)
        return PointResponse.from(point)
    }
}