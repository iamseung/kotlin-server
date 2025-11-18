package kr.hhplus.be.server.concert.infrastructure

import kr.hhplus.be.server.common.exception.BusinessException
import kr.hhplus.be.server.common.exception.ErrorCode
import kr.hhplus.be.server.concert.domain.model.Concert
import kr.hhplus.be.server.concert.domain.repository.ConcertRepository
import kr.hhplus.be.server.concert.entity.Concert as ConcertEntity
import kr.hhplus.be.server.concert.repository.ConcertJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class ConcertRepositoryAdapter(
    private val concertJpaRepository: ConcertJpaRepository,
) : ConcertRepository {

    override fun save(concert: Concert): Concert {
        val entity = toEntity(concert)
        val saved = concertJpaRepository.save(entity)
        return toDomain(saved)
    }

    override fun findById(id: Long): Concert? {
        return concertJpaRepository.findByIdOrNull(id)?.let { toDomain(it) }
    }

    override fun findByIdOrThrow(id: Long): Concert {
        return findById(id) ?: throw BusinessException(ErrorCode.CONCERT_NOT_FOUND)
    }

    override fun findAll(): List<Concert> {
        return concertJpaRepository.findAll().map { toDomain(it) }
    }

    private fun toDomain(entity: ConcertEntity): Concert {
        return Concert.reconstitute(
            id = entity.id!!,
            title = entity.title,
            description = entity.description,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }

    private fun toEntity(domain: Concert): ConcertEntity {
        val entity = ConcertEntity(
            title = domain.title,
            description = domain.description,
        )
        domain.getId()?.let { entity.id = it }
        return entity
    }
}
