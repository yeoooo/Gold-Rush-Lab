# 부하 실험 자동화

지정한 버전과 애플리케이션, Prometheus, PostgreSQL을 대상으로 웜업 1회 후
VU 10, 50, 100, 300, 500을 각각 5회 실행한다. 각 실행 결과는 즉시 CSV에
기록하며, VU별 실행이 끝나면 5회 산술평균 행을 추가한다.

각 실행에서 생성하는 광산의 초기 잔량은 `VU × ITERATIONS`로 자동 계산한다.
예를 들어 VU 50, ITERATIONS 100이면 초기 잔량은 5,000이다.

웜업을 포함한 모든 실행 직전에 다음 테이블을 초기화하고 identity 값을
재설정한다.

```sql
TRUNCATE TABLE processed_mining_event, app_user, mine, mining_log
    RESTART IDENTITY
    CASCADE;
```

따라서 자동화에 지정한 DB의 기존 광산, 사용자, 채굴 로그 데이터는 매 실행
직전에 삭제된다.

각 실행 후 다음 조건을 연속 3회 만족해야 다음 실행을 시작한다.

- Process CPU 20% 이하
- System CPU 50% 이하
- JVM heap 사용률 75% 이하
- Hikari active/max 비율 20% 이하
- 채굴 API TPS 0.1 이하
- Consumer Process CPU 20% 이하
- Consumer JVM heap 사용률 75% 이하
- Consumer Hikari active/max 비율 20% 이하
- Consumer TPS 0.1 이하

기준과 확인 간격, 제한 시간은 `.env.example`의 `STABLE_*` 변수로 변경할 수
있다.

각 실행의 시작부터 종료까지를 Grafana annotation 영역으로 등록한다.
annotation에는 버전, 시나리오, VU, 실행 회차, 실행 유형 태그가 포함되므로
같은 그래프에서도 각 실행 구간을 명확하게 구분할 수 있다.

TPS가 `0 req/s`이고 CPU, JVM heap, Hikari가 안정화 기준을 연속 3회 만족한
후에도 기본 30초를 추가로 기다린다. annotation이 정확한 시작·종료 영역을
표시하고, 실행 사이에 충분한 0 TPS 구간이 생겨 그래프에서 각 실험을 쉽게
구분할 수 있다. 추가 대기 시간은 `POST_STABLE_COOLDOWN_SECONDS`로 변경한다.

## 필요 도구

- bash 3.2 이상
- k6
- curl
- jq
- Node.js 18 이상(SSE 클라이언트)
- PostgreSQL `psql`
- awk

macOS Homebrew의 keg-only `libpq`도 자동 탐지한다.

```sh
brew install libpq
```

다른 위치의 클라이언트를 사용하려면 `PSQL_BIN`에 실행 파일 경로를 지정한다.

애플리케이션의 Actuator 지표가 Prometheus에 수집되고 있어야 하며 다음
메트릭이 필요하다.

- `process_cpu_usage`, `system_cpu_usage`
- `jvm_memory_used_bytes`, `jvm_memory_max_bytes`
- `hikaricp_connections_active`, `hikaricp_connections_max`
- `http_server_requests_seconds_count`
- `tomcat_threads_config_max_threads`, `tomcat_threads_busy_threads`

Tomcat 스레드 지표를 노출하려면 앱에
`server.tomcat.mbeanregistry.enabled=true`가 설정되어 있어야 한다. 자동화는
preflight에서 두 지표를 확인해 부하 실행 전에 누락 여부를 알린다.

## 실행

```sh
cd infra/load-test/automation
chmod +x benchmark.sh

PGPASSWORD=secret ./benchmark.sh \
  --version v0.1 \
  --request-host http://app-host:8080/api \
  --monitor-host http://prometheus-host:9090 \
  --grafana-url http://grafana-host:3000 \
  --db-url postgresql://goldrush@db-host:5432/goldrush \
  --scenario hotspot \
  --iterations 100
```

또는 `.env.example`을 `.env`로 복사해 값을 채운 후 실행할 수 있다.

```sh
cp .env.example .env
./benchmark.sh
```

기본 결과 위치는 다음과 같다.

```text
results/{version}_{UTC 실행시각}/
├── results.csv
├── event-metrics.csv
├── event-runs.jsonl
├── sse-metrics.csv
├── sse-runs.jsonl
├── runs.jsonl
├── fixtures/
├── lock-waits/
├── logs/
├── sse/
└── summaries/
```

`event-metrics.csv`에는 비동기 처리 가설 검증을 위한 다음 지표가 실행별로
기록된다.

분산 락 전용 항목을 제외한 기존 `results.csv` 지표는 그대로 유지된다. 전체
API TPS·지연시간·오류율, Hikari 사용량, DB 정합성뿐 아니라 애플리케이션
CPU·Heap, Tomcat 가용 스레드, PostgreSQL lock wait를 기록한다.
`lock-waits/`에는 실행별 `pg_locks` 원본 관측 CSV가 저장되며 관측 간격은
`LOCK_WAIT_POLL_SECONDS`로 조정한다. Redis 분산 락을 사용하지 않는 현재
구성에서는 분산 락 전용 지표를 수집하지 않는다.

