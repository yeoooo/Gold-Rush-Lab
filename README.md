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

## 현재 진행 상황

**현재 버전: `v0.5` — Event Driven**
- 전체 결과 표 : [스프레드 시트](https://docs.google.com/spreadsheets/d/1KEnCXDi56xy9ztNQ00YJJ60xO8q4opV7UBKE_EvPc1w/edit?gid=347977445#gid=347977445)
- v0.1 결과 : [[Project : Gold-Rush-Lab] 1. 모놀리식에서의 동시성과 부하](https://yeoooo.github.io/project/gold-rush-lab-monolith-concurrency-load/)
- v0.2 결과 : [[Project : Gold-Rush-Lab] 2. 동시성 문제에서의 락](https://yeoooo.github.io/project/gold-rush-lab-database-lock/)
- v0.3 결과 : [[Project : Gold-Rush-Lab] 3. 분산 시스템에서의 Lock](https://yeoooo.github.io/project/gold-rush-lab-distributed-lock/)
- v0.4 결과 : [[Project : Gold-Rush-Lab] 4. 분산 시스템과 분산 락](https://https://yeoooo.github.io/project/gold-rush-lab-redis-distributed-lock/)
### 실험 결과

아래 결과는 각 실험을 5회 실행한 평균값입니다.

<details>
<summary><strong>v0.1</strong></summary>

| Version | Hikari Max Pool Size | VU | Run | TPS (req/s) | System CPU Peak (%) | Process CPU Peak (%) | JVM Heap Peak (%) | Hikari Active Peak | Avg Latency (ms) | P95 (ms) | P99 (ms) | Error Rate (%) | 초기 잔량 | 사용자 총 채굴량 | Mining Log 총 채굴량 | 실제 잔량 | 정합성 | Started At | Finished At |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| v0.1 | 10 | 10 | AVERAGE | 435.867 req/s | 42.426 | 37.629 | 10.720 | 5 | 24.208 | 46.862 | 104.181 | 0.000 | 1000 | 1000 | 1000 | 817.200 | ❌ |  |  |
| v0.1 | 10 | 50 | AVERAGE | 639.824 req/s | 82.191 | 58.408 | 11.847 | 10 | 76.940 | 146.281 | 214.359 | 0.000 | 5000 | 5000 | 5000 | 4425.600 | ❌ |  |  |
| v0.1 | 10 | 100 | AVERAGE | 732.920 req/s | 74.177 | 43.482 | 13.199 | 10 | 132.375 | 251.413 | 296.463 | 0.000 | 10000 | 10000 | 10000 | 8900 | ❌ |  |  |
| v0.1 | 10 | 300 | AVERAGE | 694.974 req/s | 74.139 | 45.252 | 15.474 | 10 | 419.917 | 727.154 | 830.604 | 0.000 | 30000 | 30000 | 30000 | 26644 | ❌ |  |  |
| v0.1 | 10 | 500 | AVERAGE | 669.846 req/s | 71.050 | 39.281 | 17.853 | 10 | 741.527 | 1097.781 | 1217.627 | 0.000 | 50000 | 50000 | 50000 | 44537.200 | ❌ |  |  |
| v0.1 | 30 | 10 | AVERAGE | 657.690 req/s | 35.896 | 30.980 | 10.464 | 5.200 | 15.166 | 24.186 | 31.083 | 0.000 | 1000 | 1000 | 1000 | 839.600 | ❌ |  |  |
| v0.1 | 30 | 50 | AVERAGE | 450.443 req/s | 55.296 | 35.131 | 10.437 | 30 | 106.451 | 226.884 | 333.444 | 0.000 | 5000 | 5000 | 5000 | 4816 | ❌ |  |  |
| v0.1 | 30 | 100 | AVERAGE | 392.639 req/s | 48.033 | 22.581 | 11.063 | 30 | 249.590 | 445.355 | 584.345 | 0.000 | 10000 | 10000 | 10000 | 9665 | ❌ |  |  |
| v0.1 | 30 | 300 | AVERAGE | 332.578 req/s | 50.472 | 26.418 | 14.624 | 30 | 898.409 | 1422.323 | 1853.757 | 0.000 | 30000 | 30000 | 30000 | 28983.400 | ❌ |  |  |
| v0.1 | 30 | 500 | AVERAGE | 357.712 req/s | 49.396 | 23.918 | 16.566 | 30 | 1387.733 | 1978.055 | 2299.932 | 0.000 | 50000 | 50000 | 50000 | 48323.200 | ❌ |  |  |
| v0.1 | 50 | 10 | AVERAGE | 646.586 req/s | 27.866 | 23.385 | 10.040 | 3.400 | 15.454 | 24.900 | 31.328 | 0.000 | 1000 | 1000 | 1000 | 838.200 | ❌ |  |  |
| v0.1 | 50 | 50 | AVERAGE | 322.554 req/s | 56.864 | 36.259 | 10.769 | 49.800 | 148.466 | 413.422 | 663.177 | 0.000 | 5000 | 5000 | 5000 | 4876.400 | ❌ |  |  |
| v0.1 | 50 | 100 | AVERAGE | 228.577 req/s | 40.743 | 20.332 | 11.677 | 50 | 423.501 | 940.082 | 1343.563 | 0.000 | 10000 | 10000 | 10000 | 9798 | ❌ |  |  |
| v0.1 | 50 | 300 | AVERAGE | 184.781 req/s | 42.875 | 21.012 | 15.056 | 50 | 1606.764 | 2800.501 | 3457.635 | 0.041 | 30000 | 29999.800 | 30000 | 29403.200 | ❌ |  |  |
| v0.1 | 50 | 500 | AVERAGE | 175.817 req/s | 37.252 | 16.127 | 17.914 | 50 | 2831.160 | 4327.747 | 4820.181 | 2.521 | 50000 | 49999.800 | 50000 | 48998.400 | ❌ |  |  |

</details>

<details>
<summary><strong>v0.2</strong></summary>

| Version | Hikari Max Pool Size | VU | Run | TPS (req/s) | System CPU Peak (%) | Process CPU Peak (%) | JVM Heap Peak (%) | Hikari Active Peak | Avg Latency (ms) | P95 (ms) | P99 (ms) | Error Rate (%) | 초기 잔량 | 사용자 총 채굴량 | Mining Log 총 채굴량 | 실제 잔량 | 정합성 | Started At | Finished At |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| v0.2_optimistic-lock | 10 | 10 | AVERAGE | 736.274 req/s | 26.755 | 23.730 | 10.535 | 3.600 | 13.682 | 21.267 | 27.716 | 82.920 | 1000 | 170.800 | 170.800 | 829.200 | ✅ |  |  |
| v0.2_optimistic-lock | 10 | 50 | AVERAGE | 1182.808 req/s | 77.376 | 63.959 | 10.338 | 9.800 | 41.383 | 89.496 | 118.034 | 85.124 | 5000 | 743.800 | 743.800 | 4256.200 | ✅ |  |  |
| v0.2_optimistic-lock | 10 | 100 | AVERAGE | 1679.945 req/s | 94.312 | 67.255 | 10.721 | 10 | 56.643 | 125.625 | 171.584 | 85.974 | 10000 | 1402.600 | 1402.600 | 8597.400 | ✅ |  |  |
| v0.2_optimistic-lock | 10 | 300 | AVERAGE | 1669.453 req/s | 93.908 | 66.718 | 15.588 | 10 | 171.791 | 397.976 | 507.841 | 86.129 | 30000 | 4161.400 | 4161.400 | 25838.600 | ✅ |  |  |
| v0.2_optimistic-lock | 10 | 500 | AVERAGE | 1676.330 req/s | 94.032 | 66.250 | 18.542 | 10 | 289.851 | 519.593 | 627.200 | 86.112 | 50000 | 6944 | 6944 | 43056 | ✅ |  |  |
| v0.2_optimistic-lock | 30 | 10 | AVERAGE | 725.935 req/s | 22.047 | 18.847 | 10.666 | 3.200 | 13.980 | 22.074 | 28.641 | 83.160 | 1000 | 168.400 | 168.400 | 831.600 | ✅ |  |  |
| v0.2_optimistic-lock | 30 | 50 | AVERAGE | 1244.306 req/s | 81.947 | 68.663 | 11.134 | 23.400 | 39.462 | 71.717 | 89.527 | 89.616 | 5000 | 519.200 | 519.200 | 4480.800 | ✅ |  |  |
| v0.2_optimistic-lock | 30 | 100 | AVERAGE | 1728.068 req/s | 94.176 | 69.472 | 11.243 | 28.800 | 55.436 | 119.717 | 154.231 | 90.052 | 10000 | 994.800 | 994.800 | 9005.200 | ✅ |  |  |
| v0.2_optimistic-lock | 30 | 300 | AVERAGE | 1741.242 req/s | 95.278 | 68.753 | 16.243 | 30 | 165.238 | 367.788 | 466.412 | 90.105 | 30000 | 2968.400 | 2968.400 | 27031.600 | ✅ |  |  |
| v0.2_optimistic-lock | 30 | 500 | AVERAGE | 1722.364 req/s | 95.193 | 67.916 | 18.521 | 30 | 282.317 | 494.547 | 594.478 | 90.114 | 50000 | 4942.800 | 4942.800 | 45057.200 | ✅ |  |  |
| v0.2_optimistic-lock | 50 | 10 | AVERAGE | 689.171 req/s | 27.964 | 25.377 | 10.188 | 3.200 | 14.610 | 23.194 | 31.697 | 82.760 | 1000 | 172.400 | 172.400 | 827.600 | ✅ |  |  |
| v0.2_optimistic-lock | 50 | 50 | AVERAGE | 1208.122 req/s | 85.974 | 71.975 | 11.382 | 46.800 | 40.461 | 81.778 | 109.728 | 91.056 | 5000 | 447.200 | 447.200 | 4552.800 | ✅ |  |  |
| v0.2_optimistic-lock | 50 | 100 | AVERAGE | 1619.684 req/s | 92.755 | 67.615 | 12.286 | 49.600 | 59.725 | 116.708 | 153.808 | 91.078 | 10000 | 892.200 | 892.200 | 9107.800 | ✅ |  |  |
| v0.2_optimistic-lock | 50 | 300 | AVERAGE | 1657.747 req/s | 94.785 | 68.466 | 17.954 | 50 | 174.760 | 362.937 | 454.584 | 91.386 | 30000 | 2584.200 | 2584.200 | 27415.800 | ✅ |  |  |
| v0.2_optimistic-lock | 50 | 500 | AVERAGE | 1672.424 req/s | 92.230 | 65.084 | 20.347 | 50 | 291.838 | 486.765 | 578.360 | 91.562 | 50000 | 4219 | 4219 | 45781 | ✅ |  |  |
| v0.2_optimistic-lock | 10 | 10 | AVERAGE | 363.083 req/s | 70.731 | 55.032 | 11.317 | 7.200 | 27.176 | 55.370 | 73.575 | 0.000 | 1000 | 1000 | 1000 | 0 | ✅ |  |  |
| v0.2_optimistic-lock | 10 | 50 | AVERAGE | 514.927 req/s | 52.156 | 30.382 | 11.218 | 10 | 95.675 | 126.321 | 147.818 | 0.000 | 5000 | 5000 | 5000 | 0 | ✅ |  |  |
| v0.2_optimistic-lock | 10 | 100 | AVERAGE | 530.827 req/s | 47.019 | 23.185 | 12.585 | 10 | 186.050 | 216.353 | 256.685 | 0.000 | 10000 | 10000 | 10000 | 0 | ✅ |  |  |
| v0.2_optimistic-lock | 10 | 300 | AVERAGE | 519.312 req/s | 46.735 | 22.551 | 14.459 | 10 | 562.729 | 960.127 | 1225.905 | 0.000 | 30000 | 30000 | 30000 | 0 | ✅ |  |  |
| v0.2_optimistic-lock | 10 | 500 | AVERAGE | 528.602 req/s | 46.953 | 23.493 | 16.646 | 10 | 929.010 | 1320.908 | 1655.190 | 0.000 | 50000 | 50000 | 50000 | 0 | ✅ |  |  |
| v0.2_pessimistic-lock | 30 | 10 | AVERAGE | 368.724 req/s | 32.539 | 26.989 | 13.409 | 5.600 | 26.486 | 54.333 | 75.719 | 0.000 | 1000 | 1000 | 1000 | 0 | ✅ |  |  |
| v0.2_pessimistic-lock | 30 | 50 | AVERAGE | 514.916 req/s | 51.785 | 26.927 | 12.581 | 30 | 92.032 | 256.774 | 398.806 | 0.000 | 5000 | 5000 | 5000 | 0 | ✅ |  |  |
| v0.2_pessimistic-lock | 30 | 100 | AVERAGE | 527.502 req/s | 46.661 | 21.281 | 14.045 | 30 | 183.879 | 353.162 | 502.116 | 0.000 | 10000 | 10000 | 10000 | 0 | ✅ |  |  |
| v0.2_pessimistic-lock | 30 | 300 | AVERAGE | 521.704 req/s | 47.650 | 23.209 | 16.951 | 30 | 561.483 | 925.000 | 1174.809 | 0.000 | 30000 | 30000 | 30000 | 0 | ✅ |  |  |
| v0.2_pessimistic-lock | 30 | 500 | AVERAGE | 521.335 req/s | 47.206 | 22.514 | 18.784 | 30 | 943.456 | 1310.776 | 1560.288 | 0.000 | 50000 | 50000 | 50000 | 0 | ✅ |  |  |
| v0.2_pessimistic-lock | 50 | 10 | AVERAGE | 368.814 req/s | 58.048 | 43.733 | 11.351 | 7.400 | 26.508 | 58.647 | 90.743 | 0.000 | 1000 | 1000 | 1000 | 0 | ✅ |  |  |
| v0.2_pessimistic-lock | 50 | 50 | AVERAGE | 497.896 req/s | 53.206 | 29.052 | 12.406 | 49.800 | 93.620 | 307.657 | 482.103 | 0.000 | 5000 | 5000 | 5000 | 0 | ✅ |  |  |
| v0.2_pessimistic-lock | 50 | 100 | AVERAGE | 496.811 req/s | 49.124 | 23.429 | 12.786 | 50 | 193.964 | 422.403 | 611.442 | 0.000 | 10000 | 10000 | 10000 | 0 | ✅ |  |  |
| v0.2_pessimistic-lock | 50 | 300 | AVERAGE | 468.917 req/s | 47.186 | 22.583 | 15.399 | 50 | 627.296 | 1037.141 | 1281.682 | 0.000 | 30000 | 30000 | 30000 | 0 | ✅ |  |  |
| v0.2_pessimistic-lock | 50 | 500 | AVERAGE | 465.750 req/s | 47.316 | 24.160 | 17.797 | 50 | 1058.071 | 1470.929 | 1712.593 | 0.000 | 50000 | 50000 | 50000 | 0 | ✅ |  |  |

</details>  
<details>
<summary><strong>v0.3</strong></summary>   

| Record Type | Target Type | Target | Version | Hikari Max Pool Size | VU | Run | TPS (req/s) | System CPU Peak (%) | Process CPU Peak (%) | JVM Heap Peak (%) | Hikari Active Peak | Avg Latency (ms) | P95 (ms) | P99 (ms) | Error Rate (%) | Observed Lock Waits | Observed Lock Wait Total (ms) | Observed Lock Wait Avg (ms) | Observed Lock Wait Max (ms) | 초기 잔량 | 사용자 총 채굴량 | Mining Log 총 채굴량 | 실제 잔량 | 정합성 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| AVERAGE | LOAD_BALANCER | http://192.168.0.47/api | v0.3_atomic-update | 20 | 10 | AVERAGE | 345.606 req/s |  |  |  | 4.2 | 28.313 | 72.86 | 114.317 | 0 | 18.4 | 36.736 | 1.868 | 2.554 | 1000 | 1000 | 1000 | 0 | ✅ |
| AVERAGE | BACKEND | 192.168.0.41:8080 | v0.3_atomic-update |  | 10 | AVERAGE | 190.759 req/s | 36.401 | 33.468 | 12.273 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | BACKEND | 192.168.0.46:8080 | v0.3_atomic-update |  | 10 | AVERAGE | 190.759 req/s | 31.618 | 27.815 | 11.599 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | LOAD_BALANCER | http://192.168.0.47/api | v0.3_atomic-update | 20 | 50 | AVERAGE | 490.198 req/s |  |  |  | 20 | 98.382 | 197.951 | 261.369 | 0 | 143.6 | 163.11 | 1.133 | 2.125 | 5000 | 5000 | 5000 | 0 | ✅ |
| AVERAGE | BACKEND | 192.168.0.41:8080 | v0.3_atomic-update |  | 50 | AVERAGE | 259.370 req/s | 31.065 | 21.199 | 11.906 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | BACKEND | 192.168.0.46:8080 | v0.3_atomic-update |  | 50 | AVERAGE | 259.370 req/s | 31.149 | 20.52 | 12.603 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | LOAD_BALANCER | http://192.168.0.47/api | v0.3_atomic-update | 20 | 100 | AVERAGE | 506.089 req/s |  |  |  | 20 | 191.651 | 337.027 | 404.958 | 0 | 319.2 | 320.824 | 1.007 | 3.256 | 10000 | 10000 | 10000 | 0 | ✅ |
| AVERAGE | BACKEND | 192.168.0.41:8080 | v0.3_atomic-update |  | 100 | AVERAGE | 262.416 req/s | 22.72 | 11.156 | 14.816 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | BACKEND | 192.168.0.46:8080 | v0.3_atomic-update |  | 100 | AVERAGE | 262.416 req/s | 23.82 | 11.399 | 14.821 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | LOAD_BALANCER | http://192.168.0.47/api | v0.3_atomic-update | 20 | 300 | AVERAGE | 504.521 req/s |  |  |  | 20 | 574.167 | 1070.983 | 1657.26 | 0 | 867.2 | 927.442 | 1.069 | 3.184 | 30000 | 30000 | 30000 | 0 | ✅ |
| AVERAGE | BACKEND | 192.168.0.41:8080 | v0.3_atomic-update |  | 300 | AVERAGE | 255.981 req/s | 23.657 | 11.976 | 23.681 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | BACKEND | 192.168.0.46:8080 | v0.3_atomic-update |  | 300 | AVERAGE | 255.981 req/s | 24.306 | 12.789 | 23.617 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | LOAD_BALANCER | http://192.168.0.47/api | v0.3_atomic-update | 20 | 500 | AVERAGE | 502.994 req/s |  |  |  | 20 | 964.483 | 1844.406 | 2143.559 | 0 | 1511.6 | 1854.252 | 1.233 | 17.407 | 50000 | 50000 | 50000 | 0 | ✅ |
| AVERAGE | BACKEND | 192.168.0.41:8080 | v0.3_atomic-update |  | 500 | AVERAGE | 253.809 req/s | 23.703 | 12.161 | 28.8 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | BACKEND | 192.168.0.46:8080 | v0.3_atomic-update |  | 500 | AVERAGE | 253.809 req/s | 24.76 | 12.946 | 28.252 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | LOAD_BALANCER | http://192.168.0.47/api | v0.3_optimistic-lock | 20 | 10 | AVERAGE | 272.042 req/s |  |  |  | 5.4 | 31.788 | 114.661 | 455.75 | 0.02 | 0.6 | 0.26 | 0.26 | 0.26 | 1000 | 999.8 | 999.8 | 0.2 | ✅ |
| AVERAGE | BACKEND | 192.168.0.41:8080 | v0.3_optimistic-lock |  | 10 | AVERAGE | 148.990 req/s | 35.639 | 30.24 | 12.453 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | BACKEND | 192.168.0.46:8080 | v0.3_optimistic-lock |  | 10 | AVERAGE | 149.035 req/s | 46.405 | 40.06 | 11.935 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | LOAD_BALANCER | http://192.168.0.47/api | v0.3_optimistic-lock | 20 | 50 | AVERAGE | 372.790 req/s |  |  |  | 20 | 124.362 | 320.256 | 842.048 | 0.1 | 5.6 | 3.614 | 0.691 | 1.19 | 5000 | 4995 | 4995 | 5 | ✅ |
| AVERAGE | BACKEND | 192.168.0.41:8080 | v0.3_optimistic-lock |  | 50 | AVERAGE | 195.464 req/s | 38.526 | 22.834 | 13.77 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | BACKEND | 192.168.0.46:8080 | v0.3_optimistic-lock |  | 50 | AVERAGE | 195.449 req/s | 38.991 | 23.927 | 15.07 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | LOAD_BALANCER | http://192.168.0.47/api | v0.3_optimistic-lock | 20 | 100 | AVERAGE | 383.653 req/s |  |  |  | 20 | 247.753 | 511.831 | 1030.145 | 0.104 | 10.2 | 5.2 | 0.514 | 0.91 | 10000 | 9989.6 | 9989.6 | 10.4 | ✅ |
| AVERAGE | BACKEND | 192.168.0.41:8080 | v0.3_optimistic-lock |  | 100 | AVERAGE | 197.605 req/s | 35.245 | 18.492 | 17.057 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | BACKEND | 192.168.0.46:8080 | v0.3_optimistic-lock |  | 100 | AVERAGE | 197.605 req/s | 36.123 | 20.651 | 18.606 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | LOAD_BALANCER | http://192.168.0.47/api | v0.3_optimistic-lock | 20 | 300 | AVERAGE | 386.009 req/s |  |  |  | 20 | 743.866 | 1883.916 | 2388.514 | 0.139 | 23.6 | 16.325 | 0.687 | 2.36 | 30000 | 29958.4 | 29958.4 | 41.6 | ✅ |
| AVERAGE | BACKEND | 192.168.0.41:8080 | v0.3_optimistic-lock |  | 300 | AVERAGE | 195.259 req/s | 37.991 | 20.629 | 30.967 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | BACKEND | 192.168.0.46:8080 | v0.3_optimistic-lock |  | 300 | AVERAGE | 195.259 req/s | 37.71 | 21.226 | 30.662 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | LOAD_BALANCER | http://192.168.0.47/api | v0.3_optimistic-lock | 20 | 500 | AVERAGE | 385.291 req/s |  |  |  | 20 | 1236.794 | 3022.376 | 3505.934 | 0.128 | 48.2 | 47.912 | 0.965 | 8.989 | 50000 | 49935.8 | 49935.8 | 64.2 | ✅ |
| AVERAGE | BACKEND | 192.168.0.41:8080 | v0.3_optimistic-lock |  | 500 | AVERAGE | 194.041 req/s | 38.38 | 21.507 | 32.169 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | BACKEND | 192.168.0.46:8080 | v0.3_optimistic-lock |  | 500 | AVERAGE | 194.041 req/s | 38.339 | 20.931 | 32.158 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | LOAD_BALANCER | http://192.168.0.47/api | v0.3_pessimistic-lock | 20 | 10 | AVERAGE | 383.697 req/s |  |  |  | 5.6 | 25.852 | 42.367 | 49.695 | 0 | 16.2 | 26.947 | 1.671 | 2.57 | 1000 | 1000 | 1000 | 0 | ✅ |
| AVERAGE | BACKEND | 192.168.0.41:8080 | v0.3_pessimistic-lock |  | 10 | AVERAGE | 211.979 req/s | 29.998 | 24.931 | 12.057 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | BACKEND | 192.168.0.46:8080 | v0.3_pessimistic-lock |  | 10 | AVERAGE | 211.979 req/s | 25.019 | 21.8 | 12.533 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | LOAD_BALANCER | http://192.168.0.47/api | v0.3_pessimistic-lock | 20 | 50 | AVERAGE | 531.818 req/s |  |  |  | 20 | 90.6 | 184.618 | 229.063 | 0 | 156.6 | 223.071 | 1.43 | 2.865 | 5000 | 5000 | 5000 | 0 | ✅ |
| AVERAGE | BACKEND | 192.168.0.41:8080 | v0.3_pessimistic-lock |  | 50 | AVERAGE | 281.797 req/s | 34.754 | 25.669 | 13.109 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | BACKEND | 192.168.0.46:8080 | v0.3_pessimistic-lock |  | 50 | AVERAGE | 281.797 req/s | 33.353 | 23.579 | 12.625 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | LOAD_BALANCER | http://192.168.0.47/api | v0.3_pessimistic-lock | 20 | 100 | AVERAGE | 556.922 req/s |  |  |  | 20 | 173.02 | 343.742 | 405.055 | 0 | 316 | 408.938 | 1.294 | 2.752 | 10000 | 10000 | 10000 | 0 | ✅ |
| AVERAGE | BACKEND | 192.168.0.41:8080 | v0.3_pessimistic-lock |  | 100 | AVERAGE | 289.513 req/s | 23.241 | 11.126 | 16.627 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | BACKEND | 192.168.0.46:8080 | v0.3_pessimistic-lock |  | 100 | AVERAGE | 289.513 req/s | 22.717 | 11.081 | 15.707 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | LOAD_BALANCER | http://192.168.0.47/api | v0.3_pessimistic-lock | 20 | 300 | AVERAGE | 552.789 req/s |  |  |  | 20 | 520.033 | 1057.115 | 1640.65 | 0 | 946.8 | 1346.265 | 1.422 | 4.964 | 30000 | 30000 | 30000 | 0 | ✅ |
| AVERAGE | BACKEND | 192.168.0.41:8080 | v0.3_pessimistic-lock |  | 300 | AVERAGE | 280.838 req/s | 25.054 | 14.14 | 24.966 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | BACKEND | 192.168.0.46:8080 | v0.3_pessimistic-lock |  | 300 | AVERAGE | 280.838 req/s | 23.95 | 12.704 | 23.773 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | LOAD_BALANCER | http://192.168.0.47/api | v0.3_pessimistic-lock | 20 | 500 | AVERAGE | 554.501 req/s |  |  |  | 20 | 860.936 | 1840.621 | 2409.877 | 0 | 1572.6 | 2261.04 | 1.438 | 5.759 | 50000 | 50000 | 50000 | 0 | ✅ |
| AVERAGE | BACKEND | 192.168.0.41:8080 | v0.3_pessimistic-lock |  | 500 | AVERAGE | 280.051 req/s | 23.246 | 11.491 | 28.545 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | BACKEND | 192.168.0.46:8080 | v0.3_pessimistic-lock |  | 500 | AVERAGE | 280.051 req/s | 23.681 | 12.294 | 28.88 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |

</details>

<details>
<summary><strong>v0.4</strong></summary>  

| Record Type | Target Type | Target | Version | Hikari Max Pool Size | VU | Run | TPS (req/s) | System CPU Peak (%) | Process CPU Peak (%) | JVM Heap Peak (%) | Tomcat Available Threads Min | Tomcat Available Threads Avg | Hikari Active Peak | Avg Latency (ms) | P95 (ms) | P99 (ms) | Error Rate (%) | DB Observed Lock Waits | DB Lock Wait Total (ms) | DB Lock Wait Avg (ms) | DB Lock Wait Max (ms) | DB Lock Wait Poll Interval (s) | Redis Lock Wait Samples | Redis Lock Wait Total (ms) | Redis Lock Wait Avg (ms) | Redis Lock Wait Max (ms) | Redis Lock Timeout Count | 초기 잔량 | 사용자 총 채굴량 | Mining Log 총 채굴량 | 실제 잔량 | 정합성 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| AVERAGE | LOAD_BALANCER | http://192.168.0.47/api | v0.4 | 20 | 10 | AVERAGE | 213.416 req/s |  |  |  |  |  | 0.6 | 45.768 | 131.441 | 203.723 | 0 | 0 | 0 | 0 | 0 | 1 | 1085.082 | 42075.313 | 38.806 | 533.062 | 0 | 1000 | 1000 | 1000 | 0 | ✅ |
| AVERAGE | BACKEND | 192.168.0.41:8080 | v0.4 |  | 10 | AVERAGE | 115.867 req/s | 39.174 | 31.649 | 13.498 | 190.8 | 197.245 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | BACKEND | 192.168.0.46:8080 | v0.4 |  | 10 | AVERAGE | 115.867 req/s | 43.294 | 35.157 | 14.113 | 192.8 | 197.677 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | LOAD_BALANCER | http://192.168.0.47/api | v0.4 | 20 | 50 | AVERAGE | 289.518 req/s |  |  |  |  |  | 1.2 | 162.222 | 563.566 | 912.378 | 0 | 0 | 0 | 0 | 0 | 1 | 5201.795 | 818595.301 | 157.375 | 1962.832 | 0 | 5000 | 5000 | 5000 | 0 | ✅ |
| AVERAGE | BACKEND | 192.168.0.41:8080 | v0.4 |  | 50 | AVERAGE | 150.608 req/s | 31.515 | 20.34 | 16.2 | 149.8 | 173.749 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | BACKEND | 192.168.0.46:8080 | v0.4 |  | 50 | AVERAGE | 150.608 req/s | 33.057 | 20.451 | 16.188 | 174 | 193.823 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | LOAD_BALANCER | http://192.168.0.47/api | v0.4 | 20 | 100 | AVERAGE | 300.186 req/s |  |  |  |  |  | 1.6 | 311.812 | 1051.217 | 1714.346 | 0 | 0 | 0 | 0 | 0 | 1 | 10245.122 | 3144052.474 | 306.883 | 3623.164 | 0 | 10000 | 10000 | 10000 | 0 | ✅ |
| AVERAGE | BACKEND | 192.168.0.41:8080 | v0.4 |  | 100 | AVERAGE | 153.772 req/s | 27.07 | 15.256 | 19.622 | 103.4 | 146.133 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | BACKEND | 192.168.0.46:8080 | v0.4 |  | 100 | AVERAGE | 153.772 req/s | 26.287 | 13.742 | 18.861 | 121.2 | 176.992 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | LOAD_BALANCER | http://192.168.0.47/api | v0.4 | 20 | 300 | AVERAGE | 294.593 req/s |  |  |  |  |  | 2 | 956.553 | 3208.057 | 4695.147 | 0 | 0 | 0 | 0 | 0 | 1 | 30273.737 | 21581329.62 | 712.873 | 9634.88 | 0 | 30000 | 30000 | 30000 | 0 | ✅ |
| AVERAGE | BACKEND | 192.168.0.41:8080 | v0.4 |  | 300 | AVERAGE | 148.641 req/s | 27.071 | 14.44 | 28.781 | 0 | 25.702 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | BACKEND | 192.168.0.46:8080 | v0.4 |  | 300 | AVERAGE | 148.641 req/s | 27.669 | 15.166 | 23.699 | 49.8 | 178.934 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | LOAD_BALANCER | http://192.168.0.47/api | v0.4 | 20 | 500 | AVERAGE | 295.189 req/s |  |  |  |  |  | 1.8 | 1598.63 | 4617.073 | 6052.617 | 0 | 0 | 0 | 0 | 0 | 1 | 50280.91 | 35761052.48 | 711.225 | 11359.543 | 0 | 50000 | 50000 | 50000 | 0 | ✅ |
| AVERAGE | BACKEND | 192.168.0.41:8080 | v0.4 |  | 500 | AVERAGE | 148.438 req/s | 26.798 | 13.887 | 32.586 | 0 | 91.574 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| AVERAGE | BACKEND | 192.168.0.46:8080 | v0.4 |  | 500 | AVERAGE | 148.424 req/s | 27.702 | 14.501 | 29.08 | 0 | 186.655 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
</details>

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
| `POSTGRES_URL` | PostgreSQL JDBC URL |
| `POSTGRES_USER` | 애플리케이션에서 사용할 PostgreSQL 사용자명 |
| `POSTGRES_PASSWORD` | PostgreSQL 비밀번호 |
| `POSTGRES_DB` | PostgreSQL 데이터베이스 이름 |
| `DB_CONNECTION_POOL_SIZE` | HikariCP 최대 데이터베이스 커넥션 수 (기본값: `50`) |

- 애플리케이션용 Compose 파일: `app/compose.yml`
- 독립 PostgreSQL 인프라 Compose 파일: `infra/db/compose.yml`
- 초기 DB 스키마: `infra/db/init/001-schema.sql`
- 애플리케이션 포트: `8080`

HikariCP의 최대 커넥션 풀 크기는 `DB_CONNECTION_POOL_SIZE`로 조정할 수 있으며 기본값은 `50`입니다.

> 현재 두 Compose 설정의 환경 변수 이름과 애플리케이션 datasource 설정은 완전히 통일되지 않은 상태입니다.

## Container Image

`main` 브랜치나 `v*` 태그를 GitHub에 푸시하면 GitHub Actions가 애플리케이션 이미지를 빌드하여 GHCR에 게시합니다.

```bash
docker pull ghcr.io/yeoooo/gold-rush-lab:latest
```

비공개 패키지는 먼저 GitHub PAT로 로그인해야 합니다.

```bash
echo "$GHCR_TOKEN" | docker login ghcr.io -u yeoooo --password-stdin
```

컨테이너 실행 시 데이터베이스 접속 정보를 환경 변수로 전달합니다.

```bash
docker run --rm -p 8080:8080 \
  -e POSTGRES_URL=jdbc:postgresql://host.docker.internal:5432/goldrush \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=goldrush \
  ghcr.io/yeoooo/gold-rush-lab:latest
```

### Docker Compose로 실행

애플리케이션과 PostgreSQL을 함께 실행하려면 `app` 디렉터리에서 `container` 프로필을 활성화합니다.

```bash
cd app

POSTGRES_DB=goldrush \
POSTGRES_USER=goldrush \
POSTGRES_PASSWORD=change-me \
DB_CONNECTION_POOL_SIZE=30 \
docker compose --profile container up -d
```

애플리케이션은 PostgreSQL의 health check가 통과된 후 시작됩니다.

| Environment Variable | Default | Description |
| --- | --- | --- |
| `APP_IMAGE_TAG` | `v0.1` | 실행할 애플리케이션 이미지 태그 |
| `APP_PORT` | `8080` | 호스트에 노출할 애플리케이션 포트 |
| `DB_CONNECTION_POOL_SIZE` | `50` | HikariCP 최대 데이터베이스 커넥션 수 |

## Load Test

k6를 사용하여 Smoke, Hot Spot, Capacity, Stress, Soak 시나리오를 실행할 수 있습니다. Hot Spot 시나리오는 여러 사용자의 요청을 하나의 광산에 집중시켜 Lost Update와 처리 성능을 확인합니다.

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

### 데이터 정합성 검증

부하 테스트 후 `infra/db/assets/verification.sql`을 사용하여 광산의 초기 잔량, 실제 채굴량, 채굴 후 잔량, 광산별 채굴 로그 및 PostgreSQL wait 상태를 확인할 수 있습니다.

> 검증 파일 앞부분에는 테이블 초기화와 시퀀스 재설정 쿼리가 포함되어 있으므로 실행 범위를 확인해야 합니다.

## Monitoring

Spring Boot Actuator와 Micrometer가 애플리케이션 및 채굴 메트릭을 노출하고, Prometheus와 Grafana가 이를 수집·시각화합니다.

- HTTP TPS, 평균 응답 시간, P95 / P99 지연 시간
- 채굴 성공·실패 횟수
- 락 타임아웃, 데드락, 낙관적 락 재시도 횟수
- 락 획득 호출 지연 시간
- JVM, 프로세스 및 시스템 CPU 상태

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
│       │   │   ├── mine
│       │   │   ├── mining_log
│       │   │   ├── observability
│       │   │   ├── presentation
│       │   │   ├── user
│       │   │   └── GoldRushLabApplication.java
│       │   └── resources/application.yml
│       └── test
└── infra
    ├── compose.yml
    ├── db
    │   ├── assets/verification.sql
    │   ├── compose.yml
    │   └── init/001-schema.sql
    ├── load-test
    │   ├── lib
    │   ├── scenarios
    │   │   ├── smoke.js
    │   │   ├── hotspot.js
    │   │   ├── capacity.js
    │   │   ├── stress.js
    │   │   └── soak.js
    │   └── run.sh
    └── monitoring
        ├── compose.yml
        ├── grafana
        └── prometheus
```
