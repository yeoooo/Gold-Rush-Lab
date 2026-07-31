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
MINE_URI=${MINE_URI:-/mine}
PROM_SCRAPE_DELAY=${PROM_SCRAPE_DELAY:-6}
STABLE_PROCESS_CPU_PCT=${STABLE_PROCESS_CPU_PCT:-20}
STABLE_SYSTEM_CPU_PCT=${STABLE_SYSTEM_CPU_PCT:-50}
STABLE_HEAP_PCT=${STABLE_HEAP_PCT:-75}
STABLE_HIKARI_PCT=${STABLE_HIKARI_PCT:-20}
STABLE_TPS=${STABLE_TPS:-0}
STABLE_SAMPLES=${STABLE_SAMPLES:-3}
STABLE_POLL_SECONDS=${STABLE_POLL_SECONDS:-5}
STABLE_TIMEOUT_SECONDS=${STABLE_TIMEOUT_SECONDS:-600}
POST_STABLE_COOLDOWN_SECONDS=${POST_STABLE_COOLDOWN_SECONDS:-15}
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

if [[ ! "$SCENARIO" =~ ^[a-zA-Z0-9_-]+$ ]]; then
    echo "시나리오 이름에는 영문, 숫자, _, -만 사용할 수 있습니다." >&2
    exit 2
fi

SCENARIO_FILE="$SCRIPT_DIR/scenarios/$SCENARIO.js"
if [[ ! -f "$SCENARIO_FILE" ]]; then
    echo "지원하지 않는 시나리오입니다: $SCENARIO" >&2
    exit 2
fi

for command_name in k6 curl jq awk; do
    if ! command -v "$command_name" >/dev/null 2>&1; then
        echo "필요한 명령을 찾을 수 없습니다: $command_name" >&2
        exit 2
    fi
done

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
mkdir -p "$RESULT_DIR/logs" "$RESULT_DIR/summaries"

if [[ -z "$OUTPUT_FILE" ]]; then
    OUTPUT_FILE="$RESULT_DIR/results.csv"
