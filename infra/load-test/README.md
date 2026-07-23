# Gold Rush Lab Load Test

Gold Rush Lab의 동시성 동작과 버전별 성능을 같은 조건에서 비교하기 위한 k6 테스트 모음입니다. 모든 명령은 `infra/load-test` 디렉터리에서 실행합니다.

## 준비

테스트 전에 애플리케이션과 데이터베이스를 실행하고 [k6](https://grafana.com/docs/k6/latest/set-up/install-k6/)가 설치되어 있는지 확인합니다. 기본 서버 주소는 애플리케이션 context path를 포함한 `http://localhost:8080/api`입니다.

```sh
cd infra/load-test
cp .env.example .env
k6 version
```

## 실행 방법

각 시나리오는 직접 실행할 수 있습니다.

```sh
k6 run scenarios/smoke.js
k6 run scenarios/hotspot.js
k6 run scenarios/capacity.js
k6 run scenarios/stress.js
k6 run scenarios/soak.js
```

`run.sh`를 사용해도 동일합니다. `run.sh`는 같은 디렉터리의 `.env` 파일을 자동으로 불러옵니다.

```sh
./run.sh smoke
./run.sh hotspot
./run.sh capacity
```

환경변수는 명령 앞에 지정하거나 k6의 `-e` 옵션으로 전달합니다.

```sh
BASE_URL=http://localhost:8080/api USER_COUNT=100 MINE_AMOUNT=1000000 \
  k6 run scenarios/hotspot.js

k6 run -e BASE_URL=http://localhost:8080/api -e USER_COUNT=100 \
  -e MINE_AMOUNT=1000000 scenarios/hotspot.js
```

## 환경변수

| 이름 | 기본값 | 설명 |
| --- | ---: | --- |
| `BASE_URL` | `http://localhost:8080/api` | context path를 포함한 테스트 대상 애플리케이션 주소 |
| `MINE_AMOUNT` | `100000` | setup에서 생성하는 광산의 초기 금 수량 |
| `USER_COUNT` | `100` | setup에서 생성할 사용자 수. Hot Spot에서는 VU 수도 결정 |
| `ITERATIONS` | `100` | Hot Spot에서 VU 한 개가 실행할 반복 횟수 |
| `TIMEOUT` | `5s` | 각 HTTP 요청의 timeout |
| `HOTSPOT_MAX_DURATION` | `1m` | Hot Spot 시나리오의 최대 실행 시간 |
| `HOTSPOT_MINE_ID` | 미지정 | Hot Spot에서 새 광산 대신 사용할 기존 광산 ID |
| `STRESS_MAX_VU` | `1000` | Stress Test의 최대 VU 수 |
| `SOAK_VUS` | `50` | Soak Test의 동시 VU 수 |
| `SOAK_DURATION` | `2h` | Soak Test 실행 시간 |

`MINE_AMOUNT`, `USER_COUNT`, `ITERATIONS`, `HOTSPOT_MINE_ID`, `STRESS_MAX_VU`, `SOAK_VUS`는 양의 정수여야 합니다. 장시간 또는 고부하 테스트에서는 테스트 도중 광산이 고갈되지 않도록 `MINE_AMOUNT`를 예상 요청 수보다 크게 설정합니다.

## 테스트 목적

### Smoke Test

1 VU가 10초 동안 한 사용자 세션으로 채굴 API를 호출합니다. API 상태, 인증에 사용되는 세션, 공통 응답 형식과 채굴 결과 필드를 빠르게 확인합니다.

### Hot Spot Concurrency Test

가장 핵심적인 동시성 테스트입니다. setup에서 하나의 광산과 `USER_COUNT`명의 사용자를 준비합니다. `per-vu-iterations` executor가 사용자 수만큼 VU를 만들고, 각 VU는 전역 VU ID에 대응하는 서로 다른 `sessionId`로 `ITERATIONS`회 채굴합니다. 모든 세션은 같은 `mineId`로 생성되므로 요청은 동일한 Mine row에 집중됩니다. iteration 사이의 `sleep`은 없습니다. 실행 시간은 `HOTSPOT_MAX_DURATION`을 초과하지 않습니다.

기존 광산을 대상으로 반복 비교하려면 `HOTSPOT_MINE_ID`를 지정할 수 있습니다. 지정하지 않으면 setup이 새 광산을 만듭니다.

### Capacity Test

`ramping-arrival-rate`로 목표 처리율을 10, 30, 50, 100, 200, 400 TPS 순서로 올립니다. 각 목표까지 10초 동안 ramp한 뒤 30초 동안 같은 TPS를 유지하므로, hold 구간의 처리량과 지연시간을 기준으로 지속 가능 여부를 판단할 수 있습니다. 채굴 요청의 실패율 1% 미만, 응답 검증 성공률 99% 초과, p95 500ms 미만, p99 1초 미만을 threshold로 평가합니다.

기본 k6 종료 summary는 전체 실행 결과를 집계하므로 목표 TPS별 결과를 따로 보여주지 않습니다. 각 hold 구간을 분석하려면 시계열 출력을 저장하거나 Grafana 같은 출력 backend를 사용합니다.

```sh
k6 run --out csv=results/capacity.csv scenarios/capacity.js
```

### Stress Test

`ramping-vus`로 VU를 최대치의 10%, 30%, 50%, 100%까지 올립니다. 기본값에서는 100, 300, 500, 1000 VU입니다. setup은 `USER_COUNT`와 `STRESS_MAX_VU` 중 큰 값만큼 사용자를 생성하며, 각 VU는 전역 VU ID에 대응하는 고유 세션을 사용합니다. 따라서 같은 User row의 중복 경합이 아니라 서버가 처리하는 전체 부하와 공통 Mine row의 경합을 측정합니다. 최대 부하를 유지한 뒤 낮추면서 오류율, 지연시간, 회복 여부를 확인합니다.

### Soak Test

기본 50 VU로 2시간 동안 지속 호출합니다. 메모리 누수, DB 연결 고갈, 장시간 실행 시 지연 증가와 같은 안정성 문제를 확인합니다. 짧은 사전 검증은 `SOAK_DURATION=10m`처럼 실행 시간을 낮춰 수행할 수 있습니다.

## 공통 setup

모든 시나리오는 `lib/setup.js`의 공통 함수를 사용합니다.

1. `POST /mines?amount=...`로 광산 하나를 생성합니다.
2. 같은 `mineId`로 `POST /user/signin`을 N번 호출합니다.
3. 응답에서 `sessionId`를 모읍니다.
4. `{ mineId, sessions }`를 각 VU에 전달합니다.

`HOTSPOT_MINE_ID`가 설정된 Hot Spot 실행에서만 1단계를 생략하고 지정된 기존 광산에 사용자를 연결합니다. HTTP 호출은 `lib/api.js`, 응답 검증은 `lib/checks.js`, 환경설정은 `lib/config.js`에만 정의되어 시나리오에 중복되지 않습니다.

각 API 응답 검증은 HTTP 상태와 본문을 합쳐 응답당 하나의 check만 기록합니다. Capacity와 Soak는 `operation: mine` 태그가 붙은 채굴 응답의 검증 성공률이 99% 초과여야 하며, Stress는 고부하 구간을 고려해 95% 초과를 요구합니다. setup의 광산 생성 및 로그인 check는 이 threshold에서 제외됩니다. 따라서 HTTP 200 응답이라도 `success: false`, 잘못된 JSON 또는 필수 응답 필드 누락이 발생하면 채굴 응답 한 건의 실패로 정확히 반영됩니다.

## 버전 간 비교

스크립트는 내부 잠금 구현이나 배포 토폴로지를 알지 못하며 공개 HTTP API와 응답 계약만 사용합니다. 따라서 v0.2의 Optimistic/Pessimistic Lock 구현이나 v0.3의 Scale-out 환경도 같은 API 계약을 유지하면 `BASE_URL`만 대상 환경으로 변경하여 동일한 부하 패턴, 사용자 수, 광산 수량 및 threshold로 비교할 수 있습니다.

테스트 결과 파일은 `results/`, 입력 데이터가 추가될 경우에는 `data/`에 보관합니다. 예를 들어 k6 summary를 저장하려면 다음과 같이 실행합니다.

```sh
k6 run --summary-export results/hotspot-summary.json scenarios/hotspot.js
```
