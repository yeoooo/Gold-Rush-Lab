# 부하 실험 자동화

지정한 버전과 애플리케이션, Prometheus, PostgreSQL을 대상으로 웜업 1회 후
VU 10, 50, 100, 300, 500을 각각 5회 실행한다. 각 실행 결과는 즉시 CSV에
기록하며, VU별 실행이 끝나면 5회 산술평균 행을 추가한다.

각 실행에서 생성하는 광산의 초기 잔량은 `VU × ITERATIONS`로 자동 계산한다.
예를 들어 VU 50, ITERATIONS 100이면 초기 잔량은 5,000이다.

웜업을 포함한 모든 실행 직전에 다음 테이블을 초기화하고 identity 값을
재설정한다.

```sql
TRUNCATE TABLE app_user, mine, mining_log
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

기준과 확인 간격, 제한 시간은 `.env.example`의 `STABLE_*` 변수로 변경할 수
있다.

각 실행의 시작부터 종료까지를 Grafana annotation 영역으로 등록한다.
annotation에는 버전, 시나리오, VU, 실행 회차, 실행 유형 태그가 포함되므로
같은 그래프에서도 각 실행 구간을 명확하게 구분할 수 있다.

TPS가 `0 req/s`이고 CPU, JVM heap, Hikari가 안정화 기준을 연속 3회 만족한
후에도 기본 15초를 추가로 기다린다. annotation이 정확한 시작·종료 영역을
표시하고, 실행 사이에 충분한 0 TPS 구간이 생겨 그래프에서 각 실험을 쉽게
구분할 수 있다. 추가 대기 시간은 `POST_STABLE_COOLDOWN_SECONDS`로 변경한다.

## 필요 도구

- bash 3.2 이상
- k6
- curl
- jq
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
├── runs.jsonl
├── lock-waits/
│   ├── warmup_vu10_run1.csv
│   └── benchmark_vu100_run1.csv
├── logs/
└── summaries/
```

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

분산 환경의 부하 분배와 인스턴스별 병목을 쉽게 필터링할 수 있도록 하나의
실행을 대상별 CSV 행으로 나누어 기록한다.

- `Target Type=LOAD_BALANCER`: 기존 k6 전체 TPS와 실행 결과
- `Target Type=BACKEND`: Prometheus `instance`별 TPS, System CPU,
  Process CPU, JVM Heap 피크

`Target`에는 로드밸런서 요청 URL 또는 Prometheus의 백엔드 `instance`
label이 들어간다. 백엔드 행에서 해당하지 않는 Hikari, latency, 정합성
컬럼은 비워 둔다. 로드밸런서 행의 CPU와 Heap도 비워 두며, 해당 값은 각
백엔드 행에서 확인한다. 기존 Hikari 합계는 실행 설정과 전체 풀 사용량을
나타내므로 로드밸런서 실행 요약 행에 유지한다.

```text
RUN,LOAD_BALANCER,http://192.168.0.47/api,...,492.814 req/s,...
RUN,BACKEND,192.168.0.41:8080,...,245.123 req/s,...
RUN,BACKEND,192.168.0.46:8080,...,247.691 req/s,...
```

`AVERAGE`도 같은 구조이며, 백엔드 행은 같은 VU의 반복 실행 값을
인스턴스별로 산술평균한다.

각 k6 실행 중에는 별도 PostgreSQL 연결이 `pg_locks`의 `granted=false` 행을
주기적으로 조회한다. `waitstart`부터 관측 시점까지의 DB 내부 lock wait를
`lock-waits/{실행}.csv`에 원본 샘플로 기록한다. 기본 관측 간격은 0.1초이며
다음 환경변수 또는 CLI 옵션으로 변경할 수 있다.

```dotenv
LOCK_WAIT_POLL_SECONDS=0.1
```

```sh
./benchmark.sh ... --lock-wait-poll-seconds 0.05
```

메인 `results.csv`에는 같은 대기를 `PID`, virtual transaction,
`waitstart`, lock 대상의 조합으로 묶은 뒤 다음 값을 기록한다.

- `Lock Waits`: 실행 중 한 번 이상 관측한 고유 lock wait 수
- `Lock Wait Total`: 각 wait에서 마지막으로 관측한 시간의 합(ms)
- `Lock Wait Avg`: 관측한 wait의 평균 시간(ms)
- `Lock Wait Max`: 가장 길게 관측된 wait 시간(ms)

폴링 사이에 시작하고 끝난 짧은 lock wait는 수집할 수 없다. 또한 각 wait의
종료 직전이 아니라 마지막 폴링 시점까지 측정하므로 Total, Avg, Max는 실제
DB lock wait의 하한이다. 더 짧은 간격은 관측 누락을 줄이지만 PostgreSQL
연결과 `pg_locks` 조회 부하를 증가시킨다.

`Optimistic Retry Count`는 실행 구간의
`gold_rush_optimistic_lock_retry_total` 증가량이다. 메트릭이 아직 생성되지
않은 경우에는 0으로 기록한다.

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
