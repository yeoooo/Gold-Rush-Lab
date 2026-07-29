# Gold Rush Lab 로드밸런서

Nginx로 애플리케이션 요청을 프록시하고 Nginx Exporter로 상태 메트릭을 노출한다.

## 구성

| 서비스 | 역할 | 호스트 포트 |
| --- | --- | ---: |
| `nginx` | 애플리케이션 요청 프록시 및 로드밸런싱 | `80` |
| `nginx-exporter` | Nginx `stub_status`를 Prometheus 메트릭으로 변환 | `9113` |

Node Exporter는 호스트 운영체제 메트릭을 수집하는 별도 구성으로 분리되어 이 Compose 파일에 포함되지 않는다.

## 환경 설정

예제 파일을 복사한다.

```bash
cd infra/loadbalancer
cp .env.example .env
```

| 환경 변수 | 기본값 | 설명 |
| --- | --- | --- |
| `NGINX_CONFIG` | `nginx.local.conf` | `/etc/nginx/nginx.conf`에 마운트할 설정 파일 |

`.env` 파일은 Git에 포함하지 않는다.

## 로컬 실행

`nginx.local.conf`는 Docker 컨테이너에서 `host.docker.internal:8080`으로 요청을 전달한다. IntelliJ에서 애플리케이션을 먼저 실행한 뒤 로드밸런서를 시작한다.

```bash
docker compose up -d
```

- 프록시 엔드포인트: `http://localhost`
- Nginx Exporter 메트릭: `http://localhost:9113/metrics`

## VM 실행

`nginx.vm.conf`는 다음 애플리케이션 서버를 Round Robin 방식으로 사용한다.

- `192.168.0.41:8080`
- `192.168.0.46:8080`

`.env`의 `NGINX_CONFIG` 값을 변경하거나 실행 시 직접 지정한다.

```bash
NGINX_CONFIG=nginx.vm.conf docker compose up -d
```

## 상태 확인

```bash
docker compose ps
docker compose logs -f nginx nginx-exporter
```

Nginx health check는 컨테이너 내부의 `http://127.0.0.1:8080/stub_status`를 확인하고, Nginx Exporter는 `http://nginx:8080/stub_status`에서 상태 정보를 수집한다. `stub_status` 포트는 호스트에 공개하지 않는다.

## 종료

```bash
docker compose down
```
