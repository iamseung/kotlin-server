package kr.hhplus.be.server.concert.service

import kr.hhplus.be.server.common.util.findByIdOrThrow
import kr.hhplus.be.server.concert.entity.Concert
import kr.hhplus.be.server.concert.repository.ConcertRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ConcertService(
    private val concertRepository: ConcertRepository,
) {

    @Transactional(readOnly = true)
    fun getConcert(concertId: Long): Concert {
        return concertRepository.findByIdOrThrow(concertId)
    }
}