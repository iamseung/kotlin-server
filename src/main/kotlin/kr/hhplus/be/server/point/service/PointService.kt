package kr.hhplus.be.server.point.service

import kr.hhplus.be.server.common.exception.ErrorCode
import kr.hhplus.be.server.common.exception.NotFoundException
import kr.hhplus.be.server.point.entity.Point
import kr.hhplus.be.server.point.repository.PointRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional
@Service
class PointService(
    private val pointRepository: PointRepository,
) {

    @Transactional(readOnly = true)
    fun getPointByUserId(userId: Long): Point {
        return pointRepository.findByUserId(userId)
            ?: throw NotFoundException(ErrorCode.ENTITY_NOT_FOUND)
    }

    fun chargePoint(userId: Long, amount: Int): Point {
        val point = getPointByUserId(userId)
        point.chargePoint(amount)

        return point
    }

    fun usePoint(userId: Long, amount: Int): Point {
        val point = getPointByUserId(userId)
        point.usePoint(amount)

        return point
    }
}
