# Gold Rush Lab 모니터링

Spring Boot Actuator의 HTTP 메트릭과 Gold Rush Lab의 비즈니스·동시성 메트릭을 Prometheus로 수집한다.

## 메트릭

| Prometheus 메트릭 | 종류/단위 | 의미 | 태그 |
| --- | --- | --- | --- |
| `gold_rush_mining_success_total` | Counter/회 | 트랜잭션 커밋이 완료된 채굴 횟수 | `strategy` |
| `gold_rush_mining_failure_total` | Counter/회 | 채굴 실패 횟수 | `strategy`, `exception` |
| `gold_rush_lock_timeout_total` | Counter/회 | 락 획득 또는 쿼리 타임아웃 횟수 | `strategy` |
| `gold_rush_deadlock_total` | Counter/회 | PostgreSQL 데드락 횟수 | `strategy` |
| `gold_rush_optimistic_lock_retry_total` | Counter/회 | 낙관적 락 충돌 후 실제 재시도 횟수 | `strategy=optimistic` |
| `gold_rush_mining_lock_wait_seconds_*` | Timer/초 | 애플리케이션 관점의 락 획득 호출 지연 시간 | `strategy` |

`strategy`의 허용값은 `none`, `optimistic`, `pessimistic`, `redis`이며 `gold-rush.mining.lock-strategy` 설정으로 관리한다. `exception`은 `cannot_acquire_lock`, `lock_timeout`, `deadlock`, `optimistic_lock`, `unknown`으로 제한한다. 사용자 ID, 세션 ID, 광산 ID 같은 고카디널리티 값은 태그에 사용하지 않는다.

락 대기 Timer는 순수 PostgreSQL row lock wait가 아니다. 락 조회 호출 전후를 애플리케이션에서 측정하므로 DB 네트워크 왕복과 SQL 실행 시간도 포함될 수 있다. 현재 `v0.1`은 `strategy=none`이고 실제 락 획득 호출이 없으므로 채굴 경로에서 이 Timer를 기록하지 않는다. 향후 실제 락 전략을 추가할 때 `MiningMetrics.startLockWait()`와 `stopLockWait()`로 락 획득 호출을 `finally`에서 감싸야 한다. 현재 낙관적 락 재시도 로직도 없으므로 retry counter는 실제 채굴 경로에서 증가하지 않는다.

HTTP 처리량과 응답 시간은 중복 커스텀 메트릭을 만들지 않고 아래 Actuator 메트릭을 사용한다.

- `http_server_requests_seconds_count`
- `http_server_requests_seconds_sum`
- `http_server_requests_seconds_bucket`

Prometheus endpoint는 `http://localhost:8080/api/actuator/prometheus`이다. 실제 채굴 컨트롤러 경로는 `POST /api/v01/mine`이고, 일반적인 Spring MVC `uri` 태그는 컨텍스트 패스를 제외한 `/v01/mine`으로 노출된다. 실행 환경의 endpoint 출력이 다르면 아래 selector를 실제 `uri` 값에 맞춘다.

## Grafana PromQL

TPS:

```promql
sum(rate(http_server_requests_seconds_count{uri="/v01/mine",method="POST"}[1m]))
```

선택 범위 내 최대 TPS:

```promql
max_over_time(
  (sum(rate(http_server_requests_seconds_count{uri="/v01/mine",method="POST"}[1m])))
  [$__range:5s]
)
```

평균 응답 시간(초):

```promql
sum(rate(http_server_requests_seconds_sum{uri="/v01/mine",method="POST"}[1m]))
/
sum(rate(http_server_requests_seconds_count{uri="/v01/mine",method="POST"}[1m]))
```

P95 / P99:

```promql
histogram_quantile(
  0.95,
  sum by (le) (rate(http_server_requests_seconds_bucket{uri="/v01/mine",method="POST"}[1m]))
)
```

```promql
histogram_quantile(
  0.99,
  sum by (le) (rate(http_server_requests_seconds_bucket{uri="/v01/mine",method="POST"}[1m]))
)
```

평균 락 획득 호출 지연 시간(전략별):

```promql
sum by (strategy) (rate(gold_rush_mining_lock_wait_seconds_sum[1m]))
/
sum by (strategy) (rate(gold_rush_mining_lock_wait_seconds_count[1m]))
```

락 대기 P95:

```promql
histogram_quantile(
  0.95,
  sum by (le, strategy) (rate(gold_rush_mining_lock_wait_seconds_bucket[1m]))
)
```

선택 기간의 타임아웃 및 데드락 횟수:

```promql
sum by (strategy) (increase(gold_rush_lock_timeout_total[$__range]))
```

```promql
sum(increase(gold_rush_deadlock_total[$__range]))
```

데드락 발생 여부(Stat 패널에서 `0=미발생`, `1=발생`으로 매핑):

```promql
sum(increase(gold_rush_deadlock_total[$__range])) > bool 0
```
