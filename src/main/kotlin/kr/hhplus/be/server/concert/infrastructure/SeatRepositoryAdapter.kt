package kr.hhplus.be.server.concert.infrastructure

import kr.hhplus.be.server.common.exception.BusinessException
import kr.hhplus.be.server.common.exception.ErrorCode
import kr.hhplus.be.server.concert.domain.model.Seat
import kr.hhplus.be.server.concert.domain.repository.SeatRepository
import kr.hhplus.be.server.concert.entity.Seat as SeatEntity
import kr.hhplus.be.server.concert.entity.SeatStatus as SeatStatusEntity
import kr.hhplus.be.server.concert.domain.model.SeatStatus as SeatStatusDomain
import kr.hhplus.be.server.concert.repository.SeatJpaRepository
import kr.hhplus.be.server.concert.repository.ConcertScheduleJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class SeatRepositoryAdapter(
    private val seatJpaRepository: SeatJpaRepository,
    private val concertScheduleJpaRepository: ConcertScheduleJpaRepository,
) : SeatRepository {

    override fun save(seat: Seat): Seat {
        val entity = toEntity(seat)
        val saved = seatJpaRepository.save(entity)
        return toDomain(saved)
    }

    override fun findById(id: Long): Seat? {
        return seatJpaRepository.findByIdOrNull(id)?.let { toDomain(it) }
    }

    override fun findByIdOrThrow(id: Long): Seat {
        return findById(id) ?: throw BusinessException(ErrorCode.SEAT_NOT_FOUND)
    }

    override fun findByIdWithLock(id: Long): Seat? {
        return seatJpaRepository.findByIdWithLock(id)?.let { toDomain(it) }
    }

    override fun findAllByConcertScheduleId(concertScheduleId: Long): List<Seat> {
        return seatJpaRepository.findAllByConcertScheduleId(concertScheduleId).map { toDomain(it) }
    }

    override fun findAllByConcertScheduleIdAndStatus(concertScheduleId: Long, status: SeatStatusDomain): List<Seat> {
        val entityStatus = toEntityStatus(status)
        return seatJpaRepository.findAllByConcertScheduleIdAndSeatStatus(concertScheduleId, entityStatus).map { toDomain(it) }
    }

    private fun toDomain(entity: SeatEntity): Seat {
        return Seat.reconstitute(
            id = entity.id!!,
            concertScheduleId = entity.concertSchedule.id!!,
            seatNumber = entity.seatNumber,
            seatStatus = toDomainStatus(entity.seatStatus),
            price = entity.price,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }

    private fun toEntity(domain: Seat): SeatEntity {
        val concertSchedule = concertScheduleJpaRepository.findByIdOrNull(domain.concertScheduleId)
            ?: throw BusinessException(ErrorCode.CONCERT_SCHEDULE_NOT_FOUND)

        val entity = SeatEntity(
            concertSchedule = concertSchedule,
            seatNumber = domain.seatNumber,
            seatStatus = toEntityStatus(domain.seatStatus),
            price = domain.price,
        )
        domain.getId()?.let { entity.id = it }
        return entity
    }

    private fun toDomainStatus(status: SeatStatusEntity): SeatStatusDomain {
        return when (status) {
            SeatStatusEntity.AVAILABLE -> SeatStatusDomain.AVAILABLE
            SeatStatusEntity.TEMPORARY_RESERVED -> SeatStatusDomain.TEMPORARY_RESERVED
            SeatStatusEntity.RESERVED -> SeatStatusDomain.RESERVED
        }
    }

    private fun toEntityStatus(status: SeatStatusDomain): SeatStatusEntity {
        return when (status) {
            SeatStatusDomain.AVAILABLE -> SeatStatusEntity.AVAILABLE
            SeatStatusDomain.TEMPORARY_RESERVED -> SeatStatusEntity.TEMPORARY_RESERVED
            SeatStatusDomain.RESERVED -> SeatStatusEntity.RESERVED
        }
    }
}
