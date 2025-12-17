# 🎤 콘서트 예약 서비스
> 대기열 시스템 + 좌석 임시 배정(5분) + 포인트 기반 결제를 통한 콘서트 예약 서비스

## 📚 문서
- [API 명세서](./docs/openapi.yml)
- [ERD](./docs/erd.md)
- [요구사항 정의서](./docs/summary.md)

## 커밋
- f58c29f feat: 엔티티 모델링
- a024543 feat: 공통 예외 처리 구조 구현
- 1915aad feat: 콘서트 예약 시스템 API 구현
- a9095ef feat: UseCase에 대한 단위 테스트
- 8c29e00 feat: 대기열 관리 시스템 구현
- cd57e33 feat: ktlint 적용
- 9a81ea9 feat: 대기열 케이스 테스트 코드 추가
- 6e5f71d refactor: 도메인, 순수 POJO 객체 선언
- 26f7584 refactor: 클린 아키텍처로 재구성
- c8819cc refactor: 아키텍처 구성 리팩토링 V2
- 5bb5be4 fix: 도메인 레이어 의존성 개선 및 Facade 버그 수정

---

## PR 설명

### 🎯 작업 개요
콘서트 예약 서비스의 비즈니스 로직을 **클린 아키텍처** 기반으로 재구성하여 안정성과 유지보수성을 확보했습니다.
> 예약/결제에만 클린 아키텍처를 구성하는 것이 더 어려울 것이라고 판단, 전체적인 클린 아키텍처 구성

---

### 1️⃣ 클린 아키텍처 도입

**4계층 레이어 분리**로 관심사를 명확히 분리하고 의존성 방향을 제어했습니다.

```
Presentation Layer (API)
    ↓ 의존
Application Layer (Facade)
    ↓ 의존
Domain Layer (순수 비즈니스 로직)
    ↑ 구현
Infrastructure Layer (영속성)
```

**핵심 원칙:**
- **의존성 역전 원칙(DIP)**: Domain이 Infrastructure에 의존하지 않도록 Repository Interface 분리
- **단일 책임 원칙(SRP)**: 각 계층은 하나의 책임만 가짐
- **도메인 모델 순수성**: JPA 애노테이션 제거, 비즈니스 로직에만 집중

---

### 2️⃣ 도메인 레이어 구현

**순수 POJO 도메인 모델:**
```kotlin
class Concert private constructor(
    var id: Long,
    val title: String,
    val description: String?,
    val createdAt: LocalDateTime,
    var updatedAt: LocalDateTime,
) {
    companion object {
        fun create(title: String, description: String?): Concert { ... }
        fun reconstitute(id: Long, ...): Concert { ... }
    }
}
```

**구현된 도메인:**
- **Concert**: Concert, ConcertSchedule, Seat
- **Reservation**: Reservation (상태 관리 및 검증 로직)
- **Payment**: Payment (결제 상태 관리)
- **Point**: Point, PointHistory (포인트 충전/사용)
- **Queue**: QueueToken (대기열 토큰 상태 전환)
- **User**: User (사용자 정보)

---

### 3️⃣ Application Facade 패턴

**UseCase 복잡도 관리를 위한 Facade 레이어:**

여러 Domain Service를 조합하여 비즈니스 플로우를 구현하고 트랜잭션 경계를 명확히 했습니다.

**구현된 Facade:**
- `ConcertScheduleFacade`: 콘서트 일정 및 좌석 조회
- `ReservationFacade`: 좌석 예약 및 임시 배정
- `PaymentFacade`: 결제 처리 및 좌석 확정
- `PointFacade`: 포인트 조회/충전
- `QueueFacade`: 대기열 토큰 발급 및 상태 조회

---

### 4️⃣ Infrastructure 레이어 분리

**JPA Entity와 도메인 모델 완전 분리:**
```kotlin
// Infrastructure Layer
@Entity
@Table(name = "concerts")
class ConcertEntity(...) : BaseEntity() {
    fun toDomain(): Concert = Concert.reconstitute(...)
    companion object {
        fun fromDomain(concert: Concert): ConcertEntity = ...
    }
}

// Repository 구현체
@Repository
class ConcertRepositoryImpl(
    private val concertJpaRepository: ConcertJpaRepository
) : ConcertRepository {
    override fun findById(id: Long): Concert? {
        return concertJpaRepository.findById(id)
            .map { it.toDomain() }
            .orElse(null)
    }
}
```

---

### 5️⃣ API 레이어 구현

**RESTful API 엔드포인트 (8개):**
- `POST /api/v1/queue/token` - 대기열 토큰 발급
- `GET /api/v1/queue/status` - 대기열 상태 조회
- `GET /api/v1/concerts/{concertId}/schedules` - 예약 가능 일정 조회
- `GET /api/v1/concerts/{concertId}/schedules/{scheduleId}/seats` - 예약 가능 좌석 조회
- `POST /api/v1/reservations` - 좌석 예약
- `POST /api/v1/payments` - 결제 처리
- `GET /api/v1/points` - 포인트 조회
- `POST /api/v1/points/charge` - 포인트 충전

**대기열 인터셉터:**
- ACTIVE 상태의 토큰만 API 접근 허용
- 자동 토큰 검증 및 갱신

---

### 6️⃣ 예외 처리 구조

**계층화된 커스텀 예외 및 GlobalExceptionHandler:**
```kotlin
sealed class BusinessException(
    val errorCode: ErrorCode,
    override val message: String
) : RuntimeException(message)

class NotFoundException(errorCode: ErrorCode) : BusinessException(...)
class AuthenticationException(errorCode: ErrorCode) : BusinessException(...)
class AuthorizationException(errorCode: ErrorCode) : BusinessException(...)
```