- Producer TPS, Consumer TPS
- consumer 채굴 DB commit TPS, PostgreSQL 전체 commit TPS
- 최대 consumer lag와 drain time
- E2E 평균/P95/P99
- consumer Hikari active 피크
- 처리 순서 위반 횟수
- consumer instance별 TPS·처리 건수·DB commit TPS·Hikari 피크
- partition별 메시지 수

`event-metrics.csv`는 `Target Type=PIPELINE`인 전체 집계 행과
`Target Type=CONSUMER`인 consumer 인스턴스별 행을 함께 기록한다. JSON 맵을
한 셀에 넣지 않으므로 인스턴스별 실행 및 평균 결과를 행 단위로 필터링할 수
있다. PIPELINE 행은 `Partition`, `Partition Messages` 컬럼을 사용해 partition별
long format으로 기록하며, 나머지 파이프라인 지표는 각 partition 행에 반복한다.
CONSUMER 행의 partition 컬럼은 비워 둔다. 행의 기준은 metric이 발생한
인스턴스가 아니라 preflight에서 확인한 전체 UP consumer 집합이며, 처리하지
않은 인스턴스의 counter는 0으로 기록한다.

각 실행 전에 VU 수만큼 `/events/{sessionId}` SSE 연결을 열고 모든 연결에서
`connected` 이벤트를 확인한 뒤 k6 부하를 시작한다. 따라서 API 요청뿐 아니라
실제 결과 전달에 필요한 장기 연결과 직렬화·네트워크 비용도 앱 서버 부하에
포함된다. `sse-metrics.csv`에는 연결 수, 기대/수신 이벤트 수, 전달률,
중복·비정상 이벤트 수, 클라이언트 기준 E2E 평균/P95/P99/최대 시간과 SSE
drain time이 기록된다. 연결이 끊기면 실제 브라우저 `EventSource`처럼 해당
세션만 재접속하며 재접속 및 연결 오류 횟수도 함께 기록한다.

SSE 클라이언트는 한 세션당 하나의 연결을 유지한다. k6 실행 전후의 서버
`gold_rush_producer_mining_success_total` counter 차이로 실제 Kafka 성공 발행
수를 계산하고, 그 수만큼의 고유 완료 이벤트를 모두 수신해야 성공한다. HTTP
응답 유실이나 클라이언트 타임아웃이 발생해도 이미 발행된 이벤트를 기대
건수에서 제외하지 않는다. 연결 준비 및 완료 제한 시간은 각각
`SSE_READY_TIMEOUT_SECONDS`, `SSE_CLIENT_TIMEOUT_SECONDS`로 조정한다.

Kafka drain 이후 `SSE_FINALIZE_GRACE_SECONDS` 동안 네트워크 버퍼 수신을 기다린
뒤 SSE 수집을 확정한다. 기대 건수보다 적으면 `Status=incomplete`와 실제
전달률을 기록하고 다음 실험을 계속한다. 연결 자체가 복구되지 않거나 전체
제한 시간을 넘긴 경우에만 벤치마크를 중단한다.

웜업과 각 실행의 결과 및 메트릭을 기록한 뒤 앱의 SSE cleanup API를 호출한다.
cleanup 요청은 Redis Pub/Sub으로 모든 앱 인스턴스에 전달되며, 각 인스턴스는
자신이 보유한 emitter를 모두 제거하고 `complete()`한다. cleanup 이후에는 기존
안정화 확인과 `POST_STABLE_COOLDOWN_SECONDS` 대기 시간을 그대로 사용하므로
실행 사이에 별도의 cleanup 대기 시간은 두지 않는다. 실행이 중단된 경우에도
종료 trap에서 cleanup API를 최선 노력 방식으로 호출한다.

k6 부하가 끝나면 `kafka_consumergroup_lag`가 `DRAIN_LAG_THRESHOLD` 이하로
연속 `DRAIN_STABLE_SAMPLES`회 관측될 때까지 기다린다. 이 시점 이후 DB
정합성을 검증하므로 아직 처리 중인 비동기 요청 때문에 검증이 실패하지 않는다.
대기 제한은 `DRAIN_TIMEOUT_SECONDS`로 설정한다. kafka-exporter가 request lag를
`-1`로 반환하면 이를 0으로 간주하지 않고, 해당 광산의 처리 이벤트 수를
실제 성공 발행 수와 비교하고 completion lag도 함께 확인해 drain 여부를
판정한다.

Kafka Key, Partition, Offset, Event ID와 처리 순서는 consumer의 구조화 로그에
기록한다. Key와 Offset은 고카디널리티 값이므로 Prometheus label로 사용하지
않고, partition별 메시지 수만 메트릭으로 수집한다.

추가로 필요한 Prometheus target은 다음과 같다.

