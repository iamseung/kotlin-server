package kr.hhplus.be.server.infrastructure.cache

import com.fasterxml.jackson.databind.ObjectMapper
import kr.hhplus.be.server.application.usecase.concert.GetConcertsResult
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class ConcertCacheService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private const val CONCERTS_CACHE_KEY = "concerts:all"
        private val CACHE_TTL = Duration.ofHours(1) // 1시간
    }

    fun getConcerts(): GetConcertsResult? {
        val cached = redisTemplate.opsForValue().get(CONCERTS_CACHE_KEY) ?: return null
        return objectMapper.readValue(cached, GetConcertsResult::class.java)
    }

    fun setConcerts(result: GetConcertsResult) {
        val json = objectMapper.writeValueAsString(result)
        redisTemplate.opsForValue().set(CONCERTS_CACHE_KEY, json, CACHE_TTL)
    }

    fun evictConcerts() {
        redisTemplate.delete(CONCERTS_CACHE_KEY)
    }
}