**통일된 에러 응답:**
```json
{
  "code": "CONCERT_NOT_FOUND",
  "message": "콘서트를 찾을 수 없습니다",
  "timestamp": "2025-11-18T10:30:00"
}
```

---

### 7️⃣ 대기열 시스템

**QueueScheduler (자동화된 토큰 관리):**
```kotlin
@Scheduled(fixedRate = 10000) // 10초마다
fun activateWaitingTokens() {
    queueTokenService.activateWaitingTokens(ACTIVE_USER_LIMIT)
}

@Scheduled(fixedRate = 60000) // 1분마다
fun expireTokens() {
    queueTokenService.expireInactiveTokens()
}
```

**토큰 상태 전환:** WAITING → ACTIVE → EXPIRED

---

## 리뷰 포인트

### 💡 고민했던 부분

1. **도메인 모델 설계**
    - JPA Entity를 Domain Model로 사용할지, 완전히 분리할지 고민
    - 결론: 완전 분리로 도메인 로직을 프레임워크로부터 독립시킴

2. **Facade 레이어 도입**

    - Controller에서 직접 Service를 호출할지, Facade를 둘지 고민
    - 결론: Facade 패턴으로 복잡한 UseCase를 효과적으로 관리

4. **Repository 추상화**
    - Domain Layer에 Repository Interface를 두는 것이 적절한지 고민
    - 결론: DIP 원칙에 따라 Domain이 추상화를 정의하고 Infrastructure가 구현

5. **도메인 객체 전달**
    - Service 메서드에 ID(Long)를 전달할지, 도메인 객체를 전달할지 고민
    - 결론: 도메인 객체 전달로 타입 안전성과 의미 명확성 확보

### 🤔 리뷰어께 여쭤보고 싶은 점
- 현재 Facade(= UseCase?) 레이어의 책임 범위가 적절한가요?
- Domain Service와 Facade의 역할 구분이 명확한가요?
- 도메인 객체를 파라미터로 전달하는 방식에 대한 의견이 궁금합니다.

---

## Definition of Done (DoD)
- [x] 대기열 토큰 발급
- [x] 콘서트 조회 및 좌석 선택
- [x] 좌석 임시 배정 (5분)
- [x] 포인트 충전 및 잔액 조회
- [x] 결제 처리

## ✅ 추가 완료 사항
- [x] 클린 아키텍처 4계층 구조 구현
- [x] 도메인 모델 순수 POJO로 분리
- [x] Facade 패턴으로 UseCase 복잡도 관리
- [x] Repository Interface/구현체 분리
- [x] 대기열 시스템 자동화 (QueueScheduler)
- [x] 통합 예외 처리 구조
- [x] 단위 테스트 작성 (30개, 모두 통과)
- [x] ktlint 코드 스타일 적용
- [x] 동시성 제어 (비관적 락)
> Redis 도입 이전이라 해당 방법을 채택, 도입 후에 분산락 적용 예정 ❗️
- [x] 타입 안전성 개선

---

## 🛠️ 기술 스택
- **Language**: Kotlin 2.1+
- **Framework**: Spring Boot 3.x
- **JVM**: Java 17
- **Database**: MySQL 8.0
- **Cache/Queue**: Redis (Redisson)
- **Authentication**: JWT
- **Testing**: JUnit5, MockK, Testcontainers
- **Code Quality**: ktlint
- **Documentation**: OpenAPI 3.0.3

---

## 🏗️ 최종 아키텍처

```
┌─────────────────────────────────────────────────────────┐
│         Presentation Layer (API)                        │
│  - Controllers (엔드포인트 정의)                         │
│  - DTOs (Request/Response)                              │
│  - GlobalExceptionHandler                               │
│  - Interceptors (QueueTokenInterceptor)                 │
└────────────────────┬────────────────────────────────────┘
                     │ 호출
┌────────────────────▼────────────────────────────────────┐
│         Application Layer (Facade)                      │
│  - Facade (여러 도메인 서비스 조합)                      │
│  - 트랜잭션 경계 (@Transactional)                        │
│  - Scheduler (QueueScheduler)                           │
└────────────────────┬────────────────────────────────────┘
                     │ 호출
┌────────────────────▼────────────────────────────────────┐
│              Domain Layer                               │
│  - Models (순수 POJO, 비즈니스 로직)                    │
│  - Services (도메인 서비스)                              │
│  - Repository Interfaces (추상화)                       │
└────────────────────△────────────────────────────────────┘
                     │ 구현
┌────────────────────┴────────────────────────────────────┐
│         Infrastructure Layer                            │
│  - Entities (JPA 매핑)                                  │
│  - RepositoryImpl (Repository 구현체)                   │
│  - JpaRepository (Spring Data JPA)                      │
└─────────────────────────────────────────────────────────┘
```

---

## 🎯 개선 효과

### Before (기존 구조)
❌ Controller가 직접 여러 Service를 호출하여 복잡도 증가
❌ Entity가 도메인 로직과 영속성 관심사를 모두 포함
❌ 도메인 로직이 JPA에 종속
❌ 테스트 시 DB 의존성 필요
❌ ID(Long)를 파라미터로 전달하여 타입 안전성 부족

### After (개선된 구조)
✅ Facade가 UseCase를 조율, Controller는 얇게 유지
✅ 도메인 모델과 Entity 분리로 관심사 명확화
✅ 도메인 로직이 프레임워크로부터 독립적
✅ Mock을 활용한 빠른 단위 테스트 가능
✅ 도메인 객체를 파라미터로 전달하여 타입 안전성 확보
✅ 의존성 방향이 항상 외부 → 내부(Domain)

---
