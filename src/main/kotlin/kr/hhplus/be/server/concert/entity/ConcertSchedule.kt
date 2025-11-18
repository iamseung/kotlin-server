package kr.hhplus.be.server.concert.entity

import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import kr.hhplus.be.server.common.BaseEntity
import kr.hhplus.be.server.common.exception.BusinessException
import kr.hhplus.be.server.common.exception.ErrorCode
import java.time.LocalDate

@Entity
class ConcertSchedule(
    @ManyToOne
    @JoinColumn(name = "concert_id", nullable = false)
    val concert: Concert,

    val concertDate: LocalDate,

    @OneToMany(mappedBy = "concertSchedule")
    val seats: MutableList<Seat> = mutableListOf(),
) : BaseEntity() {

    val isAvailable: Boolean
        get() = !concertDate.isBefore(LocalDate.now())

    fun validateAvailable() {
        if (!isAvailable) {
            throw BusinessException(ErrorCode.CONCERT_SCHEDULE_EXPIRED)
        }
    }
}
