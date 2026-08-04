#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)

if [[ -f "$SCRIPT_DIR/../../.env" ]]; then
    set -a
    # shellcheck disable=SC1091
    source "$SCRIPT_DIR/../../.env"
    set +a
fi

if [[ -f "$SCRIPT_DIR/.env" ]]; then
    set -a
    # shellcheck disable=SC1091
    source "$SCRIPT_DIR/.env"
    set +a
fi

VERSION=${VERSION:-}
REQUEST_HOST=${REQUEST_HOST:-}
MONITOR_HOST=${MONITOR_HOST:-}
GRAFANA_URL=${GRAFANA_URL:-}
GRAFANA_USER=${GRAFANA_USER:-${GRAFANA_ADMIN_USER:-}}
GRAFANA_PASSWORD=${GRAFANA_PASSWORD:-${GRAFANA_ADMIN_PASSWORD:-}}
GRAFANA_TOKEN=${GRAFANA_TOKEN:-}
GRAFANA_DASHBOARD_UID=${GRAFANA_DASHBOARD_UID:-gold-rush-lab}
DB_URL=${DB_URL:-}
SCENARIO=${SCENARIO:-hotspot}
ITERATIONS=${ITERATIONS:-100}
REPEAT_COUNT=${REPEAT_COUNT:-5}
VU_LIST=${VU_LIST:-10,50,100,300,500}
WARMUP_VU=${WARMUP_VU:-10}
WARMUP_ITERATIONS=${WARMUP_ITERATIONS:-10}
MAX_DURATION=${MAX_DURATION:-10m}
PROM_LABELS=${PROM_LABELS:-'application="gold-rush-lab"'}
CONSUMER_PROM_LABELS=${CONSUMER_PROM_LABELS:-'application="gold-rush-consumer"'}
POSTGRES_PROM_LABELS=${POSTGRES_PROM_LABELS:-'application="gold-rush-postgres"'}
KAFKA_CONSUMER_GROUP=${KAFKA_CONSUMER_GROUP:-gold-rush-mining-consumer}
KAFKA_REQUEST_TOPIC=${KAFKA_REQUEST_TOPIC:-mining-requested}
KAFKA_COMPLETION_GROUP=${KAFKA_COMPLETION_GROUP:-gold-rush-sse-relay}
KAFKA_COMPLETION_TOPIC=${KAFKA_COMPLETION_TOPIC:-mining-completed}
POSTGRES_DATABASE=${POSTGRES_DATABASE:-goldrush}
MINE_URI=${MINE_URI:-/mine}
PROM_SCRAPE_DELAY=${PROM_SCRAPE_DELAY:-6}
DRAIN_POLL_SECONDS=${DRAIN_POLL_SECONDS:-1}
DRAIN_TIMEOUT_SECONDS=${DRAIN_TIMEOUT_SECONDS:-600}
DRAIN_STABLE_SAMPLES=${DRAIN_STABLE_SAMPLES:-2}
DRAIN_LAG_THRESHOLD=${DRAIN_LAG_THRESHOLD:-0}
SSE_CLIENT_TIMEOUT_SECONDS=${SSE_CLIENT_TIMEOUT_SECONDS:-${DRAIN_TIMEOUT_SECONDS}}
SSE_READY_TIMEOUT_SECONDS=${SSE_READY_TIMEOUT_SECONDS:-120}
SSE_READY_POLL_SECONDS=${SSE_READY_POLL_SECONDS:-1}
SSE_FINALIZE_GRACE_SECONDS=${SSE_FINALIZE_GRACE_SECONDS:-2}
STABLE_PROCESS_CPU_PCT=${STABLE_PROCESS_CPU_PCT:-20}
STABLE_SYSTEM_CPU_PCT=${STABLE_SYSTEM_CPU_PCT:-50}
STABLE_HEAP_PCT=${STABLE_HEAP_PCT:-75}
STABLE_HIKARI_PCT=${STABLE_HIKARI_PCT:-20}
STABLE_TPS=${STABLE_TPS:-0}
STABLE_SAMPLES=${STABLE_SAMPLES:-3}
STABLE_POLL_SECONDS=${STABLE_POLL_SECONDS:-5}
STABLE_TIMEOUT_SECONDS=${STABLE_TIMEOUT_SECONDS:-600}
POST_STABLE_COOLDOWN_SECONDS=${POST_STABLE_COOLDOWN_SECONDS:-30}
LOCK_WAIT_POLL_SECONDS=${LOCK_WAIT_POLL_SECONDS:-1}
OUTPUT_FILE=${OUTPUT_FILE:-}
PSQL_BIN=${PSQL_BIN:-}

usage() {
    cat <<'EOF'
Usage:
  PGPASSWORD=secret ./benchmark.sh \
    --version v0.1 \
    --request-host http://app:8080/api \
    --monitor-host http://prometheus:9090 \
    --db-url postgresql://goldrush@postgres:5432/goldrush \
    --scenario hotspot

Options:
  --version VALUE             결과에 기록할 애플리케이션 버전
  --request-host URL          k6 요청 대상(base URL)
  --monitor-host URL          Prometheus URL
  --grafana-url URL           실행 구간 annotation을 등록할 Grafana URL
  --db-url URL                PostgreSQL libpq 접속 URL
  --scenario NAME             automation/scenarios의 시나리오 이름
  --iterations N              VU 한 명당 요청 횟수
  --repeat N                  VU별 반복 횟수(기본 5)
  --vus CSV                   VU 목록(기본 10,50,100,300,500)
  --output FILE               생성할 CSV 경로
  --prom-labels MATCHERS      Prometheus label matcher(중괄호 제외)
  --mine-uri PATH             TPS 안정화 확인용 서버 URI label
  --lock-wait-poll-seconds N  pg_locks 관측 간격(초, 기본 1)
  --help

안정화 기준은 .env.example의 STABLE_* 환경변수로 조정할 수 있습니다.
EOF
}

while (($# > 0)); do
    case "$1" in
        --version) VERSION=${2:?}; shift 2 ;;
        --request-host) REQUEST_HOST=${2:?}; shift 2 ;;
        --monitor-host) MONITOR_HOST=${2:?}; shift 2 ;;
        --grafana-url) GRAFANA_URL=${2:?}; shift 2 ;;
        --db-url) DB_URL=${2:?}; shift 2 ;;
        --scenario) SCENARIO=${2:?}; shift 2 ;;
        --iterations) ITERATIONS=${2:?}; shift 2 ;;
        --repeat) REPEAT_COUNT=${2:?}; shift 2 ;;
        --vus) VU_LIST=${2:?}; shift 2 ;;
        --output) OUTPUT_FILE=${2:?}; shift 2 ;;
        --prom-labels) PROM_LABELS=${2:?}; shift 2 ;;
        --mine-uri) MINE_URI=${2:?}; shift 2 ;;
        --lock-wait-poll-seconds) LOCK_WAIT_POLL_SECONDS=${2:?}; shift 2 ;;
        --help|-h) usage; exit 0 ;;
        *) echo "알 수 없는 옵션: $1" >&2; usage >&2; exit 2 ;;
    esac
done

