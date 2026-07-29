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

**현재 버전: `v0.4` — Distributed Lock**
- 전체 결과 표 : [스프레드 시트](https://docs.google.com/spreadsheets/d/1KEnCXDi56xy9ztNQ00YJJ60xO8q4opV7UBKE_EvPc1w/edit?gid=347977445#gid=347977445)
- v0.1 결과 : [[Project : Gold-Rush-Lab] 1. 모놀리식에서의 동시성과 부하](https://yeoooo.github.io/project/gold-rush-lab-monolith-concurrency-load/)
- v0.2 결과 : [[Project : Gold-Rush-Lab] 2. 동시성 문제에서의 락](https://yeoooo.github.io/project/gold-rush-lab-database-lock/)
- v0.3 결과 : [[Project : Gold-Rush-Lab] 3. 분산 시스템에서의 Lock](https://yeoooo.github.io/project/gold-rush-lab-distributed-lock/)
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

| Target Type | Target | Version | Hikari Max Pool Size | VU | Run | TPS (req/s) | System CPU Peak (%) | Process CPU Peak (%) | JVM Heap Peak (%) | Hikari Active Peak | Avg Latency (ms) | P95 (ms) | P99 (ms) | Error Rate (%) | 초기 잔량 | 사용자 총 채굴량 | Mining Log 총 채굴량 | 실제 잔량 | 정합성 | Started At | Finished At |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| LOAD_BALANCER | http://192.168.0.47/api | v0.3_optimistic-lock-ps10 | 20 | 10 | AVERAGE | 1799.211 req/s |  |  |  | 0 | 5.267 | 7.783 | 11.045 | 83.920 | 1000 | 160.800 | 160.800 | 839.200 | ✅ |  |  |
| BACKEND | 192.168.0.41:8080 | v0.3_optimistic-lock-ps10 |  | 10 | AVERAGE | 174.638 req/s | 2.177 | 1.096 | 18.043 |  |  |  |  |  |  |  |  |  |  |  |  |
| BACKEND | 192.168.0.46:8080 | v0.3_optimistic-lock-ps10 |  | 10 | AVERAGE | 221.131 req/s | 13.179 | 7.661 | 18.545 |  |  |  |  |  |  |  |  |  |  |  |  |
| LOAD_BALANCER | http://192.168.0.47/api | v0.3_optimistic-lock-ps10 | 20 | 50 | AVERAGE | 1921.046 req/s |  |  |  | 15.800 | 24.814 | 53.275 | 72.281 | 90.616 | 5000 | 469.200 | 469.200 | 4530.800 | ✅ |  |  |
| BACKEND | 192.168.0.41:8080 | v0.3_optimistic-lock-ps10 |  | 50 | AVERAGE | 1015.406 req/s | 49.197 | 28.930 | 17.350 |  |  |  |  |  |  |  |  |  |  |  |  |
| BACKEND | 192.168.0.46:8080 | v0.3_optimistic-lock-ps10 |  | 50 | AVERAGE | 916.578 req/s | 69.547 | 43.482 | 19.654 |  |  |  |  |  |  |  |  |  |  |  |  |
| LOAD_BALANCER | http://192.168.0.47/api | v0.3_optimistic-lock-ps10 | 20 | 100 | AVERAGE | 1856.950 req/s |  |  |  | 19.200 | 51.528 | 112.818 | 163.637 | 91.024 | 10000 | 897.600 | 897.600 | 9102.400 | ✅ |  |  |
| BACKEND | 192.168.0.41:8080 | v0.3_optimistic-lock-ps10 |  | 100 | AVERAGE | 986.397 req/s | 68.954 | 40.497 | 18.101 |  |  |  |  |  |  |  |  |  |  |  |  |
| BACKEND | 192.168.0.46:8080 | v0.3_optimistic-lock-ps10 |  | 100 | AVERAGE | 965.779 req/s | 79.410 | 49.769 | 20.150 |  |  |  |  |  |  |  |  |  |  |  |  |
| LOAD_BALANCER | http://192.168.0.47/api | v0.3_optimistic-lock-ps10 | 20 | 300 | AVERAGE | 1741.941 req/s |  |  |  | 20 | 164.207 | 427.949 | 562.757 | 91.600 | 30000 | 2520 | 2520 | 27480 | ✅ |  |  |
| BACKEND | 192.168.0.41:8080 | v0.3_optimistic-lock-ps10 |  | 300 | AVERAGE | 896.803 req/s | 72.213 | 43.019 | 23.796 |  |  |  |  |  |  |  |  |  |  |  |  |
| BACKEND | 192.168.0.46:8080 | v0.3_optimistic-lock-ps10 |  | 300 | AVERAGE | 908.279 req/s | 79.240 | 50.194 | 23.284 |  |  |  |  |  |  |  |  |  |  |  |  |
| LOAD_BALANCER | http://192.168.0.47/api | v0.3_optimistic-lock-ps10 | 20 | 500 | AVERAGE | 1721.537 req/s |  |  |  | 20 | 277.923 | 671.125 | 860.101 | 91.575 | 50000 | 4212.400 | 4212.400 | 45787.600 | ✅ |  |  |
| BACKEND | 192.168.0.41:8080 | v0.3_optimistic-lock-ps10 |  | 500 | AVERAGE | 884.448 req/s | 72.947 | 43.250 | 26.581 |  |  |  |  |  |  |  |  |  |  |  |  |
| BACKEND | 192.168.0.46:8080 | v0.3_optimistic-lock-ps10 |  | 500 | AVERAGE | 884.448 req/s | 76.330 | 47.036 | 25.257 |  |  |  |  |  |  |  |  |  |  |  |  |
| LOAD_BALANCER | http://192.168.0.47/api | v0.3_optimistic-lock-ps30 | 60 | 10 | AVERAGE | 747.757 req/s |  |  |  | 4.400 | 13.523 | 21.798 | 26.583 | 82.020 | 1000 | 179.800 | 179.800 | 820.200 | ✅ |  |  |
| BACKEND | 192.168.0.41:8080 | v0.3_optimistic-lock-ps30 |  | 10 | AVERAGE | 362.163 req/s | 39.221 | 36.526 | 10.257 |  |  |  |  |  |  |  |  |  |  |  |  |
| BACKEND | 192.168.0.46:8080 | v0.3_optimistic-lock-ps30 |  | 10 | AVERAGE | 350.223 req/s | 41.256 | 37.914 | 11.260 |  |  |  |  |  |  |  |  |  |  |  |  |
| LOAD_BALANCER | http://192.168.0.47/api | v0.3_optimistic-lock-ps30 | 60 | 50 | AVERAGE | 1398.874 req/s |  |  |  | 30.600 | 34.191 | 90.929 | 127.042 | 89.724 | 5000 | 513.800 | 513.800 | 4486.200 | ✅ |  |  |
| BACKEND | 192.168.0.41:8080 | v0.3_optimistic-lock-ps30 |  | 50 | AVERAGE | 723.849 req/s | 58.700 | 46.214 | 12.308 |  |  |  |  |  |  |  |  |  |  |  |  |
| BACKEND | 192.168.0.46:8080 | v0.3_optimistic-lock-ps30 |  | 50 | AVERAGE | 718.939 req/s | 76.433 | 64.471 | 12.538 |  |  |  |  |  |  |  |  |  |  |  |  |
| LOAD_BALANCER | http://192.168.0.47/api | v0.3_optimistic-lock-ps30 | 60 | 100 | AVERAGE | 1480.528 req/s |  |  |  | 60 | 63.700 | 156.698 | 293.412 | 94.840 | 10000 | 516 | 516 | 9484 | ✅ |  |  |
| BACKEND | 192.168.0.41:8080 | v0.3_optimistic-lock-ps30 |  | 100 | AVERAGE | 766.163 req/s | 68.057 | 42.094 | 13.041 |  |  |  |  |  |  |  |  |  |  |  |  |
| BACKEND | 192.168.0.46:8080 | v0.3_optimistic-lock-ps30 |  | 100 | AVERAGE | 765.078 req/s | 70.734 | 45.321 | 14.036 |  |  |  |  |  |  |  |  |  |  |  |  |
| LOAD_BALANCER | http://192.168.0.47/api | v0.3_optimistic-lock-ps30 | 60 | 300 | AVERAGE | 1394.036 req/s |  |  |  | 60 | 205.539 | 524.355 | 724.227 | 95.327 | 30000 | 1402 | 1402 | 28598 | ✅ |  |  |
| BACKEND | 192.168.0.41:8080 | v0.3_optimistic-lock-ps30 |  | 300 | AVERAGE | 721.861 req/s | 65.015 | 38.801 | 23.698 |  |  |  |  |  |  |  |  |  |  |  |  |
| BACKEND | 192.168.0.46:8080 | v0.3_optimistic-lock-ps30 |  | 300 | AVERAGE | 721.961 req/s | 72.988 | 47.385 | 23.261 |  |  |  |  |  |  |  |  |  |  |  |  |
| LOAD_BALANCER | http://192.168.0.47/api | v0.3_optimistic-lock-ps30 | 60 | 500 | AVERAGE | 1294.716 req/s |  |  |  | 60 | 370.134 | 830.996 | 1077.349 | 95.484 | 50000 | 2258.200 | 2258.200 | 47741.800 | ✅ |  |  |
| BACKEND | 192.168.0.41:8080 | v0.3_optimistic-lock-ps30 |  | 500 | AVERAGE | 661.522 req/s | 63.145 | 37.005 | 26.888 |  |  |  |  |  |  |  |  |  |  |  |  |
| BACKEND | 192.168.0.46:8080 | v0.3_optimistic-lock-ps30 |  | 500 | AVERAGE | 661.522 req/s | 66.980 | 40.282 | 26.461 |  |  |  |  |  |  |  |  |  |  |  |  |
| LOAD_BALANCER | http://192.168.0.47/api | v0.3_optimistic-lock-ps50 | 100 | 10 | AVERAGE | 769.859 req/s |  |  |  | 1.600 | 13.110 | 20.007 | 24.176 | 82.820 | 1000 | 171.800 | 171.800 | 828.200 | ✅ |  |  |
| BACKEND | 192.168.0.41:8080 | v0.3_optimistic-lock-ps50 |  | 10 | AVERAGE | 251.085 req/s | 21.024 | 18.801 | 11.209 |  |  |  |  |  |  |  |  |  |  |  |  |
| BACKEND | 192.168.0.46:8080 | v0.3_optimistic-lock-ps50 |  | 10 | AVERAGE | 119.685 req/s | 21.147 | 18.458 | 11.980 |  |  |  |  |  |  |  |  |  |  |  |  |
| LOAD_BALANCER | http://192.168.0.47/api | v0.3_optimistic-lock-ps50 | 100 | 50 | AVERAGE | 1320.466 req/s |  |  |  | 40.200 | 35.683 | 105.170 | 159.683 | 91.596 | 5000 | 420.200 | 420.200 | 4579.800 | ✅ |  |  |
| BACKEND | 192.168.0.41:8080 | v0.3_optimistic-lock-ps50 |  | 50 | AVERAGE | 672.302 req/s | 61.092 | 48.379 | 12.210 |  |  |  |  |  |  |  |  |  |  |  |  |
| BACKEND | 192.168.0.46:8080 | v0.3_optimistic-lock-ps50 |  | 50 | AVERAGE | 635.454 req/s | 73.735 | 61.405 | 13.749 |  |  |  |  |  |  |  |  |  |  |  |  |
| LOAD_BALANCER | http://192.168.0.47/api | v0.3_optimistic-lock-ps50 | 100 | 100 | AVERAGE | 1476.870 req/s |  |  |  | 97.200 | 62.582 | 168.328 | 355.128 | 95.872 | 10000 | 412.800 | 412.800 | 9587.200 | ✅ |  |  |
| BACKEND | 192.168.0.41:8080 | v0.3_optimistic-lock-ps50 |  | 100 | AVERAGE | 761.210 req/s | 65.312 | 38.155 | 12.775 |  |  |  |  |  |  |  |  |  |  |  |  |
| BACKEND | 192.168.0.46:8080 | v0.3_optimistic-lock-ps50 |  | 100 | AVERAGE | 777.957 req/s | 72.358 | 47.393 | 16.147 |  |  |  |  |  |  |  |  |  |  |  |  |
| LOAD_BALANCER | http://192.168.0.47/api | v0.3_optimistic-lock-ps50 | 100 | 300 | AVERAGE | 1262.679 req/s |  |  |  | 100 | 227.992 | 608.053 | 944.064 | 95.993 | 30000 | 1202.200 | 1202.200 | 28797.800 | ✅ |  |  |
| BACKEND | 192.168.0.41:8080 | v0.3_optimistic-lock-ps50 |  | 300 | AVERAGE | 652.214 req/s | 63.839 | 37.473 | 23.494 |  |  |  |  |  |  |  |  |  |  |  |  |
| BACKEND | 192.168.0.46:8080 | v0.3_optimistic-lock-ps50 |  | 300 | AVERAGE | 651.110 req/s | 65.608 | 40.624 | 25.069 |  |  |  |  |  |  |  |  |  |  |  |  |
| LOAD_BALANCER | http://192.168.0.47/api | v0.3_optimistic-lock-ps50 | 100 | 500 | AVERAGE | 964.884 req/s |  |  |  | 100 | 531.091 | 1297.146 | 1775.135 | 96.015 | 50000 | 1992.400 | 1992.400 | 48007.600 | ✅ |  |  |
| BACKEND | 192.168.0.41:8080 | v0.3_optimistic-lock-ps50 |  | 500 | AVERAGE | 491.069 req/s | 62.247 | 35.548 | 28.371 |  |  |  |  |  |  |  |  |  |  |  |  |
| BACKEND | 192.168.0.46:8080 | v0.3_optimistic-lock-ps50 |  | 500 | AVERAGE | 491.069 req/s | 66.251 | 40.890 | 28.406 |  |  |  |  |  |  |  |  |  |  |  |  |
| LOAD_BALANCER | http://192.168.0.47/api | v0.3_pessimistic-lock-ps10 | 20 | 10 | AVERAGE | 318.060 req/s |  |  |  | 7.400 | 31.109 | 54.657 | 72.490 | 0.000 | 1000 | 1000 | 1000 | 0 | ✅ |  |  |
| BACKEND | 192.168.0.41:8080 | v0.3_pessimistic-lock-ps10 |  | 10 | AVERAGE | 157.046 req/s | 40.926 | 32.254 | 12.243 |  |  |  |  |  |  |  |  |  |  |  |  |
| BACKEND | 192.168.0.46:8080 | v0.3_pessimistic-lock-ps10 |  | 10 | AVERAGE | 148.454 req/s | 44.154 | 36.495 | 11.310 |  |  |  |  |  |  |  |  |  |  |  |  |
| LOAD_BALANCER | http://192.168.0.47/api | v0.3_pessimistic-lock-ps10 | 20 | 50 | AVERAGE | 485.306 req/s |  |  |  | 18.200 | 98.731 | 202.144 | 256.738 | 0.000 | 5000 | 5000 | 5000 | 0 | ✅ |  |  |
| BACKEND | 192.168.0.41:8080 | v0.3_pessimistic-lock-ps10 |  | 50 | AVERAGE | 254.774 req/s | 32.467 | 17.316 | 12.829 |  |  |  |  |  |  |  |  |  |  |  |  |
| BACKEND | 192.168.0.46:8080 | v0.3_pessimistic-lock-ps10 |  | 50 | AVERAGE | 253.964 req/s | 36.708 | 20.751 | 11.317 |  |  |  |  |  |  |  |  |  |  |  |  |
| LOAD_BALANCER | http://192.168.0.47/api | v0.3_pessimistic-lock-ps10 | 20 | 100 | AVERAGE | 511.007 req/s |  |  |  | 19.800 | 185.511 | 385.803 | 450.694 | 0.000 | 10000 | 10000 | 10000 | 0 | ✅ |  |  |
| BACKEND | 192.168.0.41:8080 | v0.3_pessimistic-lock-ps10 |  | 100 | AVERAGE | 265.303 req/s | 29.280 | 12.585 | 16.012 |  |  |  |  |  |  |  |  |  |  |  |  |
| BACKEND | 192.168.0.46:8080 | v0.3_pessimistic-lock-ps10 |  | 100 | AVERAGE | 263.624 req/s | 31.209 | 13.609 | 13.644 |  |  |  |  |  |  |  |  |  |  |  |  |
| LOAD_BALANCER | http://192.168.0.47/api | v0.3_pessimistic-lock-ps10 | 20 | 300 | AVERAGE | 506.906 req/s |  |  |  | 20 | 558.272 | 1197.953 | 1898.718 | 0.000 | 30000 | 30000 | 30000 | 0 | ✅ |  |  |
| BACKEND | 192.168.0.41:8080 | v0.3_pessimistic-lock-ps10 |  | 300 | AVERAGE | 256.780 req/s | 33.371 | 16.054 | 25.346 |  |  |  |  |  |  |  |  |  |  |  |  |
| BACKEND | 192.168.0.46:8080 | v0.3_pessimistic-lock-ps10 |  | 300 | AVERAGE | 257.317 req/s | 32.831 | 14.966 | 21.018 |  |  |  |  |  |  |  |  |  |  |  |  |
| LOAD_BALANCER | http://192.168.0.47/api | v0.3_pessimistic-lock-ps10 | 20 | 500 | AVERAGE | 507.725 req/s |  |  |  | 20 | 930.839 | 1986.360 | 2685.148 | 0.000 | 50000 | 50000 | 50000 | 0 | ✅ |  |  |
| BACKEND | 192.168.0.41:8080 | v0.3_pessimistic-lock-ps10 |  | 500 | AVERAGE | 256.267 req/s | 30.439 | 12.945 | 27.459 |  |  |  |  |  |  |  |  |  |  |  |  |
| BACKEND | 192.168.0.46:8080 | v0.3_pessimistic-lock-ps10 |  | 500 | AVERAGE | 256.267 req/s | 33.351 | 14.933 | 25.746 |  |  |  |  |  |  |  |  |  |  |  |  |

#### v0.3 최종 Scale-out 결과

| Target Type | Target | Version | Hikari Max Pool Size | VU | Run | TPS (req/s) | System CPU Peak (%) | Process CPU Peak (%) | JVM Heap Peak (%) | Hikari Active Peak | Avg Latency (ms) | P95 (ms) | P99 (ms) | Error Rate (%) | 초기 잔량 | 사용자 총 채굴량 | Mining Log 총 채굴량 | 실제 잔량 | 정합성 | Optimistic Retry Count | Started At | Finished At |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| LOAD_BALANCER | http://192.168.0.47/api | v0.3_optimistic-lock | 20 | 10 | AVERAGE | 264.572 req/s |  |  |  | 4.400 | 32.050 | 115.146 | 435.568 | 0.000 | 1000 | 1000 | 1000 | 0 | ✅ | 1363 |  |  |
| BACKEND | 192.168.0.41:8080 | v0.3_optimistic-lock | 10 | 10 | AVERAGE | 125.386 req/s | 37.393 | 26.938 | 10.162 |  |  |  |  |  |  |  |  |  |  |  |  |  |
| BACKEND | 192.168.0.46:8080 | v0.3_optimistic-lock | 10 | 10 | AVERAGE | 124.266 req/s | 44.753 | 33.397 | 11.460 |  |  |  |  |  |  |  |  |  |  |  |  |  |
| LOAD_BALANCER | http://192.168.0.47/api | v0.3_optimistic-lock | 20 | 50 | AVERAGE | 347.883 req/s |  |  |  | 19.200 | 133.117 | 345.316 | 887.712 | 0.144 | 5000 | 4992.800 | 4992.800 | 7.200 | ✅ | 10553.400 |  |  |
| BACKEND | 192.168.0.41:8080 | v0.3_optimistic-lock | 10 | 50 | AVERAGE | 181.524 req/s | 49.927 | 26.384 | 12.825 |  |  |  |  |  |  |  |  |  |  |  |  |  |
| BACKEND | 192.168.0.46:8080 | v0.3_optimistic-lock | 10 | 50 | AVERAGE | 178.788 req/s | 50.951 | 26.462 | 13.256 |  |  |  |  |  |  |  |  |  |  |  |  |  |
| LOAD_BALANCER | http://192.168.0.47/api | v0.3_optimistic-lock | 20 | 100 | AVERAGE | 360.067 req/s |  |  |  | 20 | 263.910 | 536.261 | 1074.603 | 0.144 | 10000 | 9985.600 | 9985.600 | 14.400 | ✅ | 21234.800 |  |  |
| BACKEND | 192.168.0.41:8080 | v0.3_optimistic-lock | 10 | 100 | AVERAGE | 182.420 req/s | 49.790 | 24.085 | 17.928 |  |  |  |  |  |  |  |  |  |  |  |  |  |
| BACKEND | 192.168.0.46:8080 | v0.3_optimistic-lock | 10 | 100 | AVERAGE | 182.393 req/s | 51.188 | 24.569 | 18.034 |  |  |  |  |  |  |  |  |  |  |  |  |  |
| LOAD_BALANCER | http://192.168.0.47/api | v0.3_optimistic-lock | 20 | 300 | AVERAGE | 360.716 req/s |  |  |  | 20 | 794.664 | 2010.488 | 2629.693 | 0.173 | 30000 | 29948 | 29948 | 52 | ✅ | 63644 |  |  |
| BACKEND | 192.168.0.41:8080 | v0.3_optimistic-lock | 10 | 300 | AVERAGE | 182.095 req/s | 52.728 | 25.593 | 29.021 |  |  |  |  |  |  |  |  |  |  |  |  |  |
| BACKEND | 192.168.0.46:8080 | v0.3_optimistic-lock | 10 | 300 | AVERAGE | 181.716 req/s | 53.581 | 26.676 | 28.929 |  |  |  |  |  |  |  |  |  |  |  |  |  |
| LOAD_BALANCER | http://192.168.0.47/api | v0.3_optimistic-lock | 20 | 500 | AVERAGE | 358.772 req/s |  |  |  | 20 | 1328.692 | 3061.369 | 3713.067 | 0.178 | 50000 | 49916 | 49916 | 84 | ✅ | 105659.400 |  |  |
| BACKEND | 192.168.0.41:8080 | v0.3_optimistic-lock | 10 | 500 | AVERAGE | 180.611 req/s | 51.842 | 24.320 | 31.517 |  |  |  |  |  |  |  |  |  |  |  |  |  |
| BACKEND | 192.168.0.46:8080 | v0.3_optimistic-lock | 10 | 500 | AVERAGE | 180.608 req/s | 51.845 | 25.056 | 31.869 |  |  |  |  |  |  |  |  |  |  |  |  |  |
| LOAD_BALANCER | http://192.168.0.47/api | v0.3_pessimistic-lock | 20 | 10 | AVERAGE | 318.060 req/s |  |  |  | 7.400 | 31.109 | 54.657 | 72.490 | 0.000 | 1000 | 1000 | 1000 | 0 | ✅ |  |  |  |
| BACKEND | 192.168.0.41:8080 | v0.3_pessimistic-lock | 10 | 10 | AVERAGE | 157.046 req/s | 40.926 | 32.254 | 12.243 |  |  |  |  |  |  |  |  |  |  |  |  |  |
| BACKEND | 192.168.0.46:8080 | v0.3_pessimistic-lock | 10 | 10 | AVERAGE | 148.454 req/s | 44.154 | 36.495 | 11.310 |  |  |  |  |  |  |  |  |  |  |  |  |  |
| LOAD_BALANCER | http://192.168.0.47/api | v0.3_pessimistic-lock | 20 | 50 | AVERAGE | 485.306 req/s |  |  |  | 18.200 | 98.731 | 202.144 | 256.738 | 0.000 | 5000 | 5000 | 5000 | 0 | ✅ |  |  |  |
| BACKEND | 192.168.0.41:8080 | v0.3_pessimistic-lock | 10 | 50 | AVERAGE | 254.774 req/s | 32.467 | 17.316 | 12.829 |  |  |  |  |  |  |  |  |  |  |  |  |  |
| BACKEND | 192.168.0.46:8080 | v0.3_pessimistic-lock | 10 | 50 | AVERAGE | 253.964 req/s | 36.708 | 20.751 | 11.317 |  |  |  |  |  |  |  |  |  |  |  |  |  |
| LOAD_BALANCER | http://192.168.0.47/api | v0.3_pessimistic-lock | 20 | 100 | AVERAGE | 511.007 req/s |  |  |  | 19.800 | 185.511 | 385.803 | 450.694 | 0.000 | 10000 | 10000 | 10000 | 0 | ✅ |  |  |  |
| BACKEND | 192.168.0.41:8080 | v0.3_pessimistic-lock | 10 | 100 | AVERAGE | 265.303 req/s | 29.280 | 12.585 | 16.012 |  |  |  |  |  |  |  |  |  |  |  |  |  |
| BACKEND | 192.168.0.46:8080 | v0.3_pessimistic-lock | 10 | 100 | AVERAGE | 263.624 req/s | 31.209 | 13.609 | 13.644 |  |  |  |  |  |  |  |  |  |  |  |  |  |
| LOAD_BALANCER | http://192.168.0.47/api | v0.3_pessimistic-lock | 20 | 300 | AVERAGE | 506.906 req/s |  |  |  | 20 | 558.272 | 1197.953 | 1898.718 | 0.000 | 30000 | 30000 | 30000 | 0 | ✅ |  |  |  |
| BACKEND | 192.168.0.41:8080 | v0.3_pessimistic-lock | 10 | 300 | AVERAGE | 256.780 req/s | 33.371 | 16.054 | 25.346 |  |  |  |  |  |  |  |  |  |  |  |  |  |
| BACKEND | 192.168.0.46:8080 | v0.3_pessimistic-lock | 10 | 300 | AVERAGE | 257.317 req/s | 32.831 | 14.966 | 21.018 |  |  |  |  |  |  |  |  |  |  |  |  |  |
| LOAD_BALANCER | http://192.168.0.47/api | v0.3_pessimistic-lock | 20 | 500 | AVERAGE | 507.725 req/s |  |  |  | 20 | 930.839 | 1986.360 | 2685.148 | 0.000 | 50000 | 50000 | 50000 | 0 | ✅ |  |  |  |
| BACKEND | 192.168.0.41:8080 | v0.3_pessimistic-lock | 10 | 500 | AVERAGE | 256.267 req/s | 30.439 | 12.945 | 27.459 |  |  |  |  |  |  |  |  |  |  |  |  |  |
| BACKEND | 192.168.0.46:8080 | v0.3_pessimistic-lock | 10 | 500 | AVERAGE | 256.267 req/s | 33.351 | 14.933 | 25.746 |  |  |  |  |  |  |  |  |  |  |  |  |  |

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
<summary>v0.1 실험 환경</summary>

![v0.1 실험 환경](https://github.com/yeoooo/yeoooo.github.io/blob/master/assets/images/gold-rush-lab/v0-1/environment.png?raw=true)

</details>

```text
k6 / Client
     |
     v
Spring MVC Controller
     |
     v
Transactional Service  -- Micrometer --> Prometheus --> Grafana
     |
     v
JPA Repository
     |
     v
PostgreSQL
  ├── mine
  ├── app_user
  └── mining_log
```

모든 사용자가 동일한 `mine` row를 조회하고 수정하지만 별도의 동시성 제어는 적용하지 않습니다. 이 구조를 이후 버전의 잠금 전략과 성능을 비교하는 기준선으로 사용합니다.

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
