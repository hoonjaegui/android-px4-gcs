# 개발 환경 및 버전 (Phase 2 Week 1 기준선, 2026-11-09)

## 호스트

| 항목 | 값 |
| --- | --- |
| 머신 | MacBook, Apple Silicon (arm64), RAM 32GB |
| OS | macOS 15.0 (24A335) |
| C/C++ 컴파일러 | Apple clang 16.0.0 (clang-1600.0.26.6), Xcode Command Line Tools |
| 빌드 도구 | cmake 4.4.4, ninja (Homebrew), make |
| 디버거 | lldb |
| Python | 3.12 (PX4 venv) |

## PX4 / 시뮬레이터

| 항목 | 값 |
| --- | --- |
| PX4 | v1.18.0-rc1 (`fca3df8`, 2026-09-10), 소스 `~/PX4-Autopilot` |
| 빌드 타깃 | `make px4_sitl` → `build/px4_sitl_default/bin/px4` |
| 시뮬레이터 | SIH (PX4 내장), `make px4_sitl sihsim_quadx` |
| v1.17.0을 쓰지 않는 이유 | POSIX HRT race로 SIH 센서 모듈이 멈춤 (CLAUDE.md Week 1 알려진 이슈) |
| QGroundControl | 5.1.5 |

PX4 MAVLink 포트:

| 포트 | 링크 | 사용처 |
| --- | --- | --- |
| UDP 14550 | GCS | QGroundControl |
| UDP 14540 | Onboard (API) | Android 앱 (mavsdk_server) |

## Android

| 항목 | 값 |
| --- | --- |
| JDK | Android Studio JBR 21 |
| AGP / Gradle | 9.2.1 / 9.4.1 |
| Kotlin / KSP | 2.2.10 / 2.3.4 |
| compileSdk / targetSdk / minSdk | 37.1 / 36 / 24 |
| Compose BOM | 2026.02.01 |
| Hilt | 2.59.2 |
| Coroutines (+rx2 브리지) | 1.10.2 |
| ABI | arm64-v8a, x86_64만 패키징 |
| 실기기 | USB 연결 (`adb reverse` + UDP↔TCP 브리지) |

### MAVSDK

| 항목 | 값 |
| --- | --- |
| `io.mavsdk:mavsdk` (Java API) | 4.1.0 — RxJava 2.2.21, gRPC 1.61.1 (okhttp, protobuf-lite) |
| `io.mavsdk:mavsdk-server` (AAR) | 4.1.0 — `libmavsdk_server.so` (C++ mavsdk_server, ABI당 약 37MB) |
| 런타임 보고 버전 | logcat `Mavsdk: MAVSDK version: 0.0.0` — AAR 빌드에 버전 태그가 들어가지 않음. 실제 버전은 Gradle 좌표(4.1.0)로 관리 |
| `libc++_shared.so` | AAR에 없음 → NDK r27.3 것을 `app/src/main/jniLibs`에 직접 넣음 (16KB 정렬) |

실행 구조: [architecture.md](architecture.md#1-프로세스-구조) 참고.

## 접속 URL

| 환경 | 빌드 | 호스트 쪽 준비 |
| --- | --- | --- |
| 실기기 (권장) | `./gradlew installDebug -Pgcs.connectionUrl=tcpout://127.0.0.1:5760` | `adb reverse tcp:5760 tcp:5760` + `python3 tools/mavlink_udp_tcp_bridge.py` |
| 에뮬레이터 | 기본값 `udpin://0.0.0.0:14540` | `adb emu redir add udp:14540:14540` |

실기기를 쓰는 이유: 회사 Wi-Fi에서 맥과 폰 사이 통신이 막혀 있다. `adb reverse`는 TCP만 지원하므로 브리지로 UDP를 TCP로 바꾼다. 에뮬레이터(Pixel_10, API 37.2)는 VM이 주기적으로 멈칫해서 heartbeat timeout이 난다.
