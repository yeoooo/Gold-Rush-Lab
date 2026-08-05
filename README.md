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

## 실험 결과

**Check Point 버전: `v0.5` — Event Driven**

- 전체 결과 표 : [스프레드 시트](https://docs.google.com/spreadsheets/d/1JI9cNVAPddM1r-ZIN8ciUZeEWAEWvDo7p3XPs-Si1NQ/edit?usp=sharing)
- v0.1 결과 : [[Project : Gold-Rush-Lab] 1. 모놀리식에서의 동시성과 부하](https://yeoooo.github.io/project/gold-rush-lab-monolith-concurrency-load/)
- v0.2 결과 : [[Project : Gold-Rush-Lab] 2. 동시성 문제에서의 락](https://yeoooo.github.io/project/gold-rush-lab-database-lock/)
- v0.3 결과 : [[Project : Gold-Rush-Lab] 3. 분산 시스템에서의 Lock](https://yeoooo.github.io/project/gold-rush-lab-distributed-lock/)
- v0.4 결과 : [[Project : Gold-Rush-Lab] 4. 분산 시스템과 분산 락](https://yeoooo.github.io/project/gold-rush-lab-redis-distributed-lock/)
- v0.5 결과 : [[Project : Gold-Rush-Lab] 5. Event Driven 아키텍처와 동시성](https://yeoooo.github.io/project/gold-rush-lab-event-driven-concurrency/)
- Check Point : [[Project : Gold-Rush-Lab] 6. Check Point, 회고](https://yeoooo.github.io/project/gold-rush-lab-checkpoint/)
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
- Redis
- Kafka

### Database

- PostgreSQL 16

### Infrastructure

- Docker
- Docker Compose
- Prometheus
- Grafana

### Test

- JUnit 5
- Spring Boot Test
- k6

---

## Architecture

<details>
<summary>v0.1 ~ v0.2 실험 환경</summary>

![v0.1 실험 환경](https://github.com/yeoooo/yeoooo.github.io/blob/master/assets/images/gold-rush-lab/v0-1/environment.png?raw=true)
- Host Machine
    - OS: Windows 11
    - CPU: AMD Ryzen 5 5600X
    - Memory: 32 GB
- Virtualization
    - Hyper-V
- Network
    - External Virtual Switch, 모든 VM은 동일한 Hyper-V Virtual Switch를 사용

---

- APP VM-01
    - OS: Ubuntu Server 24.04
    - vCPU: 2
    - Memory: 2 GB
- DB VM
    - OS: Ubuntu Server 24.04
    - vCPU: 2
    - Memory: 2 GB
- Monitoring VM
    - OS: Ubuntu Server 24.04
    - vCPU: 2
    - Memory: 2 GB

---

- Load Generator
    - OS : Mac OS Tahoe(26)
    - CPU: M1
    - Memory: 8GB
</details>

<details>
<summary>v0.3 실험 환경</summary>

![v0.3 실험 환경](https://github.com/yeoooo/yeoooo.github.io/blob/master/assets/images/gold-rush-lab/v0-3/environment.png?raw=true)
- Host Machine
    - OS: Windows 11
    - CPU: AMD Ryzen 5 5600X
    - Memory: 32 GB
- Virtualization
    - Hyper-V
- Network
    - External Virtual Switch, 모든 VM은 동일한 Hyper-V Virtual Switch를 사용

---

- APP VM-01, APP VM-02
    - OS: Ubuntu Server 24.04
    - vCPU: 2
    - Memory: 2 GB
- DB VM
    - OS: Ubuntu Server 24.04
    - vCPU: 2
    - Memory: 2 GB
- LB(LoadBalancer) VM
    - OS: Ubuntu Server 24.04
    - vCPU: 2
    - Memory: 2 GB
- Monitoring VM
    - OS: Ubuntu Server 24.04
    - vCPU: 2
    - Memory: 2 GB

---

- Load Generator
    - OS : Mac OS Tahoe(26)
    - CPU: M1
    - Memory: 8GB
</details>

<details>
<summary>v0.4 실험 환경</summary>

![v0.4 실험 환경](https://github.com/yeoooo/yeoooo.github.io/blob/master/assets/images/gold-rush-lab/v0-4/environment.png?raw=true)

- Host Machine
    - OS: Windows 11
    - CPU: AMD Ryzen 5 5600X
    - Memory: 32 GB
- Virtualization
    - Hyper-V
- Network
    - External Virtual Switch, 모든 VM은 동일한 Hyper-V Virtual Switch를 사용

---

- APP VM-01, APP VM-02
    - OS: Ubuntu Server 24.04
    - vCPU: 2
    - Memory: 2 GB
- DB VM
    - OS: Ubuntu Server 24.04
    - vCPU: 2
    - Memory: 2 GB
- LB(LoadBalancer) VM
    - OS: Ubuntu Server 24.04
    - vCPU: 2
    - Memory: 2 GB
- Redis VM
    - OS: Ubuntu Server 24.04
    - vCPU: 2
    - Memory: 2 GB
- Monitoring VM
    - OS: Ubuntu Server 24.04
    - vCPU: 2
    - Memory: 2 GB

---

- Load Generator
    - OS : Mac OS Tahoe(26)
    - CPU: M1
    - Memory: 8GB
</details>

<details>

<summary>v0.5 실험 환경</summary>

![v0.5 실험 환경](https://github.com/yeoooo/yeoooo.github.io/blob/master/assets/images/gold-rush-lab/v0-5/environment.png?raw=true)
- Host Machine
    - OS: Windows 11
    - CPU: AMD Ryzen 5 5600X
    - Memory: 32 GB
- Virtualization
    - Hyper-V
- Network
    - External Virtual Switch, 모든 VM은 동일한 Hyper-V Virtual Switch를 사용

    ---

- APP VM-01, APP VM-0
  - 실험에 따라 VM을 1대 감설
  - OS: Ubuntu Server 24.04
  - vCPU: 2
  - Memory: 2 GB
- Consumer VM-01, Consumer VM-02
  - 실험에 따라 VM을 1대 감설
  - OS: Ubuntu Server 24.04
  - vCPU: 2
  - Memory: 2 GB
- DB VM
  - OS: Ubuntu Server 24.04
  - vCPU: 2
  - Memory: 2 GB
- LB(LoadBalancer) VM
  - OS: Ubuntu Server 24.04
  - vCPU: 2
  - Memory: 2 GB
- Redis VM
  - OS: Ubuntu Server 24.04
  - vCPU: 2
  - Memory: 2 GB
- Messaging VM (Kafka)
  - OS: Ubuntu Server 24.04
  - vCPU: 2
  - Memory: 2 GB
- Monitoring VM
  - OS: Ubuntu Server 24.04
  - vCPU: 2
  - Memory: 2 GB

---

- Load Generator
  - OS : Mac OS Tahoe(26)
  - CPU: M1
  - Memory: 8GB
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

ProcessedMiningEventEntity
 ├── eventId: UUID
 ├── userSessionId: UUID
 ├── mineId: Long
 ├── minedAmount: Long
 ├── remainingAmount: Long
 └── requestedAt: Instant
```

`ProcessedMiningEventEntity`는 Consumer가 이미 처리한 `eventId`와 완료 결과를 저장하여 Kafka 재전달 시 채굴 트랜잭션을 중복 수행하지 않도록 합니다.

## Configuration

App와 Consumer는 각 모듈의 `application.yml` 및 `.env.example`에서 다음 환경 변수를 참조합니다.

| Environment Variable | Description |
| --- | --- |
| `POSTGRES_URL` | PostgreSQL JDBC URL |
| `POSTGRES_USER` | 애플리케이션에서 사용할 PostgreSQL 사용자명 |
| `POSTGRES_PASSWORD` | PostgreSQL 비밀번호 |
| `POSTGRES_DB` | PostgreSQL 데이터베이스 이름(App) |
| `DB_CONNECTION_POOL_SIZE` | App HikariCP 최대 데이터베이스 커넥션 수 (기본값: `50`) |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap server 주소 |
| `REDIS_HOST` | SSE 이벤트 중계를 위한 Redis 호스트(App) |
| `REDIS_PORT` | Redis 포트(App, 기본값: `6379`) |
| `SSE_HEARTBEAT_INTERVAL_MILLIS` | SSE heartbeat 간격(App, 기본값: `15000`) |

- 애플리케이션용 Compose 파일: `app/compose.yml`
- Consumer용 Compose 파일: `consumer/compose.yml`
- 독립 PostgreSQL 인프라 Compose 파일: `infra/db/compose.yml`
- Kafka Compose 파일: `infra/messaging/kafka/compose.yml`
- Redis Compose 파일: `infra/store/redis/compose.yml`
- 초기 DB 스키마: `infra/db/init/001-schema.sql`
- App 및 Consumer 기본 포트: `8080`

App의 HikariCP 최대 크기는 `DB_CONNECTION_POOL_SIZE`, Consumer는 Compose 환경 변수 `CONSUMER_DB_CONNECTION_POOL_SIZE`로 조정합니다.

## Container Image

App와 Consumer는 각각 다음 GHCR 이미지를 사용합니다.

```bash
docker pull ghcr.io/yeoooo/gold-rush-lab:v0.5_key-session_id
docker pull ghcr.io/yeoooo/gold-rush-lab-consumer:v0.2_optimistic-lock
```

비공개 패키지는 먼저 GitHub PAT로 로그인해야 합니다.

```bash
echo "$GHCR_TOKEN" | docker login ghcr.io -u yeoooo --password-stdin
```

### Docker Compose로 실행

PostgreSQL, Kafka, Redis는 별도 인프라로 실행한 뒤 App와 Consumer의 `.env`에 접속 정보를 설정합니다.

```bash
cp app/.env.example app/.env
cp consumer/.env.example consumer/.env

docker compose --env-file app/.env -f app/compose.yml up -d
docker compose --env-file consumer/.env -f consumer/compose.yml up -d
```

| Environment Variable | Default | Description |
| --- | --- | --- |
| `APP_IMAGE_TAG` | `v0.5` | 실행할 App 이미지 태그 |
| `APP_PORT` | `8080` | 호스트에 노출할 애플리케이션 포트 |
| `DB_CONNECTION_POOL_SIZE` | `50` | HikariCP 최대 데이터베이스 커넥션 수 |
| `CONSUMER_IMAGE_TAG` | `v0.1` | 실행할 Consumer 이미지 태그 |
| `CONSUMER_PORT` | `8080` | 호스트에 노출할 Consumer 포트 |
| `CONSUMER_DB_CONNECTION_POOL_SIZE` | `20` | Consumer HikariCP 최대 커넥션 수 |

## Load Test

k6를 사용하여 Smoke, Hot Spot, Capacity, Stress, Soak 시나리오를 실행할 수 있습니다. v0.5의 자동화 벤치마크는 VU별 반복 실행뿐 아니라 Kafka drain과 SSE 완료 전달까지 기다린 후 정합성을 검증합니다.

```bash
cp infra/load-test/.env.example infra/load-test/.env
./infra/load-test/run.sh hotspot
```

| Environment Variable | Default | Description |
| --- | ---: | --- |
| `BASE_URL` | `http://localhost:8080/api` | 테스트 대상 주소 |
| `MINE_AMOUNT` | `100000` | 생성할 광산의 초기 잔량 |
| `USER_COUNT` | `100` | 동시 접속 사용자 및 VU 수 |
| `ITERATIONS` | `100` | 사용자 한 명당 요청 횟수 |
| `HOTSPOT_MAX_DURATION` | `1m` | Hot Spot 시나리오 최대 실행 시간 |

자세한 실행 방법과 시나리오 설명은 [`infra/load-test/README.md`](infra/load-test/README.md)를 참고합니다.

v0.5 자동화 벤치마크는 다음과 같이 실행합니다.

```bash
cp infra/load-test/automation/.env.example infra/load-test/automation/.env
cd infra/load-test/automation
./benchmark.sh
```

결과는 `results/{version}_{실행시각}/` 아래의 `results.csv`, `event-metrics.csv`, `sse-metrics.csv`와 실행별 원본 파일로 저장됩니다. `event-metrics.csv`는 파이프라인과 Consumer 인스턴스 행을 구분하고, `Partition`, `Partition Messages` 컬럼으로 파티션별 메시지 수를 long format으로 기록합니다. 자세한 설정과 컬럼 정의는 [`infra/load-test/automation/README.md`](infra/load-test/automation/README.md)를 참고합니다.

### 데이터 정합성 검증

부하 테스트 후 `infra/db/assets/verification.sql`을 사용하여 광산의 초기 잔량, 실제 채굴량, 채굴 후 잔량, 광산별 채굴 로그 및 PostgreSQL wait 상태를 확인할 수 있습니다.

> 검증 파일 앞부분에는 테이블 초기화와 시퀀스 재설정 쿼리가 포함되어 있으므로 실행 범위를 확인해야 합니다.

## Monitoring

Spring Boot Actuator와 Micrometer가 애플리케이션 및 채굴 메트릭을 노출하고, Prometheus와 Grafana가 이를 수집·시각화합니다.

- HTTP TPS, 평균 응답 시간, P95 / P99 지연 시간
- 채굴 성공·실패 횟수
- 락 타임아웃, 데드락, 낙관적 락 재시도 횟수
- 락 획득 호출 지연 시간
- Producer TPS(App), Consumer TPS(worker), DB commit TPS
- Kafka Consumer lag, drain time, 파티션별 메시지 수
- 비동기 처리 E2E 평균/P95/P99와 SSE 전달률
- App·Consumer별 JVM, CPU, HikariCP 상태

```bash
cd infra
docker compose up -d
```

- Prometheus endpoint: `http://localhost:8080/api/actuator/prometheus`
- Prometheus UI: `http://localhost:9090`
- Grafana UI: `http://localhost:3000`
- 상세 메트릭 및 PromQL: [`infra/monitoring/README.md`](infra/monitoring/README.md)

---

## 공통 구현 사항

각 버전에서 동일하게 사용하는 기능과 실험 환경입니다.

- [x] Spring Boot, PostgreSQL, VM 환경 구성
- [x] 공통 응답 및 예외 처리
- [x] 광산 생성, 사용자 가입, 채굴 API
- [x] 도메인, Repository, Service, Controller 자동화 테스트
- [x] Smoke, Hot Spot, Capacity, Stress, Soak 부하 테스트
- [x] Prometheus / Grafana 모니터링
- [x] 데이터 정합성 및 DB wait 상태 검증 SQL
- [x] Docker / Docker Compose 실행 환경
- [x] GHCR 이미지 자동 게시
- [x] Kafka / Redis 기반 이벤트 처리 인프라
- [x] 비동기 처리 및 SSE 전달 벤치마크 자동화

---

## Roadmap

### v0.1 — Baseline

아무런 동시성 제어 없이 기본 채굴 시스템을 구현합니다.

- [x] 기본 채굴 API
- [x] 동시성 정합성 테스트
- [x] 핫스팟 스트레스 테스트
- [x] Lost Update 분석
- [x] TPS / 응답 시간 측정

### v0.2 — Database Lock

DB Lock을 이용하여 동시성 문제를 해결합니다.

- [x] Optimistic Lock
- [x] Pessimistic Lock
- [x] 동일한 벤치마크 수행
- [x] Lost Update 제거 확인
- [x] v0.1과 성능 비교
- [x] Lock 충돌률 분석

### v0.3 — Scale-out

API 서버를 여러 대로 확장합니다.

- [x] Multiple API Instance
- [x] Load Balancer
- [x] 동일한 벤치마크 수행
- [x] 처리량(TPS) 비교
- [x] DB Lock의 확장성 분석

### v0.4 — Distributed Lock

Redis 기반 분산 락을 적용합니다.

- [x] Redis
- [x] Distributed Lock
- [x] 동일한 벤치마크 수행
- [x] DB Lock 대비 성능 비교
- [x] 락 대기 시간 분석

### v0.5 — Event Driven

Kafka 기반의 비동기 처리 구조로 확장합니다.

- [x] Kafka
- [x] Event-Driven Architecture
- [x] 전용 Consumer와 멱등 처리
- [x] Kafka 완료 이벤트와 Redis Pub/Sub 기반 SSE 전달
- [x] `mineId`, `sessionId`, Consumer 원자적 업데이트 구성 벤치마크
- [x] Producer/Consumer 처리량, Kafka lag, E2E 지연 시간 수집
- [x] 최종 성능 분석

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
- Producer TPS와 Consumer TPS
- DB commit TPS와 Kafka lag/drain time
- 비동기 처리 및 SSE 전달 E2E 지연 시간
- Consumer 인스턴스·파티션별 메시지 분포
- 락 대기 시간
- 타임아웃 발생 횟수
- 데드락 발생 여부

---

## Project Structure

```text
Gold-Rush-Lab
├── .github/workflows
│   └── ghcr.yml
├── app
│   ├── Dockerfile
│   ├── build.gradle
│   ├── compose.yml
│   └── src
│       ├── main
│       │   ├── java/io/devyeoooo/Gold_Rush_Lab
│       │   │   ├── comm
│       │   │   ├── messaging
│       │   │   ├── mine
│       │   │   ├── mining_log
│       │   │   ├── observability
│       │   │   ├── presentation
│       │   │   ├── user
│       │   │   └── GoldRushLabApplication.java
│       │   └── resources/application.yml
│       └── test
├── consumer
│   ├── Dockerfile
│   ├── build.gradle
│   ├── compose.yml
│   └── src
│       ├── main
│       │   ├── java/io/devyeoooo/Gold_Rush_Lab_Consumer
│       │   │   ├── messaging
│       │   │   ├── mine
│       │   │   ├── mining_log
│       │   │   ├── observability
│       │   │   └── user
│       │   └── resources/application.yml
│       └── test
└── infra
    ├── compose.yml
    ├── db
    │   ├── assets/verification.sql
    │   ├── compose.yml
    │   └── init/001-schema.sql
    ├── load-test
    │   ├── automation
    │   │   ├── benchmark.sh
    │   │   ├── sse-client.mjs
    │   │   └── sql
    │   ├── lib
    │   ├── scenarios
    │   │   ├── smoke.js
    │   │   ├── hotspot.js
    │   │   ├── capacity.js
    │   │   ├── stress.js
    │   │   └── soak.js
    │   └── run.sh
    ├── loadbalancer
    ├── messaging/kafka
    ├── monitoring
    │   ├── compose.yml
    │   ├── grafana
    │   └── prometheus
    └── store/redis
```
