# 🎤 콘서트 예약 서비스

> 대기열 시스템 + 좌석 임시 배정(5분) + 포인트 기반 결제를 통한 콘서트 예약 서비스

## 📚 문서
- [API 명세서](./docs/openapi.yml)
- [ERD](./docs/erd.md)
- [요구사항 정의서](./docs/summary.md)

## 🎯 목표 시나리오

### 1. 대기열 토큰 발급
- 사용자는 서비스 이용을 위해 대기열에 진입하여 UUID 토큰을 발급받습니다.
- 토큰 상태: **WAITING** (대기 중) → **ACTIVE** (활성화) → **EXPIRED** (만료)
- 대기 순서(queue_position)와 예상 대기 시간을 제공합니다.

### 2. 콘서트 조회 및 좌석 선택
- **ACTIVE** 상태의 토큰을 가진 사용자만 콘서트 일정과 좌석을 조회할 수 있습니다.
- 예약 가능한 날짜 목록을 조회합니다.
- 특정 날짜의 예약 가능한 좌석(1-50번)을 조회합니다.
- 좌석 상태:
    - `AVAILABLE`: 예약 가능
    - `TEMPORARILY_RESERVED`: 임시 배정 (5분간 유효)
    - `RESERVED`: 예약 완료

### 3. 좌석 임시 배정 (5분)
- 사용자가 좌석을 선택하면 해당 좌석이 **5분간 임시 배정**됩니다.
- 임시 배정 중에는 다른 사용자가 해당 좌석을 선택할 수 없습니다.
- 5분 내에 결제를 완료하지 않으면:
    - 예약 상태가 `EXPIRED`로 변경됩니다.
    - 좌석 상태가 `AVAILABLE`로 복원되어 다른 사용자가 예약 가능합니다.

### 4. 포인트 충전 및 잔액 조회
- 사용자는 결제를 위해 사전에 포인트를 충전합니다.
- 사용자 식별자(userId)로 현재 포인트 잔액을 조회할 수 있습니다.
- 포인트 충전 내역은 `POINT_HISTORY`에 기록됩니다 (transaction_type: CHARGE).

### 5. 결제 처리
- **ACTIVE** 토큰을 가진 사용자가 임시 배정된 좌석에 대해 결제를 진행합니다.
- 포인트 잔액이 충분한지 확인합니다.
- 결제 완료 시:
    - 포인트가 차감되고 사용 내역이 기록됩니다 (transaction_type: USE).
    - 예약 상태가 `CONFIRMED`로 변경됩니다.
    - 좌석 상태가 `RESERVED`로 변경됩니다.
    - 대기열 토큰이 `EXPIRED`로 변경됩니다.
- 결제 실패 시:
    - 결제 상태가 `FAILED`로 기록됩니다.
    - 임시 배정이 유지되며, 5분 내에 재시도할 수 있습니다.

## 🏗️ 주요 비즈니스 로직

### 동시성 제어
- 좌석 예약 시 비관적 락(Pessimistic Lock) 또는 낙관적 락(Optimistic Lock) 사용
- 포인트 차감 시 트랜잭션 격리 수준 관리
- 다중 인스턴스 환경에서 안전한 동시성 처리

### 대기열 관리
- 특정 시간 동안 N명에게만 ACTIVE 권한 부여
- 활성화된 최대 유저 수를 N으로 유지
- 순서대로 정확한 대기열 제공 (queue_position)

### 임시 배정 자동 만료
- 5분 내 결제 미완료 시 자동으로 예약 만료 처리
- 좌석 상태를 AVAILABLE로 복원하여 재판매 가능

## 🛠️ 기술 스택
- **Language**: Kotlin 2.1+
- **Framework**: Spring Boot 3.x
- **JVM**: Java 17
- **Database**: MySQL 8.0
- **Cache/Queue**: Redis (Redisson)
- **Authentication**: JWT
- **Testing**: JUnit5 + Testcontainers
- **Documentation**: OpenAPI 3.0.3