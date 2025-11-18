package kr.hhplus.be.server.concert.infrastructure

import kr.hhplus.be.server.common.exception.BusinessException
import kr.hhplus.be.server.common.exception.ErrorCode
import kr.hhplus.be.server.concert.domain.model.ConcertSchedule
import kr.hhplus.be.server.concert.domain.repository.ConcertScheduleRepository
import kr.hhplus.be.server.concert.entity.ConcertSchedule as ConcertScheduleEntity
import kr.hhplus.be.server.concert.repository.ConcertScheduleJpaRepository
import kr.hhplus.be.server.concert.repository.ConcertJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class ConcertScheduleRepositoryAdapter(
    private val concertScheduleJpaRepository: ConcertScheduleJpaRepository,
    private val concertJpaRepository: ConcertJpaRepository,
) : ConcertScheduleRepository {

    override fun save(concertSchedule: ConcertSchedule): ConcertSchedule {
        val entity = toEntity(concertSchedule)
        val saved = concertScheduleJpaRepository.save(entity)
        return toDomain(saved)
    }

    override fun findById(id: Long): ConcertSchedule? {
        return concertScheduleJpaRepository.findByIdOrNull(id)?.let { toDomain(it) }
    }

    override fun findByIdOrThrow(id: Long): ConcertSchedule {
        return findById(id) ?: throw BusinessException(ErrorCode.CONCERT_SCHEDULE_NOT_FOUND)
    }

    override fun findAllByConcertId(concertId: Long): List<ConcertSchedule> {
        return concertScheduleJpaRepository.findByConcertId(concertId).map { toDomain(it) }
    }

    override fun findAvailableSchedules(concertId: Long, fromDate: LocalDate): List<ConcertSchedule> {
        return concertScheduleJpaRepository.findAvailableSchedules(concertId, fromDate).map { toDomain(it) }
    }

    private fun toDomain(entity: ConcertScheduleEntity): ConcertSchedule {
        return ConcertSchedule.reconstitute(
            id = entity.id!!,
            concertId = entity.concert.id!!,
            concertDate = entity.concertDate,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }

    private fun toEntity(domain: ConcertSchedule): ConcertScheduleEntity {
        val concert = concertJpaRepository.findByIdOrNull(domain.concertId)
            ?: throw BusinessException(ErrorCode.CONCERT_NOT_FOUND)

        val entity = ConcertScheduleEntity(
            concert = concert,
            concertDate = domain.concertDate,
        )
        domain.getId()?.let { entity.id = it }
        return entity
    }
}
