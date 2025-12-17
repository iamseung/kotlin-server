package kr.hhplus.be.server.infrastructure.persistence.redis

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Repository
import java.time.Duration

/**
 * Redis 명령어를 래핑한 범용 Repository
 *
 * Redis의 다양한 자료구조(String, Sorted Set, Hash)에 대한
 * 저수준 명령어를 간단한 메서드로 제공합니다.
 *
 * @property stringRedisTemplate String 기반 Redis 작업용 템플릿
 * @property redisTemplate 객체 직렬화를 지원하는 Redis 템플릿
 */
@Repository
class RedisRepository(
    private val stringRedisTemplate: StringRedisTemplate,
    private val redisTemplate: RedisTemplate<String, Any>,
) {

    // ========================================
    // String (Value) Operations
    // ========================================

    /**
     * 문자열 값을 저장합니다.
     * @param key Redis 키
     * @param value 저장할 값
     */
    fun set(key: String, value: String) {
        stringRedisTemplate.opsForValue().set(key, value)
    }

    /**
     * 문자열 값을 TTL과 함께 저장합니다.
     * @param key Redis 키
     * @param value 저장할 값
     * @param duration 만료 시간
     */
    fun set(key: String, value: String, duration: Duration) {
        stringRedisTemplate.opsForValue().set(key, value, duration)
    }

    /**
     * 객체 값을 저장합니다 (JSON 직렬화).
     * @param key Redis 키
     * @param value 저장할 객체
     */
    fun setObject(key: String, value: Any) {
        redisTemplate.opsForValue().set(key, value)
    }

    /**
     * 객체 값을 TTL과 함께 저장합니다 (JSON 직렬화).
     * @param key Redis 키
     * @param value 저장할 객체
     * @param duration 만료 시간
     */
    fun setObject(key: String, value: Any, duration: Duration) {
        redisTemplate.opsForValue().set(key, value, duration)
    }

    /**
     * 문자열 값을 조회합니다.
     * @param key Redis 키
     * @return 저장된 값, 없으면 null
     */
    fun get(key: String): String? {
        return stringRedisTemplate.opsForValue().get(key)
    }

    /**
     * 객체 값을 조회합니다 (JSON 역직렬화).
     * @param key Redis 키
     * @return 저장된 객체, 없으면 null
     */
    fun getObject(key: String): Any? {
        return redisTemplate.opsForValue().get(key)
    }

    /**
     * 키를 삭제합니다.
     * @param key Redis 키
     * @return 삭제 성공 여부
     */
    fun delete(key: String): Boolean {
        return stringRedisTemplate.delete(key)
    }

    /**
     * 여러 키를 삭제합니다.
     * @param keys Redis 키 목록
     * @return 삭제된 키의 개수
     */
    fun delete(vararg keys: String): Long {
        return stringRedisTemplate.delete(keys.toList()) ?: 0
    }

    // ========================================
    // Sorted Set (ZSet) Operations
    // ========================================

    /**
     * Sorted Set에 값을 추가합니다.
     * @param key Redis 키
     * @param value 추가할 값
     * @param score 정렬 기준 점수
     * @return 추가 성공 여부 (이미 존재하면 false)
     */
    fun zAdd(key: String, value: String, score: Double): Boolean {
        return stringRedisTemplate.opsForZSet().add(key, value, score) ?: false
    }

    /**
     * Sorted Set에서 특정 값의 순위를 조회합니다 (오름차순, 0부터 시작).
     * @param key Redis 키
     * @param value 조회할 값
     * @return 순위 (없으면 null)
     */
    fun zRank(key: String, value: String): Long? {
        return stringRedisTemplate.opsForZSet().rank(key, value)
    }

    /**
     * Sorted Set에서 특정 값의 순위를 조회합니다 (내림차순, 0부터 시작).
     * @param key Redis 키
     * @param value 조회할 값
     * @return 역순위 (없으면 null)
     */
    fun zRevRank(key: String, value: String): Long? {
        return stringRedisTemplate.opsForZSet().reverseRank(key, value)
    }

    /**
     * Sorted Set의 크기를 조회합니다.
     * @param key Redis 키
     * @return Sorted Set의 크기
     */
    fun zCard(key: String): Long {
        return stringRedisTemplate.opsForZSet().size(key) ?: 0
    }

    /**
     * Sorted Set에서 특정 값의 점수를 조회합니다.
     * @param key Redis 키
     * @param value 조회할 값
     * @return 점수 (없으면 null)
     */
    fun zScore(key: String, value: String): Double? {
        return stringRedisTemplate.opsForZSet().score(key, value)
    }

    /**
     * Sorted Set에서 특정 값을 제거합니다.
     * @param key Redis 키
     * @param values 제거할 값들
     * @return 제거된 요소의 개수
     */
    fun zRem(key: String, vararg values: String): Long {
        return stringRedisTemplate.opsForZSet().remove(key, *values) ?: 0
    }

    /**
     * Sorted Set에서 인덱스 범위로 값을 조회합니다 (오름차순).
     * @param key Redis 키
     * @param start 시작 인덱스 (0부터 시작)
     * @param end 종료 인덱스 (-1은 마지막까지)
     * @return 조회된 값들
     */
    fun zRange(key: String, start: Long, end: Long): Set<String> {
        return stringRedisTemplate.opsForZSet().range(key, start, end) ?: emptySet()
    }

    /**
     * Sorted Set에서 인덱스 범위로 값을 조회합니다 (내림차순).
     * @param key Redis 키
     * @param start 시작 인덱스 (0부터 시작)
     * @param end 종료 인덱스 (-1은 마지막까지)
     * @return 조회된 값들
     */
    fun zRevRange(key: String, start: Long, end: Long): Set<String> {
        return stringRedisTemplate.opsForZSet().reverseRange(key, start, end) ?: emptySet()
    }

    /**
     * Sorted Set에서 점수 범위로 값을 조회합니다.
     * @param key Redis 키
     * @param minScore 최소 점수
     * @param maxScore 최대 점수
     * @return 조회된 값들
     */
    fun zRangeByScore(key: String, minScore: Double, maxScore: Double): Set<String> {
        return stringRedisTemplate.opsForZSet().rangeByScore(key, minScore, maxScore) ?: emptySet()
    }

    /**
     * Sorted Set에서 점수 범위에 해당하는 요소를 제거합니다.
     * @param key Redis 키
     * @param minScore 최소 점수
     * @param maxScore 최대 점수
     * @return 제거된 요소의 개수
     */
    fun zRemRangeByScore(key: String, minScore: Double, maxScore: Double): Long {
        return stringRedisTemplate.opsForZSet().removeRangeByScore(key, minScore, maxScore) ?: 0
    }

    // ========================================
    // Hash Operations
    // ========================================

    /**
     * Hash에 필드-값 쌍을 저장합니다.
     * @param key Redis 키
     * @param hashKey Hash 필드
     * @param value 저장할 값
     */
    fun hSet(key: String, hashKey: String, value: String) {
        stringRedisTemplate.opsForHash<String, String>().put(key, hashKey, value)
    }

    /**
     * Hash에 여러 필드-값 쌍을 한번에 저장합니다.
     * @param key Redis 키
     * @param map 저장할 필드-값 맵
     */
    fun hSetAll(key: String, map: Map<String, String>) {
        stringRedisTemplate.opsForHash<String, String>().putAll(key, map)
    }

    /**
     * Hash에서 특정 필드의 값을 조회합니다.
     * @param key Redis 키
     * @param hashKey Hash 필드
     * @return 조회된 값 (필드가 없으면 null)
     */
    fun hGet(key: String, hashKey: String): String? {
        return stringRedisTemplate.opsForHash<String, String>().get(key, hashKey)
    }

    /**
     * Hash의 모든 필드-값 쌍을 조회합니다.
     * @param key Redis 키
     * @return 모든 필드-값 맵
     */
    fun hGetAll(key: String): Map<String, String> {
        return stringRedisTemplate.opsForHash<String, String>().entries(key)
    }

    /**
     * Hash에서 특정 필드를 삭제합니다.
     * @param key Redis 키
     * @param hashKeys 삭제할 Hash 필드들
     * @return 삭제된 필드의 개수
     */
    fun hDel(key: String, vararg hashKeys: String): Long {
        return stringRedisTemplate.opsForHash<String, String>().delete(key, *hashKeys)
    }

    /**
     * Hash에 특정 필드가 존재하는지 확인합니다.
     * @param key Redis 키
     * @param hashKey Hash 필드
     * @return 존재 여부
     */
    fun hExists(key: String, hashKey: String): Boolean {
        return stringRedisTemplate.opsForHash<String, String>().hasKey(key, hashKey)
    }

    /**
     * Hash의 필드 개수를 조회합니다.
     * @param key Redis 키
     * @return 필드 개수
     */
    fun hSize(key: String): Long {
        return stringRedisTemplate.opsForHash<String, String>().size(key)
    }

    // ========================================
    // Script Operations
    // ========================================

    /**
     * Lua 스크립트를 실행합니다 (String 결과).
     * @param script Redis 스크립트
     * @param keys KEYS 배열에 전달할 키 목록
     * @param args ARGV 배열에 전달할 인자 목록
     * @return 스크립트 실행 결과
     */
    fun executeScript(
        script: RedisScript<String>,
        keys: List<String>,
        vararg args: String,
    ): String? {
        return stringRedisTemplate.execute(script, keys, *args)
    }

    /**
     * Lua 스크립트를 실행합니다 (Long 결과).
     * @param script Redis 스크립트
     * @param keys KEYS 배열에 전달할 키 목록
     * @param args ARGV 배열에 전달할 인자 목록
     * @return 스크립트 실행 결과
     */
    fun executeScriptForLong(
        script: RedisScript<Long>,
        keys: List<String>,
        vararg args: String,
    ): Long? {
        return stringRedisTemplate.execute(script, keys, *args)
    }

    /**
     * Lua 스크립트를 실행합니다 (List 결과).
     * @param script Redis 스크립트
     * @param keys KEYS 배열에 전달할 키 목록
     * @param args ARGV 배열에 전달할 인자 목록
     * @return 스크립트 실행 결과
     */
    fun executeScriptForList(
        script: RedisScript<List<*>>,
        keys: List<String>,
        vararg args: String,
    ): List<*>? {
        return stringRedisTemplate.execute(script, keys, *args)
    }

    // ========================================
    // Key Operations
    // ========================================

    /**
     * 키가 존재하는지 확인합니다.
     * @param key Redis 키
     * @return 존재 여부
     */
    fun exists(key: String): Boolean {
        return stringRedisTemplate.hasKey(key)
    }

    /**
     * 키에 TTL을 설정합니다.
     * @param key Redis 키
     * @param duration 만료 시간
     * @return 설정 성공 여부
     */
    fun expire(key: String, duration: Duration): Boolean {
        return stringRedisTemplate.expire(key, duration) ?: false
    }

    /**
     * 키의 TTL을 조회합니다.
     * @param key Redis 키
     * @return TTL (초 단위, -1은 무제한, -2는 키가 없음)
     */
    fun ttl(key: String): Long {
        return stringRedisTemplate.getExpire(key) ?: -2
    }
}
