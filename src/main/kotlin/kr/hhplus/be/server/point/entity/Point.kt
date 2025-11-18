package kr.hhplus.be.server.point.entity

import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import kr.hhplus.be.server.common.BaseEntity
import kr.hhplus.be.server.common.exception.BusinessException
import kr.hhplus.be.server.common.exception.ErrorCode
import kr.hhplus.be.server.user.entity.User

@Entity
class Point(
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: User,

    // 포인트 잔액
    var balance: Int,
) : BaseEntity() {

    fun chargePoint(amount: Int): Point {
        validatePositiveAmount(amount)
        this.balance += amount
        return this
    }

    fun usePoint(amount: Int): Point {
        validatePositiveAmount(amount)
        validateSufficientPoint(amount)

        this.balance -= amount
        return this
    }

    private fun validatePositiveAmount(amount: Int) {
        if (amount <= 0) {
            throw BusinessException(ErrorCode.INVALID_CHARGE_AMOUNT)
        }
    }

    private fun validateSufficientPoint(amount: Int) {
        if (balance < amount) {
            throw BusinessException(ErrorCode.INSUFFICIENT_POINTS)
        }
    }
}
