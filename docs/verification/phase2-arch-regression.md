# 멀티모듈 전환 회귀 검증 (2026-11-20)

브랜치 `phase2-arch` (13단계 커밋 `0f06bd5` 빌드). 실기기 SM-A325N + PX4 v1.18.0-rc1 SITL(SIH), USB + `tools/mavlink_udp_tcp_bridge.py`.
목적: 구조 변경(모듈 분리, UseCase 도입, Navigation 3) 후 Phase 1 시나리오와 Week 2 Mission 편집이 이전과 같이 동작하는지.

| # | 시나리오 | 거친 경로 (새 구조) | 결과 |
|---|---|---|---|
| 1 | 연결 | data:drone → `ObserveDroneStatusUseCase` → Dashboard | CONNECTED, Flight Mode `HOLD`(FlightMode enum), 배터리 100%, 3D FIX, 고도, heading, 위경도 ✅ |
| 2 | ARM → TAKEOFF → LAND (앱 버튼) | `ExecuteDroneCommandUseCase` | ACCEPTED ×3, 2.6m 호버링 → 0.1m, PX4 `Armed by external command` → `Takeoff detected` → `Landing detected` → `Disarmed by landing` ✅ |
| 3 | 통신 두절 failsafe | 앱에서 RETURN 선택(`SetLinkLossActionUseCase`) → PX4 `NAV_DLL_ACT=2` → 이륙 → `sitl_fault.sh link-cut` | 앱: CONNECTION LOST, "will enter RETURN in ~6s" → "has likely entered RETURN (~9s ago). Unconfirmed", Armed/Mode는 `Last known value`. PX4: `Failsafe activated` → `RTL` → 착지·Disarm. `link-restore` 후 앱이 실제 상태(Armed FALSE, 0.0m) 반영 ✅. 앱에서 DISABLED로 원복 → `NAV_DLL_ACT=0` ✅ |
| 4 | 명령 거부 | GPS failure 주입 → ARM | 앱: GPS SIGNAL LOST, `REJECTED` + `PX4: Arming denied: Resolve system health failures first`, Armed FALSE 유지 ✅ |
| 5 | Mission 편집 + 기체 위치 | `ObserveVehiclePositionUseCase`, Mission UseCase 6종 | GPS off 동안 기체 마커 회색 → `failure gps ok` 후 노란색(픽셀 샘플로 확인). 지도 탭 3번 → Waypoint 3개 Plan valid, #3 ▲ → 순서 변경, #1 ✕ → 삭제, 고도 150 → `Rejected: altitude must be at most 120 m` ✅ |

## 발견

- **PX4 v1.18.0-rc1에서 `SYS_FAILURE_EN`은 재부팅 후 적용된다.** 실행 중 `param set SYS_FAILURE_EN 1` → `failure gps off`는 `Timeout waiting for ack`로 실패(GPS 그대로). SITL 재시작(파라미터는 저장됨) 후에는 바로 동작. `tools/sitl_fault.sh gps-off`도 같은 이유로 실패한다(Week 4 v1.17.0에서는 재부팅 없이 됐음). 앱 문제 아님
- `tools/sitl_fault.sh`의 px4 클라이언트(`px4-failure`)가 3회 timeout — 위와 같은 원인으로 보임
- 착지 후 고도 `-0.4m`/`0.1m` 등 소수 음수 표시는 Phase 2 Week 1 주말 검증 때 기록한 그대로(이번 변경과 무관)

실험 후 `NAV_DLL_ACT 0`, `SYS_FAILURE_EN 0`, `SIM_BAT_*` 기본값 확인 (`sitl_fault.sh status`).
