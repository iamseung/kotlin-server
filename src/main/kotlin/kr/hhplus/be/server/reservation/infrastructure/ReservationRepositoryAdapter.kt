package kr.hhplus.be.server.reservation.infrastructure

import kr.hhplus.be.server.common.exception.BusinessException
import kr.hhplus.be.server.common.exception.ErrorCode
import kr.hhplus.be.server.reservation.domain.model.Reservation
import kr.hhplus.be.server.reservation.domain.repository.ReservationRepository
import kr.hhplus.be.server.reservation.entity.Reservation as ReservationEntity
import kr.hhplus.be.server.reservation.entity.ReservationStatus as ReservationStatusEntity
import kr.hhplus.be.server.reservation.domain.model.ReservationStatus as ReservationStatusDomain
import kr.hhplus.be.server.reservation.repository.ReservationJpaRepository
import kr.hhplus.be.server.user.repository.UserJpaRepository
import kr.hhplus.be.server.concert.repository.SeatJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ReservationRepositoryAdapter(
    private val reservationJpaRepository: ReservationJpaRepository,
    private val userJpaRepository: UserJpaRepository,
    private val seatJpaRepository: SeatJpaRepository,
) : ReservationRepository {

    override fun save(reservation: Reservation): Reservation {
        val entity = toEntity(reservation)
        val saved = reservationJpaRepository.save(entity)
        return toDomain(saved)
    }

    override fun findById(id: Long): Reservation? {
        return reservationJpaRepository.findByIdOrNull(id)?.let { toDomain(it) }
    }

    override fun findByIdOrThrow(id: Long): Reservation {
        return findById(id) ?: throw BusinessException(ErrorCode.RESERVATION_NOT_FOUND)
    }

    override fun findByUserIdAndSeatId(userId: Long, seatId: Long): Reservation? {
        return reservationJpaRepository.findByUserIdAndSeatId(userId, seatId)?.let { toDomain(it) }
    }

    override fun findAllByUserId(userId: Long): List<Reservation> {
        return reservationJpaRepository.findAllByUserId(userId).map { toDomain(it) }
    }

    override fun findAllByStatus(status: ReservationStatusDomain): List<Reservation> {
        val entityStatus = toEntityStatus(status)
        return reservationJpaRepository.findAllByReservationStatus(entityStatus).map { toDomain(it) }
    }

    override fun findExpiredReservations(): List<Reservation> {
        return reservationJpaRepository.findExpiredReservations(LocalDateTime.now()).map { toDomain(it) }
    }

    private fun toDomain(entity: ReservationEntity): Reservation {
        return Reservation.reconstitute(
            id = entity.id!!,
            userId = entity.user.id!!,
            seatId = entity.seat.id!!,
            reservationStatus = toDomainStatus(entity.reservationStatus),
            temporaryReservedAt = entity.temporaryReservedAt,
            temporaryExpiredAt = entity.temporaryExpiredAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }

    private fun toEntity(domain: Reservation): ReservationEntity {
        val user = userJpaRepository.findByIdOrNull(domain.userId)
            ?: throw BusinessException(ErrorCode.USER_NOT_FOUND)
        val seat = seatJpaRepository.findByIdOrNull(domain.seatId)
            ?: throw BusinessException(ErrorCode.SEAT_NOT_FOUND)

        val entity = ReservationEntity.of(user, seat)
        domain.getId()?.let { entity.id = it }
        entity.reservationStatus = toEntityStatus(domain.reservationStatus)
        return entity
    }

    private fun toDomainStatus(status: ReservationStatusEntity): ReservationStatusDomain {
        return when (status) {
            ReservationStatusEntity.TEMPORARY -> ReservationStatusDomain.TEMPORARY
            ReservationStatusEntity.CONFIRMED -> ReservationStatusDomain.CONFIRMED
            ReservationStatusEntity.EXPIRED -> ReservationStatusDomain.CANCELED
            ReservationStatusEntity.CANCELED -> ReservationStatusDomain.CANCELED
        }
    }

    private fun toEntityStatus(status: ReservationStatusDomain): ReservationStatusEntity {
        return when (status) {
            ReservationStatusDomain.TEMPORARY -> ReservationStatusEntity.TEMPORARY
            ReservationStatusDomain.CONFIRMED -> ReservationStatusEntity.CONFIRMED
            ReservationStatusDomain.CANCELED -> ReservationStatusEntity.CANCELED
        }
    }
}
