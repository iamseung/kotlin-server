package kr.hhplus.be.server.api.controller

import com.fasterxml.jackson.databind.ObjectMapper
import kr.hhplus.be.server.api.dto.request.ChargePointRequest
import kr.hhplus.be.server.infrastructure.persistence.point.entity.Point
import kr.hhplus.be.server.infrastructure.persistence.point.repository.PointJpaRepository
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
@DisplayName("포인트 컨트롤러 API 통합 테스트")
class PointControllerTest : AbstractIntegrationContainerBaseTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userJpaRepository: UserJpaRepository

    @Autowired
    private lateinit var pointJpaRepository: PointJpaRepository

    @BeforeEach
    fun setUp() {
        cleanupDatabase()
    }

    @AfterEach
    fun tearDown() {
        cleanupDatabase()
    }

    private fun cleanupDatabase() {
        pointJpaRepository.deleteAll()
        userJpaRepository.deleteAll()
    }

    @Test
    @DisplayName("POST /api/v1/points/charge - 포인트 충전 성공")
    fun chargePointSuccess() {
        // given
        val user = userJpaRepository.save(User("테스트", "test@example.com", "password"))
        pointJpaRepository.save(Point(user.id, 0))
        val request = ChargePointRequest(userId = user.id, amount = 10000)

        // when & then
        mockMvc.perform(
            post("/api/v1/points/charge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(user.id))
            .andExpect(jsonPath("$.balance").value(10000))
    }

    @Test
    @DisplayName("POST /api/v1/points/charge - 여러 번 충전 시 누적")
    fun chargePointMultipleTimes() {
        // given
        val user = userJpaRepository.save(User("테스트", "test@example.com", "password"))
        pointJpaRepository.save(Point(user.id, 0))

        // when & then - 첫 번째 충전
        val request1 = ChargePointRequest(userId = user.id, amount = 10000)
        mockMvc.perform(
            post("/api/v1/points/charge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.balance").value(10000))

        // when & then - 두 번째 충전
        val request2 = ChargePointRequest(userId = user.id, amount = 5000)
        mockMvc.perform(
            post("/api/v1/points/charge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.balance").value(15000))
    }

    @Test
    @DisplayName("POST /api/v1/points/charge - 음수 금액 충전 시 실패")
    fun chargePointWithNegativeAmount() {
        // given
        val user = userJpaRepository.save(User("테스트", "test@example.com", "password"))
        val request = ChargePointRequest(userId = user.id, amount = -10000)

        // when & then
        mockMvc.perform(
            post("/api/v1/points/charge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().is4xxClientError)
    }

    @Test
    @DisplayName("POST /api/v1/points/charge - 존재하지 않는 사용자 충전 시 실패")
    fun chargePointWithNonExistentUser() {
        // given
        val request = ChargePointRequest(userId = 999L, amount = 10000)

        // when & then
        mockMvc.perform(
            post("/api/v1/points/charge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().is4xxClientError)
    }

    @Test
    @DisplayName("GET /api/v1/points - 포인트 조회 성공")
    fun getPointSuccess() {
        // given
        val user = userJpaRepository.save(User("테스트", "test@example.com", "password"))
        pointJpaRepository.save(Point(user.id, 0))
        val chargeRequest = ChargePointRequest(userId = user.id, amount = 10000)
        mockMvc.perform(
            post("/api/v1/points/charge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chargeRequest)),
        )

        // when & then
        mockMvc.perform(
            get("/api/v1/points")
                .param("userId", user.id.toString())
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(user.id))
            .andExpect(jsonPath("$.balance").value(10000))
    }

    @Test
    @DisplayName("GET /api/v1/points - 포인트 충전 이력이 없는 경우 0 반환")
    fun getPointWithNoChargeHistory() {
        // given
        val user = userJpaRepository.save(User("테스트", "test@example.com", "password"))
        pointJpaRepository.save(Point(user.id, 0))

        // when & then
        mockMvc.perform(
            get("/api/v1/points")
                .param("userId", user.id.toString())
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(user.id))
            .andExpect(jsonPath("$.balance").value(0))
    }

    @Test
    @DisplayName("GET /api/v1/points - 존재하지 않는 사용자 조회 시 실패")
    fun getPointWithNonExistentUser() {
        // given
        val nonExistentUserId = 999L

        // when & then
        mockMvc.perform(
            get("/api/v1/points")
                .param("userId", nonExistentUserId.toString())
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().is4xxClientError)
    }
}
