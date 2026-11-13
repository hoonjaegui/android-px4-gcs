# Phase 2 Week 1 주말 검증 (2026-11-13)

환경: PX4 v1.18.0-rc1 SIH(`make px4_sitl sihsim_quadx`), 실기기 USB + `adb reverse tcp:5760` + `tools/mavlink_udp_tcp_bridge.py`, 앱은 `phase2-week1` 브랜치 `installDebug -Pgcs.connectionUrl=tcpout://127.0.0.1:5760`. QGC 미실행.

확인 방법: PX4 로그(SITL stdout) + 앱 화면(`adb shell uiautomator dump`로 텍스트 추출). 버튼 조작은 사용자가 폰에서 직접.

## 1. SITL 연결 — 성공

- 브리지 `app connected from 127.0.0.1`, PX4 `Ready for takeoff!`
- 앱: CONNECTED / HOLD / Armed FALSE / Battery 100% / 3D FIX / 0.0m / 47.3977427, 8.5455940

## 2. Takeoff / Land — 3/3 성공

절차: 앱 ARM → TAKEOFF → 2.5m 호버링 → LAND

| 회차 | PX4 로그 | 앱 최종 상태 |
| --- | --- | --- |
| 1 | `Armed by external command` → `Using default takeoff altitude: 2.5 m` → `Takeoff detected` → `Landing at current position` → `Landing detected` → `Disarmed by landing` | HOLD / Armed FALSE / 0.0m / 명령 3개 ACCEPTED / CONNECTED |
| 2 | 동일, WARN/ERROR 없음 | HOLD / Armed FALSE / **-0.0m** / CONNECTED |
| 3 | 동일 | HOLD / Armed FALSE / 0.0m / CONNECTED |

- 이전 회차의 `ACCEPTED` 표시가 다음 명령을 막지 않음
- 발견: 착지 후 고도가 `-0.0m`으로 표시될 때가 있음 (작은 음수 상대고도를 소수 1자리로 반올림하면서 부호가 남음). 표시 문제, 평일 작업으로 넘김

## 3. Connection Loss — 3/3 성공

설정: 앱 `Link loss failsafe` = RETURN (PX4 `NAV_DLL_ACT = 2`, `COM_DL_LOSS_T = 10`)
절차: 앱 ARM → TAKEOFF → 호버링 중 `tools/sitl_fault.sh link-cut` → 약 30초 대기 → `link-restore`

| 회차 | 끊은 시각 | 끊긴 뒤 약 5초 앱 | PX4 (끊긴 동안) | 재연결 | 재연결 후 앱 |
| --- | --- | --- | --- | --- | --- |
| 1 | 11:27:53 | CONNECTION LOST, `Vehicle will enter RETURN in ~6s`, HOLD/Armed TRUE `Last known value`, 값 전부 `Last update ... ago` | `Connection to ground station lost` → `Failsafe activated: entering Hold for 5 seconds` → `RTL: start return at 492 m` → `RTL: land at destination` → `Landing detected` → `Disarmed by landing` | 11:28:31 | 자동 CONNECTED, HOLD / Armed FALSE / 0m |
| 2 | 11:32:02 | 동일 | 동일 | 11:32:35 | 동일 |
| 3 | 11:33:56 | 동일 | 동일 | 11:34:28 | 동일 |

- 끊긴 동안 앱은 Armed TRUE(last known)였지만 실제로는 RTL·착지·Disarm 완료 → 재연결 후에만 확인 가능 (Phase 1 Week 4 결론 재확인)
- 시뮬 배터리: 비행 중 64~69%까지 감소, Disarm 후 100%로 복귀

## 4. C++ 프로그램 빌드·실행 — 성공

`tools/cpp-link-monitor/learn/week1/05-warning-policy/` 바이너리 삭제 후 `-Wall -Wextra -Werror`로 다시 빌드

- `main.cpp + warning_policy.cpp` → `warning`: initial/lost/noGps 출력이 예측과 일치
- `warning_policy_test.cpp + warning_policy.cpp` → `warning_test`: `all tests passed` (테스트 7개), exit 0

## 정리 / 원복

- 시작 시 이전 세션의 파라미터가 남아 있었음: `SYS_FAILURE_EN = 1`, `NAV_DLL_ACT = 1` (PX4 파라미터는 rootfs에 저장돼 재시작해도 유지). 검증 전 `SYS_FAILURE_EN 0`으로, 검증 후 `NAV_DLL_ACT 0`으로 원복
- → 실험 뒤엔 `tools/sitl_fault.sh reset` 또는 `status`로 확인하는 습관 필요
