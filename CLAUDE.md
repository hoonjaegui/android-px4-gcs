# android-px4-gcs — 세션 인수인계 문서

> 이 파일은 여러 맥북에서 Claude Code 세션을 이어가기 위한 **진행 상태 + 규칙** 문서다.
> Claude는 작업 단계가 끝날 때마다 이 파일을 갱신하고 커밋/푸시한다.

## 프로젝트

PX4 SITL 기반 Android GCS 4주 스터디. 계획서 원본: `GCS with PX4.md` (Notion export, 사용자 로컬 Downloads).

```
Android GCS → MAVSDK → MAVLink(UDP) → PX4 SITL → Simulated Drone
```

목적: "실제 시스템의 상태/통신(Telemetry, State, Command, Connection, Failure)을 다루는 개발이 재미있는가" 검증. 화면을 많이 만드는 게 목적이 아니다.

## 진행 규칙

- 사용자가 "Week N 시작"이라고 하면 **해당 Week 범위만** 진행한다. 다음 주차 선행 금지.
- 브랜치: default `main`, 주차별 `Week1`, `Week2`, ... (`main`에서 분기).
- 각 단계는 **성공을 확인한 뒤** 다음 단계로 넘어간다.
- Week 1은 Android 코드 작성 금지 (PX4/SITL/QGC/MAVLink 이해가 목표).
- 서드파티 Homebrew tap(PX4/px4, osx-cross/arm)은 사용하지 않는다 (사용자 미승인, SITL엔 불필요).

## 로컬 환경 재현 (새 맥북에서)

PX4 소스는 이 레포 밖 `~/PX4-Autopilot`에 둔다.

```bash
# 1. 도구
brew install cmake ninja ccache python@3.12
brew install --cask qgroundcontrol

# 2. PX4 소스 (v1.17.0)
git clone --branch v1.17.0 --recursive --shallow-submodules --depth 1 \
  https://github.com/PX4/PX4-Autopilot.git ~/PX4-Autopilot

# 3. Python 의존성 (시스템 python 대신 venv)
cd ~/PX4-Autopilot
/opt/homebrew/bin/python3.12 -m venv .venv
.venv/bin/pip install -r Tools/setup/requirements.txt

# 4. 빌드 (약 2~3분)
export PATH="$PWD/.venv/bin:$PATH"; ulimit -S -n 2048
make px4_sitl
```

시뮬레이터: Gazebo 대신 **SIH**(PX4 내장 물리 시뮬레이터) — `make px4_sitl sihsim_quadx`.

검증된 환경: Apple Silicon(arm64), macOS 15.0, RAM 32GB / QGC 5.1.5 / PX4 v1.17.0 / Python 3.12.16 / cmake 4.4.4

## 진행 상태

### Week 1 — PX4 생태계 이해 + Drone 직접 비행 (브랜치 `Week1`)

- [x] 1. 환경 구축 — QGC 설치, PX4 v1.17.0 클론, venv, `make px4_sitl` 빌드 성공 (2026-10-27)
- [x] 2. PX4 SITL 실행 — `make px4_sitl sihsim_quadx`, `pxh>`에서 `commander status` → Disarmed / Hold / failsafe no 확인 (2026-10-27)
- [x] 3. QGroundControl 연결 — QGC가 UDP 14550 자동 연결, 툴바 Ready / Hold, 지도에 기체 표시 (취리히 Irchelpark) (2026-10-27)
  - 첫 실행 시 초기 설정 창 + macOS 로컬 네트워크 권한 허용 필요. Spotlight에 안 잡히면 `open -a QGroundControl`
- [x] 4. Drone Telemetry 확인 — QGC 화면 + MAVLink Inspector (2026-10-27)
  - 화면: Ready(Disarmed) / Hold / GPS 10 sats HDOP 0.7 / Battery 100% / Heading 358° / Alt 0.0 m
  - GLOBAL_POSITION_INT lat=473977430, lon=85455930 (degE7 정수 → 47.3977430, 8.5455930), HEARTBEAT 1.0Hz
- [x] 5. ARM — QGC 툴바 Ready → Arm → 슬라이더 (2026-10-28)
  - PX4 로그: `Armed by external command` → 약 10초 후 `Disarmed by auto preflight disarming` (`COM_DISARM_PRFLT`, 이륙 안 하면 자동 Disarm)
  - MAVLink Inspector: `HEARTBEAT.base_mode` = 129 = 128(`SAFETY_ARMED`) + 1(`CUSTOM_MODE_ENABLED`). Disarm 시 1
  - 이슈: 두 번째 Disarm 직후 SIH의 `sensor_baro`/`sensor_mag` 갱신이 멈춤 (`MAG #0 failed: TIMEOUT!`, `Preflight Fail: No valid data from Baro/Compass`). accel/gps는 정상. 원인 미확인, SITL 재시작으로 복구
  - Claude가 SITL을 백그라운드로 띄울 땐 stdin을 FIFO로 연결해야 함 (stdin 없으면 `pxh>` 무한 출력으로 로그 폭증). FIFO에 명령을 써서 pxh 조작 가능
- [ ] 6. TAKEOFF
- [ ] 7. LAND
- [ ] 8. RTL

완료 조건: `QGroundControl → MAVLink → PX4` 관계 설명 가능 + GCS / Flight Controller / Telemetry / Command / MAVLink / MAVSDK / SITL 구분 가능.

### Week 2~4 — 미시작
