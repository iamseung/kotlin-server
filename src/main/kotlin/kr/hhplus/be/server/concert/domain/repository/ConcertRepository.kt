package kr.hhplus.be.server.concert.domain.repository

import kr.hhplus.be.server.concert.domain.model.Concert

interface ConcertRepository {
    fun save(concert: Concert): Concert
    fun findById(id: Long): Concert?
    fun findByIdOrThrow(id: Long): Concert
    fun findAll(): List<Concert>
}