elif [[ "$OUTPUT_FILE" != /* ]]; then
    OUTPUT_FILE="$PWD/$OUTPUT_FILE"
fi
mkdir -p "$(dirname -- "$OUTPUT_FILE")"

RUN_JSONL="$RESULT_DIR/runs.jsonl"
: > "$RUN_JSONL"

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

calculate_timer_average_ms() {
    awk \
        -v start_sum="$1" \
        -v end_sum="$2" \
        -v start_count="$3" \
        -v end_count="$4" '
            BEGIN {
                # 애플리케이션 재시작으로 Timer가 초기화되면 종료값을 사용한다.
                if (end_sum < start_sum || end_count < start_count) {
                    delta_sum = end_sum
                    delta_count = end_count
                } else {
                    delta_sum = end_sum - start_sum
                    delta_count = end_count - start_count
                }

                if (delta_count <= 0) {
                    print 0
                } else {
                    printf "%.9f", 1000 * delta_sum / delta_count
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
    "Error Rate (%)" "초기 잔량" "사용자 총 채굴량" \
    "Mining Log 총 채굴량" "실제 잔량" "정합성" "Started At" "Finished At" \
    "Pessimistic Lock Wait Avg (ms)" \
    > "$OUTPUT_FILE"

prom_query() {
    local query=$1
    curl -fsS --get "$MONITOR_HOST/api/v1/query" \
        --data-urlencode "query=$query" |
        jq -er '
            if .status != "success" then
                error(.error // "Prometheus query failed")
            elif (.data.result | length) != 1 then
                error("Prometheus query returned \(.data.result | length) series")
            else
                .data.result[0].value[1]
            end
        '
}

prom_query_by_instance() {
    local query=$1
    curl -fsS --get "$MONITOR_HOST/api/v1/query" \
        --data-urlencode "query=$query" |
        jq -cer '
            if .status != "success" then
                error(.error // "Prometheus query failed")
            elif (.data.result | length) == 0 then
                error("Prometheus query returned no series")
            elif any(.data.result[]; .metric.instance == null) then
                error("Prometheus query returned a series without an instance label")
            else
                .data.result
                | map({
                    key: .metric.instance,
                    value: (.value[1] | tonumber)
                })
                | sort_by(.key)
                | from_entries
            end
        '
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

        printf '  CPU(process/system)=%.2f%%/%.2f%%, heap=%.2f%%, Hikari=%.2f%%, TPS=%.3f' \
            "$process_cpu" "$system_cpu" "$heap_pct" "$hikari_pct" "$tps"

        if less_than_or_equal "$process_cpu" "$STABLE_PROCESS_CPU_PCT" &&
            less_than_or_equal "$system_cpu" "$STABLE_SYSTEM_CPU_PCT" &&
            less_than_or_equal "$heap_pct" "$STABLE_HEAP_PCT" &&
            less_than_or_equal "$hikari_pct" "$STABLE_HIKARI_PCT" &&
            less_than_or_equal "$tps" "$STABLE_TPS"; then
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
    local end_epoch
    local window

    sleep "$PROM_SCRAPE_DELAY"
    end_epoch=$(date +%s)
    window=$((end_epoch - start_epoch))

    SYSTEM_CPU_PEAK=$(prom_query \
        "max_over_time((100 * max(system_cpu_usage{${PROM_LABELS}}))[${window}s:5s])")
    PROCESS_CPU_PEAK=$(prom_query \
        "max_over_time((100 * max(process_cpu_usage{${PROM_LABELS}}))[${window}s:5s])")
    JVM_HEAP_PEAK=$(prom_query \
        "max_over_time((100 * sum(jvm_memory_used_bytes{area=\"heap\",${PROM_LABELS}}) / clamp_min(sum(jvm_memory_max_bytes{area=\"heap\",${PROM_LABELS}}), 1))[${window}s:5s])")
    HIKARI_ACTIVE_PEAK=$(prom_query \
        "max_over_time((sum(hikaricp_connections_active{${PROM_LABELS}}))[${window}s:5s])")
    HIKARI_MAX=$(prom_query \
        "sum(hikaricp_connections_max{${PROM_LABELS}})")
    LOCK_WAIT_SUM_END=$(prom_query \
        "sum(gold_rush_mining_lock_wait_seconds_sum{strategy=\"pessimistic\",${PROM_LABELS}}) or vector(0)")
    LOCK_WAIT_COUNT_END=$(prom_query \
        "sum(gold_rush_mining_lock_wait_seconds_count{strategy=\"pessimistic\",${PROM_LABELS}}) or vector(0)")
    LOCK_WAIT_AVG_MS=$(calculate_timer_average_ms \
        "$LOCK_WAIT_SUM_START" "$LOCK_WAIT_SUM_END" \
        "$LOCK_WAIT_COUNT_START" "$LOCK_WAIT_COUNT_END")

    BACKEND_TPS_BY_INSTANCE=$(prom_query_by_instance \
        "sum by (instance) (increase(http_server_requests_seconds_count{uri=\"${MINE_URI}\",method=\"POST\",${PROM_LABELS}}[${window}s])) / ${BENCHMARK_DURATION_SECONDS}")
    SYSTEM_CPU_PEAK_BY_INSTANCE=$(prom_query_by_instance \
        "max_over_time((100 * system_cpu_usage{${PROM_LABELS}})[${window}s:5s])")
    PROCESS_CPU_PEAK_BY_INSTANCE=$(prom_query_by_instance \
        "max_over_time((100 * process_cpu_usage{${PROM_LABELS}})[${window}s:5s])")
    JVM_HEAP_PEAK_BY_INSTANCE=$(prom_query_by_instance \
        "max_over_time((100 * sum by (instance) (jvm_memory_used_bytes{area=\"heap\",${PROM_LABELS}}) / clamp_min(sum by (instance) (jvm_memory_max_bytes{area=\"heap\",${PROM_LABELS}}), 1))[${window}s:5s])")
    TOMCAT_AVAILABLE_THREADS_MIN_BY_INSTANCE=$(prom_query_by_instance \
        "min_over_time((clamp_min(sum by (instance) (tomcat_threads_config_max_threads{${PROM_LABELS}}) - sum by (instance) (tomcat_threads_busy_threads{${PROM_LABELS}}), 0))[${window}s:1s])")
    TOMCAT_AVAILABLE_THREADS_AVG_BY_INSTANCE=$(prom_query_by_instance \
        "avg_over_time((clamp_min(sum by (instance) (tomcat_threads_config_max_threads{${PROM_LABELS}}) - sum by (instance) (tomcat_threads_busy_threads{${PROM_LABELS}}), 0))[${window}s:1s])")
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
    echo "DB를 초기화합니다: app_user, mine, mining_log"
    "$PSQL_BIN" "$DB_URL" \
        -v ON_ERROR_STOP=1 \
        -f "$SCRIPT_DIR/sql/reset.sql"
}

preflight() {
    local app_status
    local prometheus_up
    local database_status
    local grafana_status

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

    database_status=$("$PSQL_BIN" "$DB_URL" -v ON_ERROR_STOP=1 -Atqc 'SELECT 1')
    if [[ "$database_status" != "1" ]]; then
        echo "PostgreSQL 연결 확인에 실패했습니다." >&2
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

run_k6() {
    local vu=$1
    local run=$2
    local iterations=$3
    local prefix=$4
    local summary_file="$RESULT_DIR/summaries/${prefix}_vu${vu}_run${run}.json"
    local log_file="$RESULT_DIR/logs/${prefix}_vu${vu}_run${run}.log"
    local start_epoch
    CURRENT_MINE_AMOUNT=$((vu * iterations))

    reset_database

    LOCK_WAIT_SUM_START=$(prom_query \
        "sum(gold_rush_mining_lock_wait_seconds_sum{strategy=\"pessimistic\",${PROM_LABELS}}) or vector(0)")
    LOCK_WAIT_COUNT_START=$(prom_query \
        "sum(gold_rush_mining_lock_wait_seconds_count{strategy=\"pessimistic\",${PROM_LABELS}}) or vector(0)")
    STARTED_AT_EPOCH_MS=$(($(date +%s) * 1000))
    STARTED_AT=$(TZ=Asia/Seoul date +%Y-%m-%dT%H:%M:%S%z)
    start_epoch=$(date +%s)
    echo "[$STARTED_AT] $prefix: VU=$vu, run=$run, iterations=$iterations, mineAmount=$CURRENT_MINE_AMOUNT"

    if ! k6 run \
        -e "BASE_URL=$REQUEST_HOST" \
        -e "MINE_AMOUNT=$CURRENT_MINE_AMOUNT" \
        -e "USER_COUNT=$vu" \
        -e "ITERATIONS=$iterations" \
        -e "HOTSPOT_MAX_DURATION=$MAX_DURATION" \
        --summary-export "$summary_file" "$SCENARIO_FILE" \
        > "$log_file" 2>&1; then
        echo "k6 실행에 실패했습니다. 로그: $log_file" >&2
        tail -n 40 "$log_file" >&2
        return 1
    fi

    FINISHED_AT_EPOCH_MS=$(($(date +%s) * 1000))
    FINISHED_AT=$(TZ=Asia/Seoul date +%Y-%m-%dT%H:%M:%S%z)
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

    collect_peak_metrics "$start_epoch"
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
        "$(format_count "$INITIAL_REMAINING")" \
        "$(format_count "$USER_TOTAL_MINED")" \
        "$(format_count "$LOG_TOTAL_MINED")" \
        "$(format_count "$ACTUAL_REMAINING")" "$CONSISTENCY" \
        "$STARTED_AT" "$FINISHED_AT" \
        "$(format_decimal "$LOCK_WAIT_AVG_MS")" >> "$OUTPUT_FILE"

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
        --argjson lockWaitAvgMs "$LOCK_WAIT_AVG_MS" \
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
            lockWaitAvgMs: $lockWaitAvgMs,
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
        "$(format_count "$INITIAL_REMAINING")" \
        "$(format_count "$USER_TOTAL_MINED")" \
        "$(format_count "$LOG_TOTAL_MINED")" \
        "$(format_count "$ACTUAL_REMAINING")" "$CONSISTENCY" \
        "$STARTED_AT" "$FINISHED_AT" \
        "$(format_decimal "$LOCK_WAIT_AVG_MS")" >> "$OUTPUT_FILE"

    append_backend_rows \
        "WARMUP" "$WARMUP_VU" "1" "$STARTED_AT" "$FINISHED_AT" \
        "$BACKEND_TPS_BY_INSTANCE" "$SYSTEM_CPU_PEAK_BY_INSTANCE" \
        "$PROCESS_CPU_PEAK_BY_INSTANCE" "$JVM_HEAP_PEAK_BY_INSTANCE" \
        "$TOMCAT_AVAILABLE_THREADS_MIN_BY_INSTANCE" \
        "$TOMCAT_AVAILABLE_THREADS_AVG_BY_INSTANCE"

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
            "" "" "" "" "" "" "" "" "" "" \
            "$started_at" "$finished_at" "" >> "$OUTPUT_FILE"
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
            lockWaitAvgMs: mean("lockWaitAvgMs"),
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
        "$(format_count "$(jq -r '.initial' <<< "$average")")" \
        "$(format_count "$(jq -r '.userMined' <<< "$average")")" \
        "$(format_count "$(jq -r '.logMined' <<< "$average")")" \
        "$(format_count "$(jq -r '.remaining' <<< "$average")")" \
        "$(jq -r '.consistent' <<< "$average")" "" "" \
        "$(format_decimal "$(jq -r '.lockWaitAvgMs' <<< "$average")")" \
        >> "$OUTPUT_FILE"

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
wait_until_stable

for vu in "${VUS[@]}"; do
    for ((run = 1; run <= REPEAT_COUNT; run += 1)); do
        run_k6 "$vu" "$run" "$ITERATIONS" "benchmark"
        append_run "$vu" "$run"
        echo "VU=$vu run=$run 결과를 CSV에 기록했습니다."
        annotate_grafana "benchmark" "$vu" "$run"
        wait_until_stable
    done
    append_average "$vu"
done

echo "실험이 완료되었습니다: $OUTPUT_FILE"
