package kr.hhplus.be.server.point.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import kr.hhplus.be.server.common.BaseEntity
import kr.hhplus.be.server.payment.entity.TransactionType
import kr.hhplus.be.server.user.entity.User

@Entity
class PointHistory(
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    val amount: Int,

    @Enumerated(EnumType.STRING)
    val transactionType: TransactionType,
) : BaseEntity() {

    companion object {
        fun of(user: User, amount: Int, transactionType: TransactionType): PointHistory {
            return PointHistory(
                user = user,
                amount = amount,
                transactionType = transactionType,
            )
        }
    }
}