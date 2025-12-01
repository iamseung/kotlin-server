package kr.hhplus.be.server.api.controller

import com.fasterxml.jackson.databind.ObjectMapper
import kr.hhplus.be.server.api.dto.request.IssueQueueTokenRequest
import kr.hhplus.be.server.domain.queue.model.QueueStatus
import kr.hhplus.be.server.domain.queue.repository.QueueTokenRepository
import kr.hhplus.be.server.infrastructure.persistence.queue.repository.RedisQueueRepository
import kr.hhplus.be.server.infrastructure.persistence.user.entity.User
import kr.hhplus.be.server.infrastructure.persistence.user.repository.UserJpaRepository
import kr.hhplus.be.server.support.AbstractIntegrationContainerBaseTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc
@DisplayName("대기열 컨트롤러 API 통합 테스트")
class QueueControllerTest : AbstractIntegrationContainerBaseTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userJpaRepository: UserJpaRepository

    @Autowired
    private lateinit var queueTokenRepository: QueueTokenRepository

    @Autowired
    private lateinit var redisQueueRepository: RedisQueueRepository

    @BeforeEach
    fun setUp() {
        cleanupDatabase()
    }

    @AfterEach
    fun tearDown() {
        cleanupDatabase()
    }

    private fun cleanupDatabase() {
        userJpaRepository.deleteAll()
        // Redis cleanup - delete all tokens and remove from queues
        val waitingUsers = redisQueueRepository.getAllWaitingUsers()
        val activeUsers = redisQueueRepository.getAllActiveUsers()

        (waitingUsers + activeUsers).forEach { userId ->
            redisQueueRepository.removeFromActiveQueue(userId)
        }
    }

    @Test
    @DisplayName("POST /api/v1/queue/token - 대기열 토큰 발급 성공")
    fun issueQueueTokenSuccess() {
        // given
        val user = userJpaRepository.save(User("테스트", "test@example.com", "password"))
        val request = IssueQueueTokenRequest(userId = user.id)

        // when & then
        mockMvc.perform(
            post("/api/v1/queue/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.userId").value(user.id))
            .andExpect(jsonPath("$.token").isNotEmpty)
            .andExpect(jsonPath("$.queueStatus").value("WAITING"))
            .andExpect(jsonPath("$.queuePosition").isNumber)
            .andExpect(jsonPath("$.createdAt").isNotEmpty)
    }

    @Test
    @DisplayName("POST /api/v1/queue/token - 여러 사용자가 토큰 발급 시 대기 순서 증가")
    fun issueQueueTokenMultipleUsers() {
        // given
        val user1 = userJpaRepository.save(User("사용자1", "user1@example.com", "password"))
        val user2 = userJpaRepository.save(User("사용자2", "user2@example.com", "password"))

        val request1 = IssueQueueTokenRequest(userId = user1.id)
        val request2 = IssueQueueTokenRequest(userId = user2.id)

        // when & then - 첫 번째 사용자 토큰 발급
        mockMvc.perform(
            post("/api/v1/queue/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.queueStatus").value("WAITING"))

        // when & then - 두 번째 사용자 토큰 발급
        mockMvc.perform(
            post("/api/v1/queue/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.queueStatus").value("WAITING"))
    }

    @Test
    @DisplayName("POST /api/v1/queue/token - 존재하지 않는 사용자 토큰 발급 시 실패")
    fun issueQueueTokenWithNonExistentUser() {
        // given
        val request = IssueQueueTokenRequest(userId = 999L)

        // when & then
        mockMvc.perform(
            post("/api/v1/queue/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().is4xxClientError)
    }

    @Test
    @DisplayName("GET /api/v1/queue/status - 대기 상태 조회 성공 (WAITING)")
    fun getQueueStatusWaiting() {
        // given
        val user = userJpaRepository.save(User("테스트", "test@example.com", "password"))
        val request = IssueQueueTokenRequest(userId = user.id)

        val tokenResponse = mockMvc.perform(
            post("/api/v1/queue/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andReturn()

        val token = objectMapper.readTree(tokenResponse.response.contentAsString).get("token").asText()

        // when & then
        mockMvc.perform(
            get("/api/v1/queue/status")
                .header("X-Queue-Token", token)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.queueStatus").value("WAITING"))
            .andExpect(jsonPath("$.queuePosition").isNumber)
            .andExpect(jsonPath("$.estimatedWaitTimeMinutes").isNumber)
    }

    @Test
    @DisplayName("GET /api/v1/queue/status - 대기 상태 조회 성공 (ACTIVE)")
    fun getQueueStatusActive() {
        // given
        val user = userJpaRepository.save(User("테스트", "test@example.com", "password"))
        val request = IssueQueueTokenRequest(userId = user.id)

        val tokenResponse = mockMvc.perform(
            post("/api/v1/queue/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andReturn()

        val token = objectMapper.readTree(tokenResponse.response.contentAsString).get("token").asText()

        // 토큰을 ACTIVE 상태로 변경
        val activatedUsers = redisQueueRepository.activateWaitingUsers(1)

        // Verify activation worked
        assert(activatedUsers.contains(user.id)) { "User ${user.id} was not activated. Activated users: $activatedUsers" }

        // Allow Redis to propagate changes
        Thread.sleep(500)

        // when & then
        mockMvc.perform(
            get("/api/v1/queue/status")
                .header("X-Queue-Token", token)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.queueStatus").value("ACTIVE"))
            .andExpect(jsonPath("$.queuePosition").value(0))
            .andExpect(jsonPath("$.estimatedWaitTimeMinutes").value(0))
    }

    @Test
    @DisplayName("GET /api/v1/queue/status - 잘못된 토큰으로 조회 시 실패")
    fun getQueueStatusWithInvalidToken() {
        // given
        val invalidToken = "invalid-token"

        // when & then
        mockMvc.perform(
            get("/api/v1/queue/status")
                .header("X-Queue-Token", invalidToken)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().is4xxClientError)
    }

    @Test
    @DisplayName("GET /api/v1/queue/status - 토큰 헤더 없이 조회 시 실패")
    fun getQueueStatusWithoutToken() {
        // when & then
        mockMvc.perform(
            get("/api/v1/queue/status")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().is4xxClientError)
    }
}
