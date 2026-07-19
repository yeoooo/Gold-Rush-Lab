# ⛏️ Gold Rush Lab

> 여러 사용자가 하나의 금광을 동시에 채굴하는 상황을 구현하며,
> 데이터베이스 동시성 제어를 단계적으로 학습하고 검증하는 프로젝트입니다.

---

## Why Gold Rush Lab?

동시성은 대부분의 백엔드 서비스에서 반드시 고려해야 하는 문제입니다.

Gold Rush Lab은 **여러 사용자가 하나의 자원을 동시에 수정하는 상황**을 금광 채굴이라는 도메인으로 단순화하여 다음과 같은 질문을 검증합니다.

- 동시에 채굴하면 어떤 문제가 발생하는가?
- 데이터는 어떻게 깨지는가?
- 데이터베이스는 이를 어떻게 해결하는가?
- 환경과 요구사항에 따라 어떤 동시성 제어 방식이 적합한가?

프로젝트는 버전별로 기능을 확장하며 동시성 문제와 해결 방식을 직접 구현하고 비교합니다.

---

## Tech Stack

### Backend

- Java 21
- Spring Boot 4.1.0
- Spring Web
- Spring Data JPA
- Hibernate
- Lombok
- Gradle 9.5.1

### Database

- PostgreSQL 16

### Infrastructure

- Docker
- Docker Compose

### Test

- JUnit 5
- Spring Boot Test

---

## Architecture
<details>
<summary>v0.1</summary>  

</details>

---

## Domain Model

```text
MineEntity
 ├── id: Long
 ├── remainingAmount: Long
 └── createdAt: LocalDateTime
        |
        +-- 1:N -- UserEntity
        └── 1:N -- MiningLogEntity

UserEntity
 ├── id: Long
 ├── mine: MineEntity
 ├── totalMinedGold: Long
 ├── sessionId: UUID
 └── createdAt: LocalDateTime
        |
        └── 1:N -- MiningLogEntity

MiningLogEntity
 ├── id: Long
 ├── user: UserEntity
 ├── mine: MineEntity
 ├── amount: Long
 └── createdAt: LocalDateTime
```

## Configuration

애플리케이션은 `app/src/main/resources/application.yml`에서 다음 환경 변수를 참조합니다.

| Environment Variable | Description |
| --- | --- |
| `POSTGRES_USERNAME` | 애플리케이션에서 사용할 PostgreSQL 사용자명 |
| `POSTGRES_PASSWORD` | PostgreSQL 비밀번호 |
| `POSTGRES_DB` | PostgreSQL 데이터베이스 이름 |

- 애플리케이션용 Compose 파일: `app/compose.yaml`
- 독립 PostgreSQL 인프라 Compose 파일: `infra/db/compose.yml`
- 초기 DB 스키마: `infra/db/init/001-schema.sql`
- 애플리케이션 포트: `8080`

> 현재 두 Compose 설정의 환경 변수 이름과 애플리케이션 datasource 설정은 완전히 통일되지 않은 상태입니다.

---

## Roadmap

### v0.1 — Baseline

아무런 동시성 제어 없이 기본 채굴 시스템을 구현합니다.

- [x] Spring Boot, PostgreSQL, VM 환경 구성
- [x] 기반 코드 작성
- [x] 기본 채굴 API
- [x] Integration Test
- [ ] 동시성 정합성 테스트
- [ ] 부하 테스트
- [ ] 핫스팟 스트레스 테스트
- [ ] Lost Update 분석
- [ ] TPS / 응답 시간 측정

### v0.2 — Database Lock

DB Lock을 이용하여 동시성 문제를 해결합니다.

- [ ] Optimistic Lock
- [ ] Pessimistic Lock
- [ ] 동일한 벤치마크 수행
- [ ] Lost Update 제거 확인
- [ ] v0.1과 성능 비교
- [ ] Lock 충돌률 분석

### v0.3 — Scale-out

API 서버를 여러 대로 확장합니다.

- [ ] Multiple API Instance
- [ ] Load Balancer
- [ ] 동일한 벤치마크 수행
- [ ] 처리량(TPS) 비교
- [ ] DB Lock의 확장성 분석

### v0.4 — Distributed Lock

Redis 기반 분산 락을 적용합니다.

- [ ] Redis
- [ ] Distributed Lock
- [ ] 동일한 벤치마크 수행
- [ ] DB Lock 대비 성능 비교
- [ ] 락 대기 시간 분석

### v0.5 — Event Driven

Kafka 기반의 비동기 처리 구조로 확장합니다.

- [ ] Kafka
- [ ] Event-Driven Architecture
- [ ] Consumer
- [ ] 동일한 벤치마크 수행
- [ ] 처리량 및 지연 시간 비교
- [ ] 최종 성능 분석

---

## 벤치마크 시나리오

모든 버전은 동일한 테스트 시나리오를 기준으로 정합성과 성능을 비교합니다.

### 핫스팟 스트레스 테스트

모든 요청을 하나의 금광으로 집중시켜 시스템의 처리 한계와 락 경합을 측정합니다.

#### 테스트 조건

- 동시 사용자: 단계적으로 증가
- 대상: 동일한 금광
- 시스템 포화 시점까지 수행

#### 측정 지표

- 최대 처리량(TPS)
- 평균 응답 시간
- P95 / P99 지연 시간
- 락 대기 시간
- 타임아웃 발생 횟수
- 데드락 발생 여부
---

## Project Structure

```text
Gold-Rush-Lab
├── app
│   ├── build.gradle
│   ├── compose.yaml
│   └── src
│       ├── main
│       │   ├── java/io/devyeoooo/Gold_Rush_Lab
│       │   │   ├── comm
│       │   │   │   └── BaseEntity.java
│       │   │   ├── mine/repository
│       │   │   │   ├── entity/MineEntity.java
│       │   │   │   ├── MineRepository.java
│       │   │   │   ├── MineJpaRepository.java
│       │   │   │   └── MineJpaAdapter.java
│       │   │   ├── user/repository
│       │   │   │   ├── entity/UserEntity.java
│       │   │   │   ├── UserRepository.java
│       │   │   │   ├── UserJpaRepository.java
│       │   │   │   └── UserJpaAdapter.java
│       │   │   ├── mining_log/repository
│       │   │   │   ├── entity/MiningLogEntity.java
│       │   │   │   ├── MiningLogRepository.java
│       │   │   │   ├── MiningLogJpaRepository.java
│       │   │   │   └── MiningLogJpaAdapter.java
│       │   │   └── GoldRushLabApplication.java
│       │   └── resources/application.yml
│       └── test/java/io/devyeoooo/Gold_Rush_Lab
│           └── GoldRushLabApplicationTests.java
└── infra
    └── db
        ├── compose.yml
        └── init/001-schema.sql
```