if [[ -z "$GRAFANA_URL" &&
    "$MONITOR_HOST" =~ ^(https?://[^/:]+)(:[0-9]+)?$ ]]; then
    GRAFANA_URL="${BASH_REMATCH[1]}:3000"
fi

require_value() {
    local name=$1
    local value=$2
    if [[ -z "$value" ]]; then
        echo "필수 입력이 없습니다: $name" >&2
        exit 2
    fi
}

require_positive_integer() {
    local name=$1
    local value=$2
    if [[ ! "$value" =~ ^[1-9][0-9]*$ ]]; then
        echo "$name 값은 양의 정수여야 합니다: $value" >&2
        exit 2
    fi
}

require_nonnegative_integer() {
    local name=$1
    local value=$2
    if [[ ! "$value" =~ ^[0-9]+$ ]]; then
        echo "$name 값은 0 이상의 정수여야 합니다: $value" >&2
        exit 2
    fi
}

require_positive_number() {
    local name=$1
    local value=$2
    if ! awk -v value="$value" \
        'BEGIN { exit !(value ~ /^[0-9]+([.][0-9]+)?$/ && value + 0 > 0) }'; then
        echo "$name 값은 0보다 큰 숫자여야 합니다: $value" >&2
        exit 2
    fi
}

require_value VERSION "$VERSION"
require_value REQUEST_HOST "$REQUEST_HOST"
require_value MONITOR_HOST "$MONITOR_HOST"
require_value GRAFANA_URL "$GRAFANA_URL"
require_value DB_URL "$DB_URL"
if [[ -z "$GRAFANA_TOKEN" ]]; then
    require_value GRAFANA_USER "$GRAFANA_USER"
    require_value GRAFANA_PASSWORD "$GRAFANA_PASSWORD"
fi
require_positive_integer ITERATIONS "$ITERATIONS"
require_positive_integer REPEAT_COUNT "$REPEAT_COUNT"
require_positive_integer WARMUP_VU "$WARMUP_VU"
require_positive_integer WARMUP_ITERATIONS "$WARMUP_ITERATIONS"
require_positive_integer STABLE_SAMPLES "$STABLE_SAMPLES"
require_positive_integer STABLE_POLL_SECONDS "$STABLE_POLL_SECONDS"
require_positive_integer STABLE_TIMEOUT_SECONDS "$STABLE_TIMEOUT_SECONDS"
require_positive_integer POST_STABLE_COOLDOWN_SECONDS "$POST_STABLE_COOLDOWN_SECONDS"
require_positive_integer DRAIN_POLL_SECONDS "$DRAIN_POLL_SECONDS"
require_positive_integer DRAIN_TIMEOUT_SECONDS "$DRAIN_TIMEOUT_SECONDS"
require_positive_integer DRAIN_STABLE_SAMPLES "$DRAIN_STABLE_SAMPLES"
require_nonnegative_integer DRAIN_LAG_THRESHOLD "$DRAIN_LAG_THRESHOLD"
require_positive_integer SSE_CLIENT_TIMEOUT_SECONDS "$SSE_CLIENT_TIMEOUT_SECONDS"
require_positive_integer SSE_READY_TIMEOUT_SECONDS "$SSE_READY_TIMEOUT_SECONDS"
require_positive_integer SSE_READY_POLL_SECONDS "$SSE_READY_POLL_SECONDS"
require_nonnegative_integer SSE_FINALIZE_GRACE_SECONDS "$SSE_FINALIZE_GRACE_SECONDS"
require_positive_number LOCK_WAIT_POLL_SECONDS "$LOCK_WAIT_POLL_SECONDS"

if [[ ! "$SCENARIO" =~ ^[a-zA-Z0-9_-]+$ ]]; then
    echo "시나리오 이름에는 영문, 숫자, _, -만 사용할 수 있습니다." >&2
    exit 2
fi

SCENARIO_FILE="$SCRIPT_DIR/scenarios/$SCENARIO.js"
if [[ ! -f "$SCENARIO_FILE" ]]; then
    echo "지원하지 않는 시나리오입니다: $SCENARIO" >&2
    exit 2
fi

for command_name in k6 curl jq awk node; do
    if ! command -v "$command_name" >/dev/null 2>&1; then
        echo "필요한 명령을 찾을 수 없습니다: $command_name" >&2
        exit 2
    fi
done

if (( $(node -p 'Number(process.versions.node.split(".")[0])') < 18 )); then
    echo "SSE 클라이언트 실행에는 Node.js 18 이상이 필요합니다." >&2
    exit 2
fi

if [[ -z "$PSQL_BIN" ]]; then
    if command -v psql >/dev/null 2>&1; then
        PSQL_BIN=$(command -v psql)
    elif [[ -x /opt/homebrew/opt/libpq/bin/psql ]]; then
        PSQL_BIN=/opt/homebrew/opt/libpq/bin/psql
    else
        echo "필요한 명령을 찾을 수 없습니다: psql" >&2
        echo "Homebrew 사용 시 'brew install libpq'로 설치할 수 있습니다." >&2
        exit 2
    fi
fi

if [[ ! -x "$PSQL_BIN" ]]; then
    echo "PSQL_BIN이 실행 가능한 psql을 가리키지 않습니다: $PSQL_BIN" >&2
    exit 2
fi

IFS=',' read -r -a VUS <<< "$VU_LIST"
for vu in "${VUS[@]}"; do
    require_positive_integer VU "$vu"
done

REQUEST_HOST=${REQUEST_HOST%/}
MONITOR_HOST=${MONITOR_HOST%/}
GRAFANA_URL=${GRAFANA_URL%/}

RUN_ID=$(TZ=Asia/Seoul date +%Y%m%dT%H%M%S%z)
RESULT_DIR="$SCRIPT_DIR/results/${VERSION}_${RUN_ID}"
mkdir -p "$RESULT_DIR/logs" "$RESULT_DIR/summaries" \
    "$RESULT_DIR/fixtures" "$RESULT_DIR/sse" "$RESULT_DIR/lock-waits"

if [[ -z "$OUTPUT_FILE" ]]; then
    OUTPUT_FILE="$RESULT_DIR/results.csv"
elif [[ "$OUTPUT_FILE" != /* ]]; then
    OUTPUT_FILE="$PWD/$OUTPUT_FILE"
fi
mkdir -p "$(dirname -- "$OUTPUT_FILE")"

RUN_JSONL="$RESULT_DIR/runs.jsonl"
: > "$RUN_JSONL"
EVENT_OUTPUT_FILE="$RESULT_DIR/event-metrics.csv"
EVENT_JSONL="$RESULT_DIR/event-runs.jsonl"
: > "$EVENT_JSONL"
SSE_OUTPUT_FILE="$RESULT_DIR/sse-metrics.csv"
SSE_JSONL="$RESULT_DIR/sse-runs.jsonl"
: > "$SSE_JSONL"
SSE_CLIENT_PID=
LOCK_WAIT_COLLECTOR_PID=
SSE_CLEANUP_PENDING=false

cleanup_sse_client() {
    if [[ -n "$SSE_CLIENT_PID" ]] && kill -0 "$SSE_CLIENT_PID" 2>/dev/null; then
        kill "$SSE_CLIENT_PID" 2>/dev/null || true
        wait "$SSE_CLIENT_PID" 2>/dev/null || true
    fi
    SSE_CLIENT_PID=
}

cleanup_lock_wait_collector() {
    if [[ -n "$LOCK_WAIT_COLLECTOR_PID" ]]; then
        if kill -0 "$LOCK_WAIT_COLLECTOR_PID" 2>/dev/null; then
            kill "$LOCK_WAIT_COLLECTOR_PID" 2>/dev/null || true
        fi
        wait "$LOCK_WAIT_COLLECTOR_PID" 2>/dev/null || true
        LOCK_WAIT_COLLECTOR_PID=
    fi
}

request_sse_cleanup() {
    echo "앱 인스턴스의 SSE 연결을 정리합니다."
    curl -fsS -X POST \
        "$REQUEST_HOST/internal/sse/connections/cleanup" >/dev/null
    SSE_CLEANUP_PENDING=false
}

cleanup_server_sse_connections() {
    if [[ "$SSE_CLEANUP_PENDING" == true && -n "$REQUEST_HOST" ]]; then
        curl -fsS -X POST \
            "$REQUEST_HOST/internal/sse/connections/cleanup" \
            >/dev/null 2>&1 || true
        SSE_CLEANUP_PENDING=false
    fi
}

cleanup_collectors() {
    cleanup_sse_client
    cleanup_lock_wait_collector
    cleanup_server_sse_connections
}

trap cleanup_collectors EXIT INT TERM

csv_row() {
    local first=true
    local value escaped
    for value in "$@"; do
        escaped=${value//\"/\"\"}
        if [[ "$first" == true ]]; then
            first=false
        else
            printf ','
        fi
        printf '"%s"' "$escaped"
    done
    printf '\n'
}

format_decimal() {
    awk -v value="$1" 'BEGIN { printf "%.3f", value + 0 }'
}

format_tps() {
    awk -v value="$1" 'BEGIN { printf "%.3f req/s", value + 0 }'
}

format_count() {
    awk -v value="$1" '
        BEGIN {
            rounded = int(value + 0.5)
            if (value + 0 == rounded) {
                printf "%d", rounded
            } else {
                printf "%.3f", value + 0
            }
        }
    '
}

csv_row \
    "Record Type" "Target Type" "Target" "Version" \
    "Hikari Max Pool Size" "VU" "Run" "TPS (req/s)" \
    "System CPU Peak (%)" "Process CPU Peak (%)" "JVM Heap Peak (%)" \
    "Tomcat Available Threads Min" "Tomcat Available Threads Avg" \
    "Hikari Active Peak" "Avg Latency (ms)" "P95 (ms)" "P99 (ms)" \
    "Error Rate (%)" "DB Observed Lock Waits" "DB Lock Wait Total (ms)" \
    "DB Lock Wait Avg (ms)" "DB Lock Wait Max (ms)" \
    "DB Lock Wait Poll Interval (s)" \
    "초기 잔량" "사용자 총 채굴량" \
    "Mining Log 총 채굴량" "실제 잔량" "정합성" "Started At" "Finished At" \
    > "$OUTPUT_FILE"

csv_row \
    "Record Type" "Target Type" "Target" "Version" "VU" "Run" \
    "Producer TPS" "Consumer TPS" "Processed Messages" \
    "Mining DB Commit TPS" "PostgreSQL Commit TPS" \
    "Lag Peak" "Drain Time (s)" "E2E Avg (ms)" "E2E P95 (ms)" "E2E P99 (ms)" \
    "Consumer Hikari Active Peak" "Order Violations" \
    "Partition" "Partition Messages" \
    "Started At" "Load Finished At" "Drained At" \
    > "$EVENT_OUTPUT_FILE"

csv_row \
    "Record Type" "Version" "VU" "Run" "Status" "Connections" "Expected" \
    "Received" "Delivery Rate (%)" "Duplicate" "Invalid" \
    "Reconnects" "Connection Errors" \
    "Client E2E Avg (ms)" "Client E2E P95 (ms)" "Client E2E P99 (ms)" \
    "Client E2E Max (ms)" "SSE Drain Time (s)" "Started At" "Finished At" \
    > "$SSE_OUTPUT_FILE"

start_lock_wait_collector() {
    local output_file=$1
    local watch_sql

    csv_row \
        "Sampled Epoch" "Sampled At" "PID" "Backend XID" \
        "Virtual Transaction" "Wait Started At" \
        "Observed Wait (ms)" "Lock Type" "Mode" "Relation" "Page" "Tuple" \
        "Target Transaction ID" "Blocking PIDs" > "$output_file"

    watch_sql=$(cat <<SQL
SELECT
    EXTRACT(EPOCH FROM clock_timestamp()) AS sampled_epoch,
    clock_timestamp() AS sampled_at,
    l.pid,
    COALESCE(a.backend_xid::text, '') AS backend_xid,
    l.virtualtransaction,
    l.waitstart,
    round(EXTRACT(EPOCH FROM (clock_timestamp() - l.waitstart)) * 1000, 3)
        AS observed_wait_ms,
    l.locktype,
    l.mode,
    replace(COALESCE(l.relation::regclass::text, ''), ',', ' ') AS relation,
    COALESCE(l.page::text, '') AS page,
    COALESCE(l.tuple::text, '') AS tuple,
    COALESCE(l.transactionid::text, '') AS target_transaction_id,
    array_to_string(pg_blocking_pids(l.pid), ';') AS blocking_pids
FROM pg_locks l
JOIN pg_stat_activity a ON a.pid = l.pid
WHERE NOT l.granted
  AND l.waitstart IS NOT NULL
  AND a.datname = current_database()
  AND l.pid <> pg_backend_pid()
ORDER BY l.waitstart, l.pid;
\watch $LOCK_WAIT_POLL_SECONDS
SQL
)

    "$PSQL_BIN" "$DB_URL" \
        -X -q -t --csv \
        -v ON_ERROR_STOP=1 \
        <<< "$watch_sql" >> "$output_file" &
    LOCK_WAIT_COLLECTOR_PID=$!
}

stop_lock_wait_collector() {
    local collector_pid=$LOCK_WAIT_COLLECTOR_PID
    local collector_was_running=true

    if [[ -z "$collector_pid" ]]; then
        return
    fi

    if kill -0 "$collector_pid" 2>/dev/null; then
        kill "$collector_pid" 2>/dev/null || true
    else
        echo "경고: lock wait 수집기가 실행 중 먼저 종료되었습니다." >&2
        collector_was_running=false
    fi
    wait "$collector_pid" 2>/dev/null || true
    LOCK_WAIT_COLLECTOR_PID=

    [[ "$collector_was_running" == true ]]
}

summarize_lock_waits() {
    local input_file=$1
    local start_epoch=$2
    local end_epoch=$3
    local summary

    summary=$(awk -F ',' -v start_epoch="$start_epoch" -v end_epoch="$end_epoch" '
        NR == 1 { next }
        NF >= 14 && $1 + 0 >= start_epoch && $1 + 0 <= end_epoch {
            key = $3 SUBSEP $5 SUBSEP $6 SUBSEP $8 SUBSEP $9 SUBSEP \
                $10 SUBSEP $11 SUBSEP $12 SUBSEP $13
            duration = $7 + 0
            if (!(key in maximum) || duration > maximum[key]) {
                maximum[key] = duration
            }
        }
        END {
            count = 0
            total = 0
            max = 0
            for (key in maximum) {
                count++
                total += maximum[key]
                if (maximum[key] > max) {
                    max = maximum[key]
                }
            }
            average = count == 0 ? 0 : total / count
            printf "%d|%.3f|%.3f|%.3f\n", count, total, average, max
        }
    ' "$input_file")

    IFS='|' read -r LOCK_WAIT_COUNT LOCK_WAIT_TOTAL_MS LOCK_WAIT_AVG_MS \
        LOCK_WAIT_MAX_MS <<< "$summary"
}

prom_query() {
    local query=$1
    local evaluation_time=${2:-}
    local response

    if [[ -n "$evaluation_time" ]]; then
        response=$(curl -fsS --get "$MONITOR_HOST/api/v1/query" \
            --data-urlencode "query=$query" \
            --data-urlencode "time=$evaluation_time")
    else
        response=$(curl -fsS --get "$MONITOR_HOST/api/v1/query" \
            --data-urlencode "query=$query")
    fi

    jq -er --arg query "$query" '
            if .status != "success" then
                error($query + ": " + (.error // "Prometheus query failed"))
            elif (.data.result | length) != 1 then
                error($query + ": Prometheus query returned \(.data.result | length) series")
            else
                .data.result[0].value[1]
            end
        ' <<< "$response"
}

prom_query_by_instance() {
    local query=$1
    local evaluation_time=${2:-}
    local response

    if [[ -n "$evaluation_time" ]]; then
        response=$(curl -fsS --get "$MONITOR_HOST/api/v1/query" \
            --data-urlencode "query=$query" \
            --data-urlencode "time=$evaluation_time")
    else
        response=$(curl -fsS --get "$MONITOR_HOST/api/v1/query" \
            --data-urlencode "query=$query")
    fi

    jq -cer --arg query "$query" '
            if .status != "success" then
                error($query + ": " + (.error // "Prometheus query failed"))
            elif (.data.result | length) == 0 then
                error($query + ": Prometheus query returned no series")
            elif any(.data.result[]; .metric.instance == null) then
                error($query + ": Prometheus query returned a series without an instance label")
            else
                .data.result
                | map({
                    key: .metric.instance,
                    value: (.value[1] | tonumber)
                })
                | sort_by(.key)
                | from_entries
            end
        ' <<< "$response"
}

prom_query_by_instance_optional() {
    local query=$1
    local response

    response=$(curl -fsS --get "$MONITOR_HOST/api/v1/query" \
        --data-urlencode "query=$query")
    jq -cer --arg query "$query" '
        if .status != "success" then
            error($query + ": " + (.error // "Prometheus query failed"))
        elif any(.data.result[]; .metric.instance == null) then
            error($query + ": Prometheus query returned a series without an instance label")
        else
            .data.result
            | map({key: .metric.instance, value: (.value[1] | tonumber)})
            | sort_by(.key)
            | from_entries
        end
    ' <<< "$response"
}

prom_query_instances() {
    local query=$1
    local response

    response=$(curl -fsS --get "$MONITOR_HOST/api/v1/query" \
        --data-urlencode "query=$query")
    jq -cer --arg query "$query" '
        if .status != "success" then
            error($query + ": " + (.error // "Prometheus query failed"))
        elif any(.data.result[]; .metric.instance == null) then
            error($query + ": Prometheus query returned a series without an instance label")
        else
            [.data.result[].metric.instance] | unique | sort
        end
    ' <<< "$response"
}

normalize_instance_map() {
    local instances=$1
    local metrics=$2

    jq -cn --argjson instances "$instances" --argjson metrics "$metrics" '
        reduce $instances[] as $instance ({};
            .[$instance] = ($metrics[$instance] // 0)
        )
    '
}

prom_query_by_label() {
    local label=$1
    local query=$2
    curl -fsS --get "$MONITOR_HOST/api/v1/query" \
        --data-urlencode "query=$query" |
        jq -cer --arg label "$label" --arg query "$query" '
            if .status != "success" then
                error($query + ": " + (.error // "Prometheus query failed"))
            elif (.data.result | length) == 0 then
                {}
            elif any(.data.result[]; (.metric[$label] // "") == "") then
                error($query + ": Prometheus query returned a series without label " + $label)
            else
                .data.result
                | map({key: .metric[$label], value: (.value[1] | tonumber)})
                | sort_by(.key)
                | from_entries
            end
        '
}

wait_for_consumer_drain() {
    local load_finished_epoch=$1
    local deadline=$((SECONDS + DRAIN_TIMEOUT_SECONDS))
    local stable=0
    local request_lag completion_lag total_lag processed_count

    echo "Kafka consumer drain을 기다립니다..."
    while ((SECONDS < deadline)); do
        request_lag=$(prom_query \
            "(sum(clamp_min(kafka_consumergroup_lag{consumergroup=\"${KAFKA_CONSUMER_GROUP}\",topic=\"${KAFKA_REQUEST_TOPIC}\"}, 0)) and on() (min(kafka_consumergroup_lag{consumergroup=\"${KAFKA_CONSUMER_GROUP}\",topic=\"${KAFKA_REQUEST_TOPIC}\"}) >= 0)) or vector(-1)") || return 1
        completion_lag=$(prom_query \
            "(sum(clamp_min(kafka_consumergroup_lag{consumergroup=\"${KAFKA_COMPLETION_GROUP}\",topic=\"${KAFKA_COMPLETION_TOPIC}\"}, 0)) and on() (min(kafka_consumergroup_lag{consumergroup=\"${KAFKA_COMPLETION_GROUP}\",topic=\"${KAFKA_COMPLETION_TOPIC}\"}) >= 0)) or vector(-1)") || return 1
        total_lag=$(awk -v request="$request_lag" -v completion="$completion_lag" \
            'BEGIN { print request + completion }')
        printf '  lag(request/completion)=%.0f/%.0f' "$request_lag" "$completion_lag"

        if awk -v completion="$completion_lag" \
            'BEGIN { exit !(completion + 0 < 0) }'; then
            stable=0
            printf ' (측정 불가, 재시도)\n'
        elif awk -v request="$request_lag" \
            'BEGIN { exit !(request + 0 < 0) }'; then
            processed_count=$("$PSQL_BIN" "$DB_URL" -v ON_ERROR_STOP=1 -Atqc \
                "SELECT COUNT(*) FROM processed_mining_event WHERE mine_id = $MINE_ID")
            if ((processed_count >= PUBLISHED_EVENT_COUNT)) &&
                less_than_or_equal "$completion_lag" "$DRAIN_LAG_THRESHOLD"; then
                stable=$((stable + 1))
                printf ' (DB fallback %d/%d, drained %d/%d)\n' \
                    "$processed_count" "$PUBLISHED_EVENT_COUNT" \
                    "$stable" "$DRAIN_STABLE_SAMPLES"
            else
                stable=0
                printf ' (DB fallback %d/%d, processing)\n' \
                    "$processed_count" "$PUBLISHED_EVENT_COUNT"
            fi
        elif less_than_or_equal "$total_lag" "$DRAIN_LAG_THRESHOLD"; then
            stable=$((stable + 1))
            printf ' (drained %d/%d)\n' "$stable" "$DRAIN_STABLE_SAMPLES"
        else
            stable=0
            printf ' (processing)\n'
        fi

        if ((stable >= DRAIN_STABLE_SAMPLES)); then
            DRAINED_AT_EPOCH_MS=$(node -p 'Date.now()')
            DRAINED_AT_EPOCH=$((DRAINED_AT_EPOCH_MS / 1000))
            DRAINED_AT_EPOCH_PRECISE=$(awk \
                -v milliseconds="$DRAINED_AT_EPOCH_MS" \
                'BEGIN { printf "%.3f", milliseconds / 1000 }')
            DRAINED_AT=$(TZ=Asia/Seoul date +%Y-%m-%dT%H:%M:%S%z)
            DRAIN_TIME_SECONDS=$((DRAINED_AT_EPOCH - load_finished_epoch))
            return 0
        fi

        sleep "$DRAIN_POLL_SECONDS"
    done

    echo "consumer drain 제한 시간(${DRAIN_TIMEOUT_SECONDS}s)을 초과했습니다." >&2
    return 1
}

grafana_request() {
    if [[ -n "$GRAFANA_TOKEN" ]]; then
        curl -fsS \
            -H "Authorization: Bearer $GRAFANA_TOKEN" \
            "$@"
    else
        curl -fsS \
            -u "$GRAFANA_USER:$GRAFANA_PASSWORD" \
            "$@"
    fi
}

annotate_grafana() {
    local run_type=$1
    local vu=$2
    local run=$3
    local title
    local payload
    local response

    title="$VERSION | $SCENARIO | VU $vu | Run $run | $run_type"
    payload=$(jq -cn \
        --arg dashboardUid "$GRAFANA_DASHBOARD_UID" \
        --argjson time "$STARTED_AT_EPOCH_MS" \
        --argjson timeEnd "$FINISHED_AT_EPOCH_MS" \
        --arg title "$title" \
        --arg version "$VERSION" \
        --arg scenario "$SCENARIO" \
        --arg vu "$vu" \
        --arg run "$run" \
        --arg runType "$run_type" \
        '{
            dashboardUID: $dashboardUid,
            time: $time,
            timeEnd: $timeEnd,
            isRegion: true,
            tags: [
                "gold-rush-benchmark",
                ("version:" + $version),
                ("scenario:" + $scenario),
                ("vu:" + $vu),
                ("run:" + $run),
                ("type:" + $runType)
            ],
            text: $title
        }')

    response=$(grafana_request \
        -X POST \
        -H 'Content-Type: application/json' \
        --data "$payload" \
        "$GRAFANA_URL/api/annotations")

    if ! jq -e '.message == "Annotation added"' <<< "$response" >/dev/null; then
        echo "Grafana annotation 등록에 실패했습니다: $response" >&2
        return 1
    fi

    echo "Grafana 실행 구간을 등록했습니다: $title"
}

less_than_or_equal() {
    awk -v actual="$1" -v limit="$2" 'BEGIN { exit !(actual + 0 <= limit + 0) }'
}

wait_until_stable() {
    local deadline=$((SECONDS + STABLE_TIMEOUT_SECONDS))
    local consecutive=0
    local process_cpu system_cpu heap_pct hikari_active hikari_max hikari_pct tps
    local consumer_cpu consumer_heap consumer_hikari_active consumer_hikari_max
    local consumer_hikari_pct consumer_tps

    echo "시스템 안정화를 기다립니다..."
    while ((SECONDS < deadline)); do
        process_cpu=$(prom_query \
            "100 * max(process_cpu_usage{${PROM_LABELS}})") || return 1
        system_cpu=$(prom_query \
            "100 * max(system_cpu_usage{${PROM_LABELS}})") || return 1
        heap_pct=$(prom_query \
            "100 * sum(jvm_memory_used_bytes{area=\"heap\",${PROM_LABELS}}) / clamp_min(sum(jvm_memory_max_bytes{area=\"heap\",${PROM_LABELS}}), 1)") || return 1
        hikari_active=$(prom_query \
            "sum(hikaricp_connections_active{${PROM_LABELS}})") || return 1
        hikari_max=$(prom_query \
            "sum(hikaricp_connections_max{${PROM_LABELS}})") || return 1
        hikari_pct=$(awk -v active="$hikari_active" -v max="$hikari_max" \
            'BEGIN { if (max == 0) print 0; else printf "%.6f", 100 * active / max }')
        tps=$(prom_query \
            "sum(rate(http_server_requests_seconds_count{uri=\"${MINE_URI}\",method=\"POST\",${PROM_LABELS}}[30s]))") || return 1
        consumer_cpu=$(prom_query \
            "100 * max(process_cpu_usage{${CONSUMER_PROM_LABELS}})") || return 1
        consumer_heap=$(prom_query \
            "100 * sum(jvm_memory_used_bytes{area=\"heap\",${CONSUMER_PROM_LABELS}}) / clamp_min(sum(jvm_memory_max_bytes{area=\"heap\",${CONSUMER_PROM_LABELS}}), 1)") || return 1
        consumer_hikari_active=$(prom_query \
            "sum(hikaricp_connections_active{${CONSUMER_PROM_LABELS}})") || return 1
        consumer_hikari_max=$(prom_query \
            "sum(hikaricp_connections_max{${CONSUMER_PROM_LABELS}})") || return 1
        consumer_hikari_pct=$(awk -v active="$consumer_hikari_active" -v max="$consumer_hikari_max" \
            'BEGIN { if (max == 0) print 0; else printf "%.6f", 100 * active / max }')
        consumer_tps=$(prom_query \
            "sum(rate(gold_rush_consumer_mining_success_total{${CONSUMER_PROM_LABELS}}[30s]))") || return 1

        printf '  app CPU/system=%.2f%%/%.2f%%, heap=%.2f%%, Hikari=%.2f%%, TPS=%.3f' \
            "$process_cpu" "$system_cpu" "$heap_pct" "$hikari_pct" "$tps"
        printf ', consumer CPU=%.2f%%, heap=%.2f%%, Hikari=%.2f%%, TPS=%.3f' \
            "$consumer_cpu" "$consumer_heap" "$consumer_hikari_pct" "$consumer_tps"

        if less_than_or_equal "$process_cpu" "$STABLE_PROCESS_CPU_PCT" &&
            less_than_or_equal "$system_cpu" "$STABLE_SYSTEM_CPU_PCT" &&
            less_than_or_equal "$heap_pct" "$STABLE_HEAP_PCT" &&
            less_than_or_equal "$hikari_pct" "$STABLE_HIKARI_PCT" &&
            less_than_or_equal "$tps" "$STABLE_TPS" &&
            less_than_or_equal "$consumer_cpu" "$STABLE_PROCESS_CPU_PCT" &&
            less_than_or_equal "$consumer_heap" "$STABLE_HEAP_PCT" &&
            less_than_or_equal "$consumer_hikari_pct" "$STABLE_HIKARI_PCT" &&
            less_than_or_equal "$consumer_tps" "$STABLE_TPS"; then
            consecutive=$((consecutive + 1))
            printf ' (안정 %d/%d)\n' "$consecutive" "$STABLE_SAMPLES"
            if ((consecutive >= STABLE_SAMPLES)); then
                echo "실험 구간 분리를 위해 ${POST_STABLE_COOLDOWN_SECONDS}초 추가 대기합니다."
                sleep "$POST_STABLE_COOLDOWN_SECONDS"
                return 0
            fi
        else
            consecutive=0
            printf ' (대기)\n'
        fi

        sleep "$STABLE_POLL_SECONDS"
    done

    echo "안정화 제한 시간(${STABLE_TIMEOUT_SECONDS}s)을 초과했습니다." >&2
    return 1
}

collect_peak_metrics() {
    local start_epoch=$1
    local load_finished_epoch=$2
    local scraped_epoch=$3
    local load_window=$((load_finished_epoch - start_epoch))
    local peak_window=$((scraped_epoch - start_epoch))
    local backend_request_after_by_instance

    if ((load_window < 1)); then
        load_window=1
    fi
    if ((peak_window < 1)); then
        peak_window=1
    fi

    SYSTEM_CPU_PEAK=$(prom_query \
        "max_over_time((100 * max(system_cpu_usage{${PROM_LABELS}}))[${peak_window}s:5s])" \
        "$scraped_epoch")
    PROCESS_CPU_PEAK=$(prom_query \
        "max_over_time((100 * max(process_cpu_usage{${PROM_LABELS}}))[${peak_window}s:5s])" \
        "$scraped_epoch")
    JVM_HEAP_PEAK=$(prom_query \
        "max_over_time((100 * sum(jvm_memory_used_bytes{area=\"heap\",${PROM_LABELS}}) / clamp_min(sum(jvm_memory_max_bytes{area=\"heap\",${PROM_LABELS}}), 1))[${peak_window}s:5s])" \
        "$scraped_epoch")
    HIKARI_ACTIVE_PEAK=$(prom_query \
        "max_over_time((sum(hikaricp_connections_active{${PROM_LABELS}}))[${peak_window}s:5s])" \
        "$scraped_epoch")
    HIKARI_MAX=$(prom_query \
        "sum(hikaricp_connections_max{${PROM_LABELS}})" "$scraped_epoch")
    backend_request_after_by_instance=$(prom_query_by_instance_optional \
        "sum by (instance) (http_server_requests_seconds_count{uri=\"${MINE_URI}\",method=\"POST\",${PROM_LABELS}})")
    BACKEND_TPS_BY_INSTANCE=$(jq -cn \
        --argjson instances "$APP_INSTANCES" \
        --argjson before "$BACKEND_REQUEST_BASELINE_BY_INSTANCE" \
        --argjson after "$backend_request_after_by_instance" \
        --argjson duration "$BENCHMARK_DURATION_SECONDS" '
            reduce $instances[] as $instance ({};
                .[$instance] = (
                    ([((($after[$instance] // 0) - ($before[$instance] // 0))), 0] | max)
                    / $duration
                )
            )
        ')
    SYSTEM_CPU_PEAK_BY_INSTANCE=$(prom_query_by_instance \
        "max_over_time((100 * system_cpu_usage{${PROM_LABELS}})[${peak_window}s:5s])" \
        "$scraped_epoch")
    PROCESS_CPU_PEAK_BY_INSTANCE=$(prom_query_by_instance \
        "max_over_time((100 * process_cpu_usage{${PROM_LABELS}})[${peak_window}s:5s])" \
        "$scraped_epoch")
    JVM_HEAP_PEAK_BY_INSTANCE=$(prom_query_by_instance \
        "max_over_time((100 * sum by (instance) (jvm_memory_used_bytes{area=\"heap\",${PROM_LABELS}}) / clamp_min(sum by (instance) (jvm_memory_max_bytes{area=\"heap\",${PROM_LABELS}}), 1))[${peak_window}s:5s])" \
        "$scraped_epoch")
    TOMCAT_AVAILABLE_THREADS_MIN_BY_INSTANCE=$(prom_query_by_instance \
        "min_over_time((clamp_min(sum by (instance) (tomcat_threads_config_max_threads{${PROM_LABELS}}) - sum by (instance) (tomcat_threads_busy_threads{${PROM_LABELS}}), 0))[${peak_window}s:1s])" \
        "$scraped_epoch")
    TOMCAT_AVAILABLE_THREADS_AVG_BY_INSTANCE=$(prom_query_by_instance \
        "avg_over_time((clamp_min(sum by (instance) (tomcat_threads_config_max_threads{${PROM_LABELS}}) - sum by (instance) (tomcat_threads_busy_threads{${PROM_LABELS}}), 0))[${load_window}s:1s])" \
        "$load_finished_epoch")
}

collect_event_metrics() {
    local start_epoch=$1
    local end_epoch=$2
    local window=$((end_epoch - start_epoch + PROM_SCRAPE_DELAY))
    local event_duration=$((end_epoch - start_epoch))

    if ((window < 10)); then
        window=10
    fi
    if ((event_duration < 1)); then
        event_duration=1
    fi

    PRODUCER_TPS=$(awk \
        -v count="$PUBLISHED_EVENT_COUNT" \
        -v duration="$BENCHMARK_DURATION_SECONDS" \
        'BEGIN { printf "%.6f", count / duration }')
    CONSUMER_TPS=$(prom_query \
        "(sum(increase(gold_rush_consumer_mining_success_total{${CONSUMER_PROM_LABELS}}[${window}s])) or vector(0)) / ${event_duration}")
    CONSUMER_PROCESSED_COUNT=$(prom_query \
        "sum(increase(gold_rush_consumer_mining_success_total{${CONSUMER_PROM_LABELS}}[${window}s])) or vector(0)")
    MINING_DB_COMMIT_TPS=$(prom_query \
        "(sum(increase(gold_rush_consumer_mining_db_commit_total{${CONSUMER_PROM_LABELS}}[${window}s])) or vector(0)) / ${event_duration}")
    POSTGRES_COMMIT_TPS=$(prom_query \
        "sum(increase(pg_stat_database_xact_commit{datname=\"${POSTGRES_DATABASE}\",${POSTGRES_PROM_LABELS}}[${window}s])) / ${event_duration}")
    LAG_PEAK=$(prom_query \
        "max_over_time((sum(clamp_min(kafka_consumergroup_lag{consumergroup=\"${KAFKA_CONSUMER_GROUP}\",topic=\"${KAFKA_REQUEST_TOPIC}\"}, 0)) + sum(clamp_min(kafka_consumergroup_lag{consumergroup=\"${KAFKA_COMPLETION_GROUP}\",topic=\"${KAFKA_COMPLETION_TOPIC}\"}, 0)))[${window}s:1s])")

    E2E_AVG_MS=$(prom_query \
        "1000 * sum(increase(gold_rush_mining_end_to_end_seconds_sum{${PROM_LABELS}}[${window}s])) / clamp_min(sum(increase(gold_rush_mining_end_to_end_seconds_count{${PROM_LABELS}}[${window}s])), 1)")
    E2E_P95_MS=$(prom_query \
        "1000 * histogram_quantile(0.95, sum by (le) (increase(gold_rush_mining_end_to_end_seconds_bucket{${PROM_LABELS}}[${window}s])))")
    E2E_P99_MS=$(prom_query \
        "1000 * histogram_quantile(0.99, sum by (le) (increase(gold_rush_mining_end_to_end_seconds_bucket{${PROM_LABELS}}[${window}s])))")

    CONSUMER_HIKARI_ACTIVE_PEAK=$(prom_query \
        "max_over_time((sum(hikaricp_connections_active{${CONSUMER_PROM_LABELS}}))[${window}s:1s])")
    ORDER_VIOLATIONS=$(prom_query \
        "sum(increase(gold_rush_consumer_mining_order_violation_total{${CONSUMER_PROM_LABELS}}[${window}s])) or vector(0)")

    CONSUMER_TPS_BY_INSTANCE=$(prom_query_by_instance_optional \
        "sum by (instance) (increase(gold_rush_consumer_mining_success_total{${CONSUMER_PROM_LABELS}}[${window}s])) / ${event_duration}")
    CONSUMER_TPS_BY_INSTANCE=$(normalize_instance_map \
        "$CONSUMER_INSTANCES" "$CONSUMER_TPS_BY_INSTANCE")
    CONSUMER_COUNT_BY_INSTANCE=$(prom_query_by_instance_optional \
        "sum by (instance) (increase(gold_rush_consumer_mining_success_total{${CONSUMER_PROM_LABELS}}[${window}s]))")
    CONSUMER_COUNT_BY_INSTANCE=$(normalize_instance_map \
        "$CONSUMER_INSTANCES" "$CONSUMER_COUNT_BY_INSTANCE")
    MINING_DB_COMMIT_TPS_BY_INSTANCE=$(prom_query_by_instance_optional \
        "sum by (instance) (increase(gold_rush_consumer_mining_db_commit_total{${CONSUMER_PROM_LABELS}}[${window}s])) / ${event_duration}")
    MINING_DB_COMMIT_TPS_BY_INSTANCE=$(normalize_instance_map \
        "$CONSUMER_INSTANCES" "$MINING_DB_COMMIT_TPS_BY_INSTANCE")
    CONSUMER_HIKARI_PEAK_BY_INSTANCE=$(prom_query_by_instance_optional \
        "max_over_time((sum by (instance) (hikaricp_connections_active{${CONSUMER_PROM_LABELS}}))[${window}s:1s])")
    CONSUMER_HIKARI_PEAK_BY_INSTANCE=$(normalize_instance_map \
        "$CONSUMER_INSTANCES" "$CONSUMER_HIKARI_PEAK_BY_INSTANCE")
    MESSAGES_BY_PARTITION=$(prom_query_by_label "partition" \
        "sum by (partition) (increase(gold_rush_consumer_mining_received_total{${CONSUMER_PROM_LABELS}}[${window}s]))")
}

append_partitioned_event_rows() {
    local messages_by_partition=$1
    local started_at=$2
    local load_finished_at=$3
    local drained_at=$4
    local partition_rows
    local partition
    local messages
    shift 4

    partition_rows=$(jq -r '
        to_entries
        | sort_by(.key | tonumber)
        | .[]
        | [.key, .value]
        | @tsv
    ' <<< "$messages_by_partition")

    if [[ -z "$partition_rows" ]]; then
        csv_row "$@" "" "" \
            "$started_at" "$load_finished_at" "$drained_at" \
            >> "$EVENT_OUTPUT_FILE"
        return
    fi

    while IFS=$'\t' read -r partition messages; do
        csv_row "$@" "$partition" "$(format_count "$messages")" \
            "$started_at" "$load_finished_at" "$drained_at" \
            >> "$EVENT_OUTPUT_FILE"
    done <<< "$partition_rows"
}

append_event_metrics() {
    local record_type=$1
    local vu=$2
    local run=$3

    append_partitioned_event_rows \
        "$MESSAGES_BY_PARTITION" "$STARTED_AT" "$LOAD_FINISHED_AT" "$DRAINED_AT" \
        "$record_type" "PIPELINE" "ALL" "$VERSION" "$vu" "$run" \
        "$(format_decimal "$PRODUCER_TPS")" \
        "$(format_decimal "$CONSUMER_TPS")" \
        "$(format_count "$CONSUMER_PROCESSED_COUNT")" \
        "$(format_decimal "$MINING_DB_COMMIT_TPS")" \
        "$(format_decimal "$POSTGRES_COMMIT_TPS")" \
        "$(format_count "$LAG_PEAK")" \
        "$(format_decimal "$DRAIN_TIME_SECONDS")" \
        "$(format_decimal "$E2E_AVG_MS")" \
        "$(format_decimal "$E2E_P95_MS")" \
        "$(format_decimal "$E2E_P99_MS")" \
        "$(format_count "$CONSUMER_HIKARI_ACTIVE_PEAK")" \
        "$(format_count "$ORDER_VIOLATIONS")"

    append_consumer_event_rows \
        "$record_type" "$vu" "$run" \
        "$STARTED_AT" "$LOAD_FINISHED_AT" "$DRAINED_AT" \
        "$CONSUMER_TPS_BY_INSTANCE" "$CONSUMER_COUNT_BY_INSTANCE" \
        "$MINING_DB_COMMIT_TPS_BY_INSTANCE" \
        "$CONSUMER_HIKARI_PEAK_BY_INSTANCE"

    if [[ "$record_type" == "RUN" ]]; then
        jq -cn \
            --argjson vu "$vu" \
            --argjson producerTps "$PRODUCER_TPS" \
            --argjson consumerTps "$CONSUMER_TPS" \
            --argjson consumerProcessedCount "$CONSUMER_PROCESSED_COUNT" \
            --argjson miningDbCommitTps "$MINING_DB_COMMIT_TPS" \
            --argjson postgresCommitTps "$POSTGRES_COMMIT_TPS" \
            --argjson lagPeak "$LAG_PEAK" \
            --argjson drainTime "$DRAIN_TIME_SECONDS" \
            --argjson e2eAvg "$E2E_AVG_MS" \
            --argjson e2eP95 "$E2E_P95_MS" \
            --argjson e2eP99 "$E2E_P99_MS" \
            --argjson consumerHikari "$CONSUMER_HIKARI_ACTIVE_PEAK" \
            --argjson orderViolations "$ORDER_VIOLATIONS" \
            --argjson consumerByInstance "$CONSUMER_TPS_BY_INSTANCE" \
            --argjson consumerCountByInstance "$CONSUMER_COUNT_BY_INSTANCE" \
            --argjson miningDbCommitByInstance "$MINING_DB_COMMIT_TPS_BY_INSTANCE" \
            --argjson consumerHikariByInstance "$CONSUMER_HIKARI_PEAK_BY_INSTANCE" \
            --argjson messagesByPartition "$MESSAGES_BY_PARTITION" \
            '{
                vu: $vu,
                producerTps: $producerTps,
                consumerTps: $consumerTps,
                consumerProcessedCount: $consumerProcessedCount,
                miningDbCommitTps: $miningDbCommitTps,
                postgresCommitTps: $postgresCommitTps,
                lagPeak: $lagPeak,
                drainTime: $drainTime,
                e2eAvg: $e2eAvg,
                e2eP95: $e2eP95,
                e2eP99: $e2eP99,
                consumerHikari: $consumerHikari,
                orderViolations: $orderViolations,
                consumerByInstance: $consumerByInstance,
                consumerCountByInstance: $consumerCountByInstance,
                miningDbCommitByInstance: $miningDbCommitByInstance,
                consumerHikariByInstance: $consumerHikariByInstance,
                messagesByPartition: $messagesByPartition
            }' >> "$EVENT_JSONL"
    fi
}

append_consumer_event_rows() {
    local record_type=$1
    local vu=$2
    local run=$3
    local started_at=$4
    local load_finished_at=$5
    local drained_at=$6
    local tps_by_instance=$7
    local count_by_instance=$8
    local db_commit_by_instance=$9
    local hikari_by_instance=${10}
    local instance
    local instance_tps
    local instance_count
    local instance_db_commit_tps
    local instance_hikari

    while IFS= read -r instance; do
        instance_tps=$(jq -er --arg instance "$instance" '.[$instance]' \
            <<< "$tps_by_instance")
        instance_count=$(jq -er --arg instance "$instance" '.[$instance]' \
            <<< "$count_by_instance")
        instance_db_commit_tps=$(jq -er --arg instance "$instance" '.[$instance]' \
            <<< "$db_commit_by_instance")
        instance_hikari=$(jq -er --arg instance "$instance" '.[$instance]' \
            <<< "$hikari_by_instance")

        csv_row \
            "$record_type" "CONSUMER" "$instance" "$VERSION" "$vu" "$run" \
            "" "$(format_decimal "$instance_tps")" \
            "$(format_count "$instance_count")" \
            "$(format_decimal "$instance_db_commit_tps")" \
            "" "" "" "" "" "" \
            "$(format_count "$instance_hikari")" "" "" "" \
            "$started_at" "$load_finished_at" "$drained_at" \
            >> "$EVENT_OUTPUT_FILE"
    done < <(jq -r 'keys[]' <<< "$tps_by_instance")
}

append_event_average() {
    local vu=$1
    local average

    average=$(jq -sc --argjson vu "$vu" '
        map(select(.vu == $vu)) as $rows
        | ($rows | length) as $count
        | def mean(field): ($rows | map(.[field]) | add / $count);
        def mean_map(field):
            reduce ($rows | map(.[field])[]) as $metrics ({};
                reduce ($metrics | to_entries[]) as $metric (.;
                    .[$metric.key] = ((.[$metric.key] // []) + [$metric.value])
                )
            )
            | with_entries(.value = (.value | add / length));
        {
            producerTps: mean("producerTps"),
            consumerTps: mean("consumerTps"),
            consumerProcessedCount: mean("consumerProcessedCount"),
            miningDbCommitTps: mean("miningDbCommitTps"),
            postgresCommitTps: mean("postgresCommitTps"),
            lagPeak: mean("lagPeak"),
            drainTime: mean("drainTime"),
            e2eAvg: mean("e2eAvg"),
            e2eP95: mean("e2eP95"),
            e2eP99: mean("e2eP99"),
            consumerHikari: mean("consumerHikari"),
            orderViolations: mean("orderViolations"),
            consumerByInstance: mean_map("consumerByInstance"),
            consumerCountByInstance: mean_map("consumerCountByInstance"),
            miningDbCommitByInstance: mean_map("miningDbCommitByInstance"),
            consumerHikariByInstance: mean_map("consumerHikariByInstance"),
            messagesByPartition: mean_map("messagesByPartition")
        }
    ' "$EVENT_JSONL")

    append_partitioned_event_rows \
        "$(jq -c '.messagesByPartition' <<< "$average")" "" "" "" \
        "AVERAGE" "PIPELINE" "ALL" "$VERSION" "$vu" "AVERAGE" \
        "$(format_decimal "$(jq -r '.producerTps' <<< "$average")")" \
        "$(format_decimal "$(jq -r '.consumerTps' <<< "$average")")" \
        "$(format_count "$(jq -r '.consumerProcessedCount' <<< "$average")")" \
        "$(format_decimal "$(jq -r '.miningDbCommitTps' <<< "$average")")" \
        "$(format_decimal "$(jq -r '.postgresCommitTps' <<< "$average")")" \
        "$(format_count "$(jq -r '.lagPeak' <<< "$average")")" \
        "$(format_decimal "$(jq -r '.drainTime' <<< "$average")")" \
        "$(format_decimal "$(jq -r '.e2eAvg' <<< "$average")")" \
        "$(format_decimal "$(jq -r '.e2eP95' <<< "$average")")" \
        "$(format_decimal "$(jq -r '.e2eP99' <<< "$average")")" \
        "$(format_count "$(jq -r '.consumerHikari' <<< "$average")")" \
        "$(format_count "$(jq -r '.orderViolations' <<< "$average")")"

    append_consumer_event_rows \
        "AVERAGE" "$vu" "AVERAGE" "" "" "" \
        "$(jq -c '.consumerByInstance' <<< "$average")" \
        "$(jq -c '.consumerCountByInstance' <<< "$average")" \
        "$(jq -c '.miningDbCommitByInstance' <<< "$average")" \
        "$(jq -c '.consumerHikariByInstance' <<< "$average")"
}

append_sse_metrics() {
    local record_type=$1
    local vu=$2
    local run=$3

    csv_row \
        "$record_type" "$VERSION" "$vu" "$run" \
        "$SSE_STATUS" "$SSE_CONNECTIONS" "$SSE_EXPECTED" "$SSE_RECEIVED" \
        "$(format_decimal "$SSE_DELIVERY_RATE")" \
        "$SSE_DUPLICATE_COUNT" "$SSE_INVALID_COUNT" \
        "$SSE_RECONNECT_COUNT" "$SSE_CONNECTION_ERROR_COUNT" \
        "$(format_decimal "$SSE_E2E_AVG_MS")" \
        "$(format_decimal "$SSE_E2E_P95_MS")" \
        "$(format_decimal "$SSE_E2E_P99_MS")" \
        "$(format_decimal "$SSE_E2E_MAX_MS")" \
        "$(format_decimal "$SSE_DRAIN_TIME_SECONDS")" \
        "$STARTED_AT" "$FINISHED_AT" >> "$SSE_OUTPUT_FILE"

    if [[ "$record_type" == "RUN" ]]; then
        jq -cn \
            --argjson vu "$vu" \
            --arg status "$SSE_STATUS" \
            --argjson connections "$SSE_CONNECTIONS" \
            --argjson expected "$SSE_EXPECTED" \
            --argjson received "$SSE_RECEIVED" \
            --argjson deliveryRate "$SSE_DELIVERY_RATE" \
            --argjson duplicates "$SSE_DUPLICATE_COUNT" \
            --argjson invalid "$SSE_INVALID_COUNT" \
            --argjson reconnects "$SSE_RECONNECT_COUNT" \
            --argjson connectionErrors "$SSE_CONNECTION_ERROR_COUNT" \
            --argjson avg "$SSE_E2E_AVG_MS" \
            --argjson p95 "$SSE_E2E_P95_MS" \
            --argjson p99 "$SSE_E2E_P99_MS" \
            --argjson max "$SSE_E2E_MAX_MS" \
            --argjson drainTime "$SSE_DRAIN_TIME_SECONDS" \
            '{vu: $vu, status: $status,
              connections: $connections, expected: $expected,
              received: $received, deliveryRate: $deliveryRate,
              duplicates: $duplicates, invalid: $invalid,
              reconnects: $reconnects, connectionErrors: $connectionErrors, avg: $avg,
              p95: $p95, p99: $p99, max: $max, drainTime: $drainTime}' \
            >> "$SSE_JSONL"
    fi
}

append_sse_average() {
    local vu=$1
    local average

    average=$(jq -sc --argjson vu "$vu" '
        map(select(.vu == $vu)) as $rows
        | ($rows | length) as $count
        | def mean(field): ($rows | map(.[field]) | add / $count);
        {status: (if all($rows[]; .status == "completed") then "completed" else "incomplete" end),
         connections: mean("connections"), expected: mean("expected"),
         received: mean("received"), deliveryRate: mean("deliveryRate"),
         duplicates: mean("duplicates"), invalid: mean("invalid"),
         reconnects: mean("reconnects"), connectionErrors: mean("connectionErrors"),
         avg: mean("avg"), p95: mean("p95"), p99: mean("p99"),
         max: mean("max"), drainTime: mean("drainTime")}
    ' "$SSE_JSONL")

    csv_row \
        "AVERAGE" "$VERSION" "$vu" "AVERAGE" \
        "$(jq -r '.status' <<< "$average")" \
        "$(format_count "$(jq -r '.connections' <<< "$average")")" \
        "$(format_count "$(jq -r '.expected' <<< "$average")")" \
        "$(format_count "$(jq -r '.received' <<< "$average")")" \
        "$(format_decimal "$(jq -r '.deliveryRate' <<< "$average")")" \
        "$(format_count "$(jq -r '.duplicates' <<< "$average")")" \
        "$(format_count "$(jq -r '.invalid' <<< "$average")")" \
        "$(format_count "$(jq -r '.reconnects' <<< "$average")")" \
        "$(format_count "$(jq -r '.connectionErrors' <<< "$average")")" \
        "$(format_decimal "$(jq -r '.avg' <<< "$average")")" \
        "$(format_decimal "$(jq -r '.p95' <<< "$average")")" \
        "$(format_decimal "$(jq -r '.p99' <<< "$average")")" \
        "$(format_decimal "$(jq -r '.max' <<< "$average")")" \
        "$(format_decimal "$(jq -r '.drainTime' <<< "$average")")" \
        "" "" >> "$SSE_OUTPUT_FILE"
}

verify_database() {
    local verification
    verification=$("$PSQL_BIN" "$DB_URL" \
        -v ON_ERROR_STOP=1 \
        -v mine_id="$MINE_ID" \
        -v initial_amount="$CURRENT_MINE_AMOUNT" \
        -At -F '|' \
        -f "$SCRIPT_DIR/sql/verify.sql")

    IFS='|' read -r INITIAL_REMAINING USER_TOTAL_MINED LOG_TOTAL_MINED \
        ACTUAL_REMAINING CONSISTENT <<< "$verification"

    if [[ "$CONSISTENT" == "t" ]]; then
        CONSISTENCY="✅"
    else
        CONSISTENCY="❌"
    fi
}

reset_database() {
    echo "DB를 초기화합니다: processed_mining_event, app_user, mine, mining_log"
    "$PSQL_BIN" "$DB_URL" \
        -v ON_ERROR_STOP=1 \
        -f "$SCRIPT_DIR/sql/reset.sql"
}

preflight() {
    local app_status
    local prometheus_up
    local database_status
    local grafana_status
    local dependency_up
    local schema_status
    local app_instances
    local metric_instances
    local metric_name

    echo "애플리케이션, Prometheus, PostgreSQL 연결을 확인합니다."

    app_status=$(curl -fsS "$REQUEST_HOST/actuator/health" | jq -er '.status')
    if [[ "$app_status" != "UP" ]]; then
        echo "애플리케이션 상태가 UP이 아닙니다: $app_status" >&2
        return 1
    fi

    prometheus_up=$(prom_query "max(up{${PROM_LABELS}})")
    if ! awk -v value="$prometheus_up" 'BEGIN { exit !(value + 0 == 1) }'; then
        echo "Prometheus에서 애플리케이션 target이 UP이 아닙니다: $prometheus_up" >&2
        return 1
    fi

    app_instances=$(prom_query_instances \
        "max by (instance) (up{${PROM_LABELS}} == 1)")
    APP_INSTANCES=$app_instances

    for metric_name in \
        tomcat_threads_config_max_threads \
        tomcat_threads_busy_threads; do
        metric_instances=$(prom_query_instances \
            "count by (instance) (${metric_name}{${PROM_LABELS}})")
        if ! jq -en \
            --argjson app "$app_instances" \
            --argjson metric "$metric_instances" \
            '$app == $metric' >/dev/null; then
            echo "Tomcat 지표의 백엔드 인스턴스 집합이 일치하지 않습니다: $metric_name" >&2
            echo "  UP instances: $app_instances" >&2
            echo "  metric instances: $metric_instances" >&2
            echo "server.tomcat.mbeanregistry.enabled=true 설정과 앱 재배포가 필요합니다." >&2
            return 1
        fi
    done

    for target_labels in \
        "$CONSUMER_PROM_LABELS" \
        'job="gold-rush-kafka"' \
        "$POSTGRES_PROM_LABELS"; do
        dependency_up=$(prom_query "max(up{${target_labels}})")
        if ! awk -v value="$dependency_up" 'BEGIN { exit !(value + 0 == 1) }'; then
            echo "Prometheus target이 UP이 아닙니다: $target_labels" >&2
            return 1
        fi
    done

    CONSUMER_INSTANCES=$(prom_query_instances \
        "max by (instance) (up{${CONSUMER_PROM_LABELS}} == 1)")

    database_status=$("$PSQL_BIN" "$DB_URL" -v ON_ERROR_STOP=1 -Atqc 'SELECT 1')
    if [[ "$database_status" != "1" ]]; then
        echo "PostgreSQL 연결 확인에 실패했습니다." >&2
        return 1
    fi

    schema_status=$("$PSQL_BIN" "$DB_URL" -v ON_ERROR_STOP=1 -Atqc \
        "SELECT COUNT(*) FROM information_schema.columns WHERE table_name='processed_mining_event' AND column_name='requested_at'")
    if [[ "$schema_status" != "1" ]]; then
        echo "processed_mining_event.requested_at 컬럼이 없습니다. 최신 DDL을 적용해야 합니다." >&2
        return 1
    fi

    grafana_status=$(grafana_request "$GRAFANA_URL/api/health" | jq -er '.database')
    if [[ "$grafana_status" != "ok" ]]; then
        echo "Grafana 상태가 정상이 아닙니다: $grafana_status" >&2
        return 1
    fi

    grafana_request "$GRAFANA_URL/api/annotations?limit=1" |
        jq -e 'type == "array"' >/dev/null
}

provision_test_fixture() {
    local vu=$1
    local fixture_file=$2
    local session_lines="${fixture_file}.sessions"
    local mine_response signin_response mine_id session_id

    mine_response=$(curl -fsS -X POST \
        "$REQUEST_HOST/mines?amount=$CURRENT_MINE_AMOUNT")
    mine_id=$(jq -er \
        'select(.success == true) | .data.mineId | select(type == "number")' \
        <<< "$mine_response")

    : > "$session_lines"
    for ((index = 1; index <= vu; index += 1)); do
        signin_response=$(curl -fsS -X POST \
            "$REQUEST_HOST/user/signin?mineId=$mine_id")
        session_id=$(jq -er \
            'select(.success == true) | .data.sessionId | select(type == "string")' \
            <<< "$signin_response")
        printf '%s\n' "$session_id" >> "$session_lines"
    done

    jq -Rs --argjson mineId "$mine_id" \
        '{mineId: $mineId, sessions: (split("\n") | map(select(length > 0)))}' \
        "$session_lines" > "$fixture_file"
    rm "$session_lines"
    MINE_ID=$mine_id
}

start_sse_client() {
    local vu=$1
    local fixture_file=$2
    local ready_file=$3
    local summary_file=$4
    local log_file=$5
    local expected_file=$6
    local complete_file=$7
    local deadline=$((SECONDS + SSE_READY_TIMEOUT_SECONDS))
    local connections

    node "$SCRIPT_DIR/sse-client.mjs" \
        --base-url "$REQUEST_HOST" \
        --sessions-file "$fixture_file" \
        --ready-file "$ready_file" \
        --summary-file "$summary_file" \
        --expected-file "$expected_file" \
        --complete-file "$complete_file" \
        --timeout-seconds "$SSE_CLIENT_TIMEOUT_SECONDS" \
        > "$log_file" 2>&1 &
    SSE_CLIENT_PID=$!

    while ((SECONDS < deadline)); do
        if [[ -f "$ready_file" ]]; then
            connections=$(jq -er '.connections' "$ready_file")
            if ((connections == vu)); then
                echo "SSE 연결 준비 완료: ${connections}개"
                return 0
            fi
        fi
        if ! kill -0 "$SSE_CLIENT_PID" 2>/dev/null; then
            wait "$SSE_CLIENT_PID" || true
            SSE_CLIENT_PID=
            echo "SSE 클라이언트가 연결 준비 전에 종료되었습니다: $log_file" >&2
            tail -n 40 "$log_file" >&2
            return 1
        fi
        sleep "$SSE_READY_POLL_SECONDS"
    done

    echo "SSE 연결 준비 제한 시간(${SSE_READY_TIMEOUT_SECONDS}s)을 초과했습니다." >&2
    cleanup_sse_client
    return 1
}

wait_for_sse_client() {
    local summary_file=$1
    local log_file=$2
    local load_finished_epoch=$3
    local sse_finished_epoch

    if ! wait "$SSE_CLIENT_PID"; then
        SSE_CLIENT_PID=
        echo "SSE 결과 수신에 실패했습니다: $log_file" >&2
        tail -n 40 "$log_file" >&2
        return 1
    fi
    SSE_CLIENT_PID=

    SSE_STATUS=$(jq -er '.status' "$summary_file")
    if [[ "$SSE_STATUS" != "completed" && "$SSE_STATUS" != "incomplete" ]]; then
        echo "SSE 결과 상태가 유효하지 않습니다: $SSE_STATUS ($summary_file)" >&2
        return 1
    fi

    SSE_CONNECTIONS=$(jq -er '.connections' "$summary_file")
    SSE_EXPECTED=$(jq -er '.expected' "$summary_file")
    SSE_RECEIVED=$(jq -er '.received' "$summary_file")
    SSE_DELIVERY_RATE=$(jq -er '.deliveryRate * 100' "$summary_file")
    SSE_DUPLICATE_COUNT=$(jq -er '.duplicateCount' "$summary_file")
    SSE_INVALID_COUNT=$(jq -er '.invalidCount' "$summary_file")
    SSE_RECONNECT_COUNT=$(jq -er '.reconnectCount' "$summary_file")
    SSE_CONNECTION_ERROR_COUNT=$(jq -er '.connectionErrorCount' "$summary_file")
    SSE_E2E_AVG_MS=$(jq -er '.latency.avg' "$summary_file")
    SSE_E2E_P95_MS=$(jq -er '.latency.p95' "$summary_file")
    SSE_E2E_P99_MS=$(jq -er '.latency.p99' "$summary_file")
    SSE_E2E_MAX_MS=$(jq -er '.latency.max' "$summary_file")
    sse_finished_epoch=$(date +%s)
    SSE_DRAIN_TIME_SECONDS=$((sse_finished_epoch - load_finished_epoch))
}

run_k6() {
    local vu=$1
    local run=$2
    local iterations=$3
    local prefix=$4
    local summary_file="$RESULT_DIR/summaries/${prefix}_vu${vu}_run${run}.json"
    local log_file="$RESULT_DIR/logs/${prefix}_vu${vu}_run${run}.log"
    local fixture_file="$RESULT_DIR/fixtures/${prefix}_vu${vu}_run${run}.json"
    local sse_ready_file="$RESULT_DIR/sse/${prefix}_vu${vu}_run${run}.ready.json"
    local sse_summary_file="$RESULT_DIR/sse/${prefix}_vu${vu}_run${run}.summary.json"
    local sse_log_file="$RESULT_DIR/sse/${prefix}_vu${vu}_run${run}.log"
    local sse_expected_file="$RESULT_DIR/sse/${prefix}_vu${vu}_run${run}.expected"
    local sse_complete_file="$RESULT_DIR/sse/${prefix}_vu${vu}_run${run}.complete"
    local lock_wait_file="$RESULT_DIR/lock-waits/${prefix}_vu${vu}_run${run}.csv"
    local start_epoch
    local load_finished_epoch
    local load_metrics_scraped_epoch
    local metrics_end_epoch
    local producer_success_after
    local lock_wait_failed_at
    local k6_status=0
    local collector_status=0
    CURRENT_MINE_AMOUNT=$((vu * iterations))

    reset_database
    provision_test_fixture "$vu" "$fixture_file"
    SSE_CLEANUP_PENDING=true
    start_sse_client "$vu" "$fixture_file" "$sse_ready_file" \
        "$sse_summary_file" "$sse_log_file" "$sse_expected_file" \
        "$sse_complete_file"

    start_lock_wait_collector "$lock_wait_file"
    sleep "$LOCK_WAIT_POLL_SECONDS"
    PRODUCER_SUCCESS_BASELINE=$(prom_query \
        "sum(gold_rush_producer_mining_success_total{${PROM_LABELS}}) or vector(0)")
    BACKEND_REQUEST_BASELINE_BY_INSTANCE=$(prom_query_by_instance_optional \
        "sum by (instance) (http_server_requests_seconds_count{uri=\"${MINE_URI}\",method=\"POST\",${PROM_LABELS}})")
    STARTED_AT_EPOCH_MS=$(node -p 'Date.now()')
    STARTED_AT_EPOCH_PRECISE=$(awk \
        -v milliseconds="$STARTED_AT_EPOCH_MS" \
        'BEGIN { printf "%.3f", milliseconds / 1000 }')
    STARTED_AT=$(TZ=Asia/Seoul date +%Y-%m-%dT%H:%M:%S%z)
    start_epoch=$((STARTED_AT_EPOCH_MS / 1000))
    echo "[$STARTED_AT] $prefix: VU=$vu, run=$run, iterations=$iterations, mineAmount=$CURRENT_MINE_AMOUNT"

    k6 run \
        -e "BASE_URL=$REQUEST_HOST" \
        -e "MINE_AMOUNT=$CURRENT_MINE_AMOUNT" \
        -e "USER_COUNT=$vu" \
        -e "ITERATIONS=$iterations" \
        -e "HOTSPOT_MAX_DURATION=$MAX_DURATION" \
        -e "SESSIONS_FILE=$fixture_file" \
        --summary-export "$summary_file" "$SCENARIO_FILE" \
        > "$log_file" 2>&1 || k6_status=$?

    if ((k6_status != 0)); then
        stop_lock_wait_collector || true
        lock_wait_failed_at=$(node -p 'Date.now() / 1000')
        summarize_lock_waits \
            "$lock_wait_file" "$STARTED_AT_EPOCH_PRECISE" "$lock_wait_failed_at"
        echo "k6 실행에 실패했습니다. 로그: $log_file" >&2
        tail -n 40 "$log_file" >&2
        return 1
    fi

    load_finished_epoch=$(date +%s)
    LOAD_FINISHED_AT_EPOCH_MS=$((load_finished_epoch * 1000))
    LOAD_FINISHED_AT=$(TZ=Asia/Seoul date +%Y-%m-%dT%H:%M:%S%z)
    TPS=$(jq -er '
        .metrics as $metrics
        | (($metrics.benchmark_finished_at_ms.max
            - $metrics.benchmark_started_at_ms.min) / 1000) as $seconds
        | if $seconds <= 0 then
            $metrics.benchmark_requests.count
          else
            $metrics.benchmark_requests.count / $seconds
          end
    ' "$summary_file")
    BENCHMARK_DURATION_SECONDS=$(jq -er '
        (.metrics.benchmark_finished_at_ms.max
            - .metrics.benchmark_started_at_ms.min) / 1000
        | if . <= 0 then 0.001 else . end
    ' "$summary_file")
    AVG_LATENCY=$(jq -er '.metrics.benchmark_latency.avg' "$summary_file")
    P95=$(jq -er '.metrics.benchmark_latency["p(95)"]' "$summary_file")
    P99=$(jq -er '.metrics.benchmark_latency["p(99)"]' "$summary_file")
    ERROR_RATE=$(jq -er '.metrics.benchmark_errors.value * 100' "$summary_file")
    MINE_ID=$(jq -er '.metrics.benchmark_mine_id.value | round' "$summary_file")

    sleep "$PROM_SCRAPE_DELAY"
    load_metrics_scraped_epoch=$(date +%s)
    producer_success_after=$(prom_query \
        "sum(gold_rush_producer_mining_success_total{${PROM_LABELS}}) or vector(0)")
    PUBLISHED_EVENT_COUNT=$(awk \
        -v before="$PRODUCER_SUCCESS_BASELINE" \
        -v after="$producer_success_after" \
        'BEGIN { printf "%.0f", after - before }')
    if ((PUBLISHED_EVENT_COUNT < 0 || PUBLISHED_EVENT_COUNT > CURRENT_MINE_AMOUNT)); then
        echo "producer success counter delta가 유효하지 않습니다: $PUBLISHED_EVENT_COUNT" >&2
        echo "실행 중 counter reset 또는 다른 채굴 트래픽이 있었는지 확인해야 합니다." >&2
        return 1
    fi
    printf '%s\n' "$PUBLISHED_EVENT_COUNT" > "$sse_expected_file"

    wait_for_consumer_drain "$load_finished_epoch"
    stop_lock_wait_collector || collector_status=$?
    summarize_lock_waits \
        "$lock_wait_file" "$STARTED_AT_EPOCH_PRECISE" \
        "$DRAINED_AT_EPOCH_PRECISE"
    if ((collector_status != 0)); then
        echo "lock wait 수집에 실패했습니다: $lock_wait_file" >&2
        return 1
    fi
    sleep "$SSE_FINALIZE_GRACE_SECONDS"
    printf 'drained\n' > "$sse_complete_file"
    wait_for_sse_client "$sse_summary_file" "$sse_log_file" "$load_finished_epoch"
    FINISHED_AT_EPOCH_MS=$(($(date +%s) * 1000))
    FINISHED_AT=$(TZ=Asia/Seoul date +%Y-%m-%dT%H:%M:%S%z)
    sleep "$PROM_SCRAPE_DELAY"
    collect_peak_metrics \
        "$start_epoch" "$load_finished_epoch" "$load_metrics_scraped_epoch"
    metrics_end_epoch=$(date +%s)
    collect_event_metrics "$start_epoch" "$metrics_end_epoch"
    verify_database
}

append_run() {
    local vu=$1
    local run=$2

    csv_row \
        "RUN" "LOAD_BALANCER" "$REQUEST_HOST" "$VERSION" \
        "$(format_count "$HIKARI_MAX")" "$vu" "$run" \
        "$(format_tps "$TPS")" \
        "" "" "" \
        "" "" \
        "$(format_count "$HIKARI_ACTIVE_PEAK")" \
        "$(format_decimal "$AVG_LATENCY")" \
        "$(format_decimal "$P95")" \
        "$(format_decimal "$P99")" \
        "$(format_decimal "$ERROR_RATE")" \
        "$LOCK_WAIT_COUNT" \
        "$(format_decimal "$LOCK_WAIT_TOTAL_MS")" \
        "$(format_decimal "$LOCK_WAIT_AVG_MS")" \
        "$(format_decimal "$LOCK_WAIT_MAX_MS")" \
        "$LOCK_WAIT_POLL_SECONDS" \
        "$(format_count "$INITIAL_REMAINING")" \
        "$(format_count "$USER_TOTAL_MINED")" \
        "$(format_count "$LOG_TOTAL_MINED")" \
        "$(format_count "$ACTUAL_REMAINING")" "$CONSISTENCY" \
        "$STARTED_AT" "$FINISHED_AT" >> "$OUTPUT_FILE"

    jq -cn \
        --argjson vu "$vu" \
        --argjson hikariMax "$HIKARI_MAX" \
        --argjson tps "$TPS" \
        --argjson backendTpsByInstance "$BACKEND_TPS_BY_INSTANCE" \
        --argjson systemCpu "$SYSTEM_CPU_PEAK" \
        --argjson processCpu "$PROCESS_CPU_PEAK" \
        --argjson heap "$JVM_HEAP_PEAK" \
        --argjson systemCpuByInstance "$SYSTEM_CPU_PEAK_BY_INSTANCE" \
        --argjson processCpuByInstance "$PROCESS_CPU_PEAK_BY_INSTANCE" \
        --argjson heapByInstance "$JVM_HEAP_PEAK_BY_INSTANCE" \
        --argjson tomcatAvailableThreadsMinByInstance "$TOMCAT_AVAILABLE_THREADS_MIN_BY_INSTANCE" \
        --argjson tomcatAvailableThreadsAvgByInstance "$TOMCAT_AVAILABLE_THREADS_AVG_BY_INSTANCE" \
        --argjson hikariActive "$HIKARI_ACTIVE_PEAK" \
        --argjson avgLatency "$AVG_LATENCY" \
        --argjson p95 "$P95" \
        --argjson p99 "$P99" \
        --argjson errorRate "$ERROR_RATE" \
        --argjson dbLockWaitCount "$LOCK_WAIT_COUNT" \
        --argjson dbLockWaitTotalMs "$LOCK_WAIT_TOTAL_MS" \
        --argjson dbLockWaitAvgMs "$LOCK_WAIT_AVG_MS" \
        --argjson dbLockWaitMaxMs "$LOCK_WAIT_MAX_MS" \
        --argjson initial "$INITIAL_REMAINING" \
        --argjson userMined "$USER_TOTAL_MINED" \
        --argjson logMined "$LOG_TOTAL_MINED" \
        --argjson remaining "$ACTUAL_REMAINING" \
        --arg consistent "$CONSISTENCY" \
        '{
            vu: $vu,
            hikariMax: $hikariMax,
            tps: $tps,
            backendTpsByInstance: $backendTpsByInstance,
            systemCpu: $systemCpu,
            processCpu: $processCpu,
            heap: $heap,
            systemCpuByInstance: $systemCpuByInstance,
            processCpuByInstance: $processCpuByInstance,
            heapByInstance: $heapByInstance,
            tomcatAvailableThreadsMinByInstance: $tomcatAvailableThreadsMinByInstance,
            tomcatAvailableThreadsAvgByInstance: $tomcatAvailableThreadsAvgByInstance,
            hikariActive: $hikariActive,
            avgLatency: $avgLatency,
            p95: $p95,
            p99: $p99,
            errorRate: $errorRate,
            dbLockWaitCount: $dbLockWaitCount,
            dbLockWaitTotalMs: $dbLockWaitTotalMs,
            dbLockWaitAvgMs: $dbLockWaitAvgMs,
            dbLockWaitMaxMs: $dbLockWaitMaxMs,
            initial: $initial,
            userMined: $userMined,
            logMined: $logMined,
            remaining: $remaining,
            consistent: $consistent
        }' >> "$RUN_JSONL"

    append_backend_rows \
        "RUN" "$vu" "$run" "$STARTED_AT" "$FINISHED_AT" \
        "$BACKEND_TPS_BY_INSTANCE" "$SYSTEM_CPU_PEAK_BY_INSTANCE" \
        "$PROCESS_CPU_PEAK_BY_INSTANCE" "$JVM_HEAP_PEAK_BY_INSTANCE" \
        "$TOMCAT_AVAILABLE_THREADS_MIN_BY_INSTANCE" \
        "$TOMCAT_AVAILABLE_THREADS_AVG_BY_INSTANCE"
    append_event_metrics "RUN" "$vu" "$run"
    append_sse_metrics "RUN" "$vu" "$run"
}

append_warmup() {
    csv_row \
        "WARMUP" "LOAD_BALANCER" "$REQUEST_HOST" "$VERSION" \
        "$(format_count "$HIKARI_MAX")" "$WARMUP_VU" "1" \
        "$(format_tps "$TPS")" \
        "" "" "" \
        "" "" \
        "$(format_count "$HIKARI_ACTIVE_PEAK")" \
        "$(format_decimal "$AVG_LATENCY")" \
        "$(format_decimal "$P95")" \
        "$(format_decimal "$P99")" \
        "$(format_decimal "$ERROR_RATE")" \
        "$LOCK_WAIT_COUNT" \
        "$(format_decimal "$LOCK_WAIT_TOTAL_MS")" \
        "$(format_decimal "$LOCK_WAIT_AVG_MS")" \
        "$(format_decimal "$LOCK_WAIT_MAX_MS")" \
        "$LOCK_WAIT_POLL_SECONDS" \
        "$(format_count "$INITIAL_REMAINING")" \
        "$(format_count "$USER_TOTAL_MINED")" \
        "$(format_count "$LOG_TOTAL_MINED")" \
        "$(format_count "$ACTUAL_REMAINING")" "$CONSISTENCY" \
        "$STARTED_AT" "$FINISHED_AT" >> "$OUTPUT_FILE"

    append_backend_rows \
        "WARMUP" "$WARMUP_VU" "1" "$STARTED_AT" "$FINISHED_AT" \
        "$BACKEND_TPS_BY_INSTANCE" "$SYSTEM_CPU_PEAK_BY_INSTANCE" \
        "$PROCESS_CPU_PEAK_BY_INSTANCE" "$JVM_HEAP_PEAK_BY_INSTANCE" \
        "$TOMCAT_AVAILABLE_THREADS_MIN_BY_INSTANCE" \
        "$TOMCAT_AVAILABLE_THREADS_AVG_BY_INSTANCE"
    append_event_metrics "WARMUP" "$WARMUP_VU" "1"
    append_sse_metrics "WARMUP" "$WARMUP_VU" "1"

    echo "웜업 결과를 CSV에 기록했습니다."
    annotate_grafana "warmup" "$WARMUP_VU" "1"
}

append_backend_rows() {
    local record_type=$1
    local vu=$2
    local run=$3
    local started_at=$4
    local finished_at=$5
    local tps_by_instance=$6
    local system_cpu_by_instance=$7
    local process_cpu_by_instance=$8
    local heap_by_instance=$9
    local tomcat_available_threads_min_by_instance=${10}
    local tomcat_available_threads_avg_by_instance=${11}
    local instance
    local instance_tps
    local instance_system_cpu
    local instance_process_cpu
    local instance_heap
    local instance_tomcat_available_threads_min
    local instance_tomcat_available_threads_avg

    while IFS= read -r instance; do
        instance_tps=$(jq -er --arg instance "$instance" '.[$instance]' \
            <<< "$tps_by_instance")
        instance_system_cpu=$(jq -er --arg instance "$instance" '.[$instance]' \
            <<< "$system_cpu_by_instance")
        instance_process_cpu=$(jq -er --arg instance "$instance" '.[$instance]' \
            <<< "$process_cpu_by_instance")
        instance_heap=$(jq -er --arg instance "$instance" '.[$instance]' \
            <<< "$heap_by_instance")
        instance_tomcat_available_threads_min=$(jq -er \
            --arg instance "$instance" '.[$instance]' \
            <<< "$tomcat_available_threads_min_by_instance")
        instance_tomcat_available_threads_avg=$(jq -er \
            --arg instance "$instance" '.[$instance]' \
            <<< "$tomcat_available_threads_avg_by_instance")

        csv_row \
            "$record_type" "BACKEND" "$instance" "$VERSION" "" "$vu" "$run" \
            "$(format_tps "$instance_tps")" \
            "$(format_decimal "$instance_system_cpu")" \
            "$(format_decimal "$instance_process_cpu")" \
            "$(format_decimal "$instance_heap")" \
            "$(format_count "$instance_tomcat_available_threads_min")" \
            "$(format_decimal "$instance_tomcat_available_threads_avg")" \
            "" "" "" "" "" "" "" "" "" "" "" "" "" "" "" \
            "$started_at" "$finished_at" >> "$OUTPUT_FILE"
    done < <(jq -r 'keys[]' <<< "$tps_by_instance")
}

append_average() {
    local vu=$1
    local average

    average=$(jq -sc --argjson vu "$vu" '
        map(select(.vu == $vu)) as $rows
        | ($rows | length) as $count
        | def mean(field): ($rows | map(.[field]) | add / $count);
        def mean_by_instance(field):
            reduce ($rows | map(.[field])[]) as $metrics ({};
                reduce ($metrics | to_entries[]) as $metric (.;
                    .[$metric.key] = ((.[$metric.key] // []) + [$metric.value])
                )
            )
            | with_entries(.value = (.value | add / length));
        {
            hikariMax: mean("hikariMax"),
            tps: mean("tps"),
            backendTpsByInstance: mean_by_instance("backendTpsByInstance"),
            systemCpu: mean("systemCpu"),
            processCpu: mean("processCpu"),
            heap: mean("heap"),
            systemCpuByInstance: mean_by_instance("systemCpuByInstance"),
            processCpuByInstance: mean_by_instance("processCpuByInstance"),
            heapByInstance: mean_by_instance("heapByInstance"),
            tomcatAvailableThreadsMinByInstance:
                mean_by_instance("tomcatAvailableThreadsMinByInstance"),
            tomcatAvailableThreadsAvgByInstance:
                mean_by_instance("tomcatAvailableThreadsAvgByInstance"),
            hikariActive: mean("hikariActive"),
            avgLatency: mean("avgLatency"),
            p95: mean("p95"),
            p99: mean("p99"),
            errorRate: mean("errorRate"),
            dbLockWaitCount: mean("dbLockWaitCount"),
            dbLockWaitTotalMs: mean("dbLockWaitTotalMs"),
            dbLockWaitAvgMs: mean("dbLockWaitAvgMs"),
            dbLockWaitMaxMs: mean("dbLockWaitMaxMs"),
            initial: mean("initial"),
            userMined: mean("userMined"),
            logMined: mean("logMined"),
            remaining: mean("remaining"),
            consistent: (if all($rows[]; .consistent == "✅") then "✅" else "❌" end)
        }
    ' "$RUN_JSONL")

    csv_row \
        "AVERAGE" "LOAD_BALANCER" "$REQUEST_HOST" "$VERSION" \
        "$(format_count "$(jq -r '.hikariMax' <<< "$average")")" "$vu" \
        "AVERAGE" "$(format_tps "$(jq -r '.tps' <<< "$average")")" \
        "" "" "" \
        "" "" \
        "$(format_count "$(jq -r '.hikariActive' <<< "$average")")" \
        "$(format_decimal "$(jq -r '.avgLatency' <<< "$average")")" \
        "$(format_decimal "$(jq -r '.p95' <<< "$average")")" \
        "$(format_decimal "$(jq -r '.p99' <<< "$average")")" \
        "$(format_decimal "$(jq -r '.errorRate' <<< "$average")")" \
        "$(format_decimal "$(jq -r '.dbLockWaitCount' <<< "$average")")" \
        "$(format_decimal "$(jq -r '.dbLockWaitTotalMs' <<< "$average")")" \
        "$(format_decimal "$(jq -r '.dbLockWaitAvgMs' <<< "$average")")" \
        "$(format_decimal "$(jq -r '.dbLockWaitMaxMs' <<< "$average")")" \
        "$LOCK_WAIT_POLL_SECONDS" \
        "$(format_count "$(jq -r '.initial' <<< "$average")")" \
        "$(format_count "$(jq -r '.userMined' <<< "$average")")" \
        "$(format_count "$(jq -r '.logMined' <<< "$average")")" \
        "$(format_count "$(jq -r '.remaining' <<< "$average")")" \
        "$(jq -r '.consistent' <<< "$average")" "" "" >> "$OUTPUT_FILE"

    append_backend_rows \
        "AVERAGE" "$vu" "AVERAGE" "" "" \
        "$(jq -c '.backendTpsByInstance' <<< "$average")" \
        "$(jq -c '.systemCpuByInstance' <<< "$average")" \
        "$(jq -c '.processCpuByInstance' <<< "$average")" \
        "$(jq -c '.heapByInstance' <<< "$average")" \
        "$(jq -c '.tomcatAvailableThreadsMinByInstance' <<< "$average")" \
        "$(jq -c '.tomcatAvailableThreadsAvgByInstance' <<< "$average")"

    echo "VU=$vu 산술평균 행을 기록했습니다."
}

echo "결과 CSV: $OUTPUT_FILE"
preflight
echo "웜업을 시작합니다."
run_k6 "$WARMUP_VU" 1 "$WARMUP_ITERATIONS" "warmup"
append_warmup
request_sse_cleanup
wait_until_stable

for vu in "${VUS[@]}"; do
    for ((run = 1; run <= REPEAT_COUNT; run += 1)); do
        run_k6 "$vu" "$run" "$ITERATIONS" "benchmark"
        append_run "$vu" "$run"
        echo "VU=$vu run=$run 결과를 CSV에 기록했습니다."
        annotate_grafana "benchmark" "$vu" "$run"
        request_sse_cleanup
        wait_until_stable
    done
    append_average "$vu"
    append_event_average "$vu"
    append_sse_average "$vu"
done

echo "실험이 완료되었습니다: $OUTPUT_FILE"
echo "이벤트 지표 CSV: $EVENT_OUTPUT_FILE"
echo "SSE 지표 CSV: $SSE_OUTPUT_FILE"
