package kr.hhplus.be.server.domain.concert.repository

import kr.hhplus.be.server.domain.concert.model.ConcertScheduleModel

interface ConcertScheduleRepository {
    fun findById(id: Long): ConcertScheduleModel?
    fun findByIdOrThrow(id: Long): ConcertScheduleModel
    fun findAllByConcertId(concertId: Long): List<ConcertScheduleModel>
}