- `gold-rush-consumer`: consumer Actuator
- `gold-rush-kafka`: kafka-exporter
- `gold-rush-postgres`: postgres-exporter

VM 주소가 기본 `prometheus.yml`과 다르면 benchmark 실행 전에 target 주소를
실제 배포 환경에 맞게 변경해야 한다.

비밀번호는 명령행 인자로 받지 않는다. `PGPASSWORD` 또는 PostgreSQL
`.pgpass`를 사용한다.

Grafana 인증정보도 환경변수 또는 `.env`에 지정한다. Basic Auth 대신 service
account token을 사용하는 경우 `GRAFANA_TOKEN`을 지정하고 사용자·비밀번호는
생략할 수 있다.

```dotenv
GRAFANA_URL=http://grafana-host:3000
GRAFANA_USER=admin
GRAFANA_PASSWORD=change-me
GRAFANA_DASHBOARD_UID=gold-rush-lab
```

등록되는 영역 annotation의 예시는 다음과 같다.

```text
v0.1 | hotspot | VU 100 | Run 3 | benchmark
```

## CSV

`WARMUP` 행은 웜업 결과, `RUN` 행은 한 번의 본 실행 결과이고 `AVERAGE` 행은
같은 VU의 5회 산술평균이다. 웜업은 평균에 포함하지 않는다. 각 실행 행에는
`Asia/Seoul` 기준 `Started At`, `Finished At`을 `+0900` 오프셋과 함께
기록한다. 정합성은 다음 조건을 모두 만족할 때만 `✅`로 기록한다.

TPS는 초당 요청 수인 `req/s`로 기록한다. TPS, CPU, JVM heap, latency,
error rate 등의 실수 지표는 소수점 아래 3자리로 반올림한다.

`TPS (req/s)`는 k6가 로드밸런서를 통해 보낸 전체 요청을 최초 요청 시작부터
마지막 응답 완료까지의 시간으로 나눈 값이다. 이 전체 TPS는 기존과 동일하게
유지한다.

애플리케이션 CPU·Heap·Hikari 피크와 Tomcat 최소 가용 스레드는 부하 종료 후
scrape까지 포함해 마지막 관측치를 놓치지 않는다. 백엔드 TPS는 실행 직전과
종료 후 scrape된 HTTP counter 차이로 계산한다. Tomcat 평균 가용 스레드는
idle drain 구간이 섞이지 않도록 k6 시작부터 HTTP 부하 종료 시점까지만
계산한다. PostgreSQL lock wait 결과는 원본 수집 시점 중 `STARTED_AT`부터 Kafka
drain 완료 시점까지만 밀리초 단위로 필터링해 요약하며 SSE 완료 대기 구간은
제외한다.

분산 환경의 부하 분배와 인스턴스별 병목을 쉽게 필터링할 수 있도록 하나의
실행을 대상별 CSV 행으로 나누어 기록한다.

- `Target Type=LOAD_BALANCER`: 기존 k6 전체 TPS와 실행 결과
- `Target Type=BACKEND`: Prometheus `instance`별 TPS, System CPU,
  Process CPU, JVM Heap 피크, Tomcat 가용 스레드 최소/평균

`Target`에는 로드밸런서 요청 URL 또는 Prometheus의 백엔드 `instance`
label이 들어간다. 백엔드 행에서 해당하지 않는 Hikari, latency, 정합성
컬럼은 비워 둔다. 로드밸런서 행의 CPU와 Heap도 비워 두며, 해당 값은 각
백엔드 행에서 확인한다. 기존 Hikari 합계는 실행 설정과 전체 풀 사용량을
나타내므로 로드밸런서 실행 요약 행에 유지한다. 백엔드 행도 preflight의 전체
UP 앱 인스턴스를 기준으로 생성하며 요청을 처리하지 않은 인스턴스의 TPS는
0으로 기록한다.

```text
RUN,LOAD_BALANCER,http://192.168.0.47/api,...,492.814 req/s,...
RUN,BACKEND,192.168.0.41:8080,...,245.123 req/s,...
RUN,BACKEND,192.168.0.46:8080,...,247.691 req/s,...
```

`AVERAGE`도 같은 구조이며, 백엔드 행은 같은 VU의 반복 실행 값을
인스턴스별로 산술평균한다.

```text
초기 잔량 = 사용자 총 채굴량 + 실제 잔량
초기 잔량 = Mining Log 총 채굴량 + 실제 잔량
사용자 총 채굴량 = Mining Log 총 채굴량
```

평균 행의 정합성은 5회가 모두 `✅`일 때만 `✅`이다.

Prometheus에 여러 애플리케이션이 수집된다면 `--prom-labels`로 실험 대상을
유일하게 선택해야 한다.

```sh
./benchmark.sh \
  ... \
  --prom-labels 'application="gold-rush-lab",environment="test"'
```

현재 자동화 시나리오는 `hotspot`이다. 다른 요청 흐름은
`scenarios/{name}.js`를 같은 메트릭 계약으로 추가하면 선택할 수 있다.
