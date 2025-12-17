#[과제] Redis 기반의 구조 개선
## [필수] Ranking Design
- 콘서트 예약 시나리오
> (인기도) 빠른 매진 랭킹을 Redis 기반으로 개발하고 설계 및 구현

## [선택] Asynchronous Design
- 콘서트 예약 시나리오
> 대기열 기능에 대해 Redis 기반의 설계를 진행하고 적절하게 동작할 수 있도록 하여 제출
> (대기유저 / 활성유저) Set ? Sorted Set

> Redis가 현업에서 어떠 식으로 구현되고 안전하게 서비스 할 수 있는가? (자동복구) 에 대한 고민
> HAProxy

# Ranking Design
## (인기도) 빠른 매진 랭킹을 Redis 기반으로 개발하고 설계 및 구현
