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
- **Phase 2** (12주, 계획서 `GCS with PX4 — Phase 2.md`, 레포 루트에 untracked로 둠): Android GCS + C++/Linux + 실제 PX4 FC 3트랙.
  - 브랜치 `phase2-week1`, `phase2-week2`, ... (`main`에서 분기, 주차 종료 시 main에 ff 머지)
  - 기능별 구현 계획을 먼저 제안하고, 한 번에 다 짜지 않는다. C++은 사용자가 완전 초보 → Kotlin과 비교해 설명하고 **사용자가 직접 작성**하게 한다
  - 테스트/실행으로 확인하지 않은 것은 완료로 기록하지 않는다. 주말 SITL/FC 검증 항목은 따로 정리
  - 문서는 `docs/`, C++ 도구는 `tools/cpp-link-monitor/` (계획서 9장)

## 로컬 환경 재현 (새 맥북에서)

PX4 소스는 이 레포 밖 `~/PX4-Autopilot`에 둔다.

```bash
# 1. 도구
brew install cmake ninja ccache python@3.12
brew install --cask qgroundcontrol

# 2. PX4 소스 (v1.18.0-rc1 — v1.17.0은 SIH 센서 멈춤 버그, 아래 Week 1 알려진 이슈 참고)
git clone --branch v1.18.0-rc1 --recursive --shallow-submodules --depth 1 \
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

검증된 환경: Apple Silicon(arm64), macOS 15.0, RAM 32GB / QGC 5.1.5 / PX4 v1.18.0-rc1 (Week 1은 v1.17.0) / Python 3.12.16 / cmake 4.4.4
Android: Android Studio JBR 21 (`JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`), AGP 9.2.1, Gradle 9.4.1, MAVSDK Java/mavsdk-server 4.1.0, Hilt 2.59.2 + KSP 2.3.4 (버전은 사용자 프로덕트 Grider_Rider 기준)

### 장애 주입 (`tools/sitl_fault.sh`)

휴대폰이 아니라 **PX4 SITL(시뮬 드론) 쪽**에 장애를 넣는다. PX4 클라이언트 명령(`build/px4_sitl_default/bin/px4-*`)을 쓰므로 SITL이 어느 터미널에서 돌든 동작. `tools/sitl_fault.sh help`로 전체 목록.
- `gps-off/on`, `link-cut/restore`(브리지 종료/재시작), `dll <0|1|2|3>`(NAV_DLL_ACT), `telemetry-off/on`, `freeze/resume`(PX4 SIGSTOP/CONT), `battery-low/reset`, `takeoff/land/rtl`, `status`, `reset`(전부 원복)
- 폰 쪽 통신 두절은 USB를 뽑으면 됨 (다시 꽂은 뒤 `adb reverse tcp:5760 tcp:5760`)

### Android 앱을 SITL에 붙이기

앱 안에서 mavsdk_server가 돌고, PX4의 Onboard 링크(UDP 14540)를 받는다. QGC(14550)와 동시 연결 가능.

**실기기 (권장)** — 회사 Wi-Fi는 맥↔폰 통신이 막혀 있어(ping 100% 손실) USB로 연결. `adb reverse`는 TCP만 되므로 브리지 사용:
```bash
adb reverse tcp:5760 tcp:5760
python3 tools/mavlink_udp_tcp_bridge.py          # PX4 UDP 14540 <-> TCP 5760
./gradlew installDebug -Pgcs.connectionUrl=tcpout://127.0.0.1:5760
```

**에뮬레이터** — `adb emu redir add udp:14540:14540` 후 기본 빌드(`udpin://0.0.0.0:14540`). 단 Pixel_10(API 37.2, 16KB) 에뮬레이터는 VM 전체가 주기적으로 멈칫해서(앱을 꺼도 system_server가 프레임 스킵) heartbeat timeout으로 연결이 끊겼다 붙었다 함(90초에 4회, 실기기는 0회). 브리지와 동시 사용 불가(같은 호스트 포트 14540).

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
  - Claude가 SITL을 백그라운드로 띄울 땐 stdin을 FIFO로 연결해야 함 (stdin 없으면 `pxh>` 무한 출력으로 로그 폭증). FIFO에 명령을 써서 pxh 조작 가능
- [x] 6. TAKEOFF — QGC Takeoff 버튼, 10m (2026-10-28)
  - 로그: `Armed by external command` → `Takeoff detected` → 모드 Hold(호버링)
  - `vehicle_local_position.z` ≈ -9.87 (NED 좌표계라 위쪽이 음수)
  - 시뮬 배터리는 ARM 후 `SIM_BAT_DRAIN`(60초)에 따라 줄다가 `SIM_BAT_MIN_PCT`(50%)에서 멈춤. 정상
- [x] 7. LAND — QGC Land 버튼 (2026-10-28)
  - 로그: `Landing detected` → `Disarmed by landing` (착지 감지 시 자동 Disarm), z ≈ 0.01

**알려진 이슈 (macOS + SIH, PX4 v1.17.0) — Week 2에서 원인 확인, v1.18.0-rc1로 해결**: ARM/Disarm 시점에 `wq:hp_default`의 시뮬 센서 모듈(`sensor_baro_sim`, `sensor_mag_sim`, `sensor_gps_sim`, `battery_simulator`) 일부 또는 전부가 실행을 멈춤. 같은 큐의 `system_power_simulator`, `simulator_sih`는 정상
  - 증상: QGC Not Ready, `Preflight Fail: No valid data from Baro 0 / Compass 0`, `Battery unhealthy`, `MAG #0 failed: TIMEOUT!`
  - 확인: pxh에서 `perf`를 두 번 실행해 `*_sim: cycle` 카운트가 늘지 않으면 멈춘 것
  - 복구: `battery_simulator`만 멈췄으면 `battery_simulator stop` → `start`. baro가 멈췄으면 재시작해도 새 인스턴스(#1)가 생기고 Baro 0은 그대로라 **SITL 재시작 필요**
  - ~~근본 원인 미확인~~ → **원인 (2026-11-03)**: POSIX `drv_hrt.cpp`의 `hrt_call_invoke()`가 콜백 중 lock을 풀어 다른 스레드가 HRT 콜아웃 큐(단일 연결 리스트)를 동시에 수정 → 리스트가 찢어져 `ScheduleOnInterval` 모듈들이 짝지어 영구 정지. ARM/Disarm이 아니라 시뮬 시간 특정 시점에 발생(battery+mag ≈17.7s, gps+baro ≈36.6s)
  - 업스트림 수정: PX4 커밋 `9b62a17` "fix(WorkQueue): fix lifetime race between Run() and destruction" (recursive mutex로 콜백 중 lock 유지). v1.17.x엔 없고 v1.18 계열에만 있음 → **v1.18.0-rc1로 업그레이드**, ARM/Disarm 4회 반복에도 4개 센서 정상. v1.17.0 소스는 `~/PX4-Autopilot-v1.17.0`에 백업
  - 재현 기록 (모두 2026-10-28): ①2번째 Disarm에 baro+mag ②ARM에 baro+mag+gps+battery ③LAND Disarm에 battery ④RTL Disarm에 gps. 거의 매번 발생하므로 **비행 전 SITL 재시작을 기본으로**
- [x] 8. RTL — Takeoff → 지도 클릭 Go to location → Return (2026-10-28)
  - 로그: `RTL: start return at 521 m (31 m above destination)` (AMSL, 홈 ≈ 489m) → `RTL: land at destination` → `Landing detected` → `Disarmed by landing`
  - 최종 위치 홈 기준 x=0.26, y=1.36m (약 1.4m 오차)

완료 조건: `QGroundControl → MAVLink → PX4` 관계 설명 가능 + GCS / Flight Controller / Telemetry / Command / MAVLink / MAVSDK / SITL 구분 가능.

### Week 2 — Android GCS Telemetry (브랜치 `Week2`, main에서 분기)

- [x] 1. Android 프로젝트 생성 — 사용자가 Android Studio로 생성(Compose, AGP 9.2.1). `core-ktx 1.19.1`이 API 37 요구 → compileSdk 36.1→37.1 (2026-11-03)
- [x] 2~3. MAVSDK 연결 + Vehicle Discovery — `MavsdkServer.run(url)`은 첫 HEARTBEAT까지 블로킹(=Discovery), 반환된 gRPC 포트로 `io.mavsdk.System` 생성. `core.connectionState`로 Connected/Disconnected. SITL 종료 → heartbeat timeout(3s) → DISCONNECTED → 재시작 시 자동 재연결 확인 (2026-11-03)
  - `mavsdk-server` AAR이 `libc++_shared.so`를 안 담고 있어 크래시 → NDK r27.3 것을 `app/src/main/jniLibs`에 번들 (16KB 정렬 확인)
- [x] 4. Telemetry — Battery → GPS → Altitude → Heading → Flight Mode(+Armed) 순서로 하나씩 연결 (2026-11-03)
  - 구조: `MavsdkDroneDataSource`(MAVSDK는 여기만) → `DefaultDroneRepository`(스트림별 `DroneState` 갱신 함수를 merge+scan) → `StateFlow<DroneState>` → `DashboardViewModel` → Compose
  - Battery `remainingPercent`는 MAVSDK 4.x에서 0~100. Altitude는 `relativeAltitudeM`(홈 기준), GPS는 fix ≥ 3D면 available
- [x] 5. Compose Dashboard + 완료 조건 — QGC로 Takeoff → Go to → RTL → 착륙하는 동안 폰 앱에서 Flight Mode(HOLD→RETURN→HOLD), Armed, Altitude(0→2.7→29.8m→0), Heading(4°→93°→276°→3°), 위경도가 동시에 변함, 연결 끊김 없음 (2026-11-03)
  - 우연히 겪은 Failure: v1.17.0 센서 멈춤 중 앱이 GPS NO FIX / Flight Mode LAND(blind land failsafe)를 그대로 표시 → Week 4 소재
- [x] 6. 수동 DI → Hilt 전환 — `GcsApplication`(`@HiltAndroidApp`), `di/DroneModule.kt`(`@ApplicationScope` CoroutineScope, `@MavsdkConnectionUrl`, `DroneRepository` `@Binds`), DataSource/Repository `@Singleton @Inject`, `@HiltViewModel` + `hiltViewModel()`. 실기기 + SITL에서 연결/텔레메트리, pxh `commander takeoff`로 Armed TRUE·Altitude 2.6m 갱신 확인 (2026-11-05)

### Week 3 — Android에서 Drone Command (브랜치 `Week3`, Week2를 main에 ff 머지 후 분기)

- [x] 1. ARM — `DroneCommand`/`CommandResult`/`CommandState`(core/model), `MavsdkDroneDataSource.arm()`(`action.arm().await()`), `DefaultDroneRepository.runCommand`가 `ActionException(code, description)` → `CommandResult.Failure`로 변환(MAVSDK 타입 차단), `DashboardViewModel.onCommand`가 명령별 Idle→Running→Success/Failed 관리(Running 중 중복 전송 차단) (2026-11-05)
  - 실기기: ARM 탭 → `arm accepted` / PX4 `Armed by external command` / 화면 ACCEPTED + Armed TRUE → 약 10초 후 `Disarmed by auto preflight disarming`으로 Armed FALSE, 명령 상태는 ACCEPTED 그대로
  - 핵심: Command 결과(ACK 수락)와 기체 상태(Telemetry)는 별개. 상태 판단은 항상 Telemetry로
- [x] 2~4. TAKEOFF / LAND / RTL — `action.takeoff()/land()/returnToLaunch().await()`, 계층마다 메서드 하나 + `DroneCommand` enum 항목 추가 (2026-11-05)
  - Disarm 상태에서 TAKEOFF → **ACCEPTED**, Flight Mode TAKEOFF로 바뀌지만 Armed FALSE / 0m (모드만 전환). 이어서 ARM → 즉시 `Takeoff detected` (미리 걸린 모드대로 이륙)
  - RTL(2.5m 호버링 중) → Flight Mode RETURN → `RTL: start return at 492 m (3 m above destination)` → `land at destination` → `Disarmed by landing`
  - 완료 조건: ARM → TAKEOFF(HOLD, 2.4m) → LAND(Flight Mode LAND, 1.6m) → `Disarmed by landing`(HOLD, Armed FALSE, 0.2m). 네 명령 모두 앱 버튼으로, 상태 변화는 앱 Telemetry로 확인

### Week 4 — GCS Failure Handling (브랜치 `Week4`, Week3를 main에 ff 머지 후 분기)

- [x] 공통 설계 (2026-11-05)
  - `DroneState.connection: ConnectionStatus`(SEARCHING / CONNECTED / LOST — 처음 못 찾은 것과 끊긴 것을 구분), `gpsAvailable: Boolean?`(null=모름), `receivedAt: Map<TelemetryField, Long>`(필드별 수신 시각, `SystemClock.elapsedRealtime` monotonic)
  - `DroneWarningPolicy.evaluate(state, nowMs)` 순수 함수 → `DroneWarningState`. 기준: stale 3초(HEARTBEAT timeout과 동일), low battery < 20%. JVM 단위 테스트 7개 (`DroneWarningPolicyTest`)
  - ViewModel이 500ms 시계 Flow를 `combine` — "안 오는 것"은 이벤트가 없어서 시계 없이는 감지 불가
  - UI: 경고 배너, stale 값은 회색 + `Last update Ns ago`, LOST 시 HEARTBEAT 값(Armed/Mode)은 `Last known value`, 미연결 시 명령 버튼 비활성
  - `CommandFailureReason`(NOT_CONNECTED / REJECTED / BUSY / TIMEOUT / UNKNOWN)으로 MAVSDK `ActionResult` 분류, 사유별 운용자 안내 문구
- [x] 1. Connection Loss — `kill -STOP <px4 pid>` → 약 2초 후 CONNECTION LOST 배너, 값 전부 회색 + 경과 시간, 버튼 비활성 → `kill -CONT` → 자동 복구
- [x] 2. Telemetry Stale — pxh `mavlink stream -u 14580 -s GLOBAL_POSITION_INT -r 0` (앱 쪽 Onboard 링크만) → **CONNECTED인데** TELEMETRY LOST, Altitude/Heading/위경도만 `Last update 6s ago`. 복구: `-r 50`
- [x] 3. GPS Lost — `param set SYS_FAILURE_EN 1` → `failure gps off` → GPS SIGNAL LOST / NO FIX. 위경도는 stale 아님(EKF 추정치가 계속 옴) → "No GPS fix — estimated / last known position" 표시. 복구: `failure gps ok`
- [x] 4. Low Battery — `param set SIM_BAT_MIN_PCT 10`, `SIM_BAT_DRAIN 20` → 이륙 → 10%에서 LOW BATTERY 배너 + 빨간 값. PX4도 `Low battery` 경고. 테스트 후 50 / 60 / `SYS_FAILURE_EN 0`으로 원복
- [x] 5. Command Failure
  - REJECTED: GPS failure 상태에서 ARM → PX4 `Arming denied: Resolve system health failures first` → 앱 REJECTED / COMMAND_DENIED, Armed FALSE 유지
  - TIMEOUT: PX4 SIGSTOP 직후 ARM → 앱 NO RESPONSE(TIMEOUT) → SIGCONT 하자 PX4 `Armed by external command`, 앱 Armed TRUE. **Timeout ≠ 미실행** (소켓에 쌓인 명령이 뒤늦게 실행됨)
- [x] 6. 위치 신뢰성 + 시각 표시 개선 (2026-11-05)
  - GPS fix 없음 또는 `health.isGlobalPositionOk == false`면 들어오는 위경도를 **버리고** 마지막 GPS 위치 유지(`DroneState.withPosition`). 고도는 기압계 기반이라 계속 갱신. `TelemetryField`를 POSITION(위경도)/ALTITUDE로 분리. EKF만 불신하면 POSITION UNRELIABLE 배너
  - 수신 시각을 `Timestamp(elapsedMs, epochMs)` 두 시계로 저장 → `Last update 17:36:45 (10s ago)`. 연결 끊기면 Armed/Mode에 `Last known value — link lost 17:38:20 (22s ago)`
  - 실기기: `failure gps off` 후 10초 지나도 위경도 17:36:45 값 고정, 고도/heading은 계속 갱신. 단위 테스트 10개
- [x] 7. 통신 두절 failsafe 재현 — PX4는 그대로 두고 **브리지만 kill** (현장과 같은 상황: 기체는 계속 비행, 링크만 끊김). PX4가 MAVSDK를 GCS로 인식(`Connection to ground station lost` / `GCS connection regained`). 브리지 재시작 시 앱 자동 재연결
  - `NAV_DLL_ACT=0`(**PX4 기본값**, `COM_DL_LOSS_T=10`): 18초 지나도 failsafe 없음, HOLD로 계속 호버링
  - `NAV_DLL_ACT=2`(RTL): `Failsafe activated: entering Hold for 5 seconds` → `RTL: start return` → 착지·Disarm. 끊긴 동안 앱은 HOLD/Armed TRUE(last known)를 보여줬지만 재연결하니 실제는 착륙·Disarm 완료 → 끊긴 동안 기체 상태는 GCS가 알 수 없음
  - 테스트 후 `NAV_DLL_ACT 0`, `SYS_FAILURE_EN 0` 원복
- [x] 8. 거부 사유 + 통신 두절 예상 동작 (2026-11-05)
  - `telemetry.statusText`(STATUSTEXT) 수신 → 최근 5개 `Vehicle messages` 표시. 명령 실패 시 보낸 시각 이후의 WARNING 이상 메시지를 최대 500ms 기다려 `CommandResult.Failure.vehicleMessage`로 첨부 (ACK와 STATUSTEXT는 순서 보장 없음)
  - 연결될 때마다 `param.getParamInt("NAV_DLL_ACT" / "COM_DL_LOSS_T")` → `LinkLossPolicy`. LOST 동안 `LinkLossOutlook`(Unknown / NoAction / Pending / Triggered) 표시
  - 실기기: GPS off + ARM → `PX4: Arming denied: Resolve system health failures first`. `NAV_DLL_ACT=2` + 이륙 + link-cut → "will enter RETURN in ~7s" → "has likely entered RETURN (~6s ago). Unconfirmed until reconnected" → 재연결 시 실제 RETURN / 착지 중 확인
  - 한계: 파라미터는 연결 시점에만 읽음 (연결 중 변경은 재연결해야 반영). 예상 시간은 GCS LOST 판정(3초)과 PX4 판정의 차이만큼 오차
- [x] 9. 앱에서 통신 두절 failsafe 설정 (2026-11-05)
  - `Link loss failsafe` 섹션: DISABLED / HOLD / RETURN / LAND (TERMINATE·DISARM은 공중 사고 위험이라 제외). `param.setParamInt("NAV_DLL_ACT")` → **성공/실패와 무관하게 다시 읽어** 기체 값으로 판정·표시 (쓰기 TIMEOUT이어도 실제 반영됐을 수 있음). 표시는 "쓰려던 값"이 아니라 "기체에서 읽은 값"
  - 설정은 기체(FC)에 저장되고 GCS는 쓰기만 한다 — 끊긴 순간 GCS는 아무것도 못 하므로. QGC는 Vehicle Setup → Safety → Data Link Loss에서 같은 파라미터를 씀
  - QGC가 켜져 있으면 PX4는 GCS가 살아 있다고 보고 앱 링크를 끊어도 failsafe 안 함 → 실험 시 QGC 종료
  - 실기기: 앱 RETURN → PX4 `NAV_DLL_ACT : 2`, 앱 DISABLED → `0` 확인
  - `sitl_fault.sh`: px4 클라이언트가 간헐적으로 멈춤 → 5초 timeout + 3회 재시도
  - 버그 수정: 땅에서 Disarm 상태로 끊겨도 "will enter RETURN" 카운트다운을 보여줬음. PX4 failsafe는 Armed일 때만 동작 → 끊기기 직전 Armed=FALSE면 `LinkLossOutlook.Disarmed`("no failsafe expected"). 실기기 확인
- 명령 "수락"과 "목표 달성"(예: 착지 완료)의 연결은 하지 않기로 함 — PX4가 Flight Mode(HEARTBEAT)와 landed state로 이미 제공하므로 앱이 별도 상태를 만들 필요 없음
- [x] 완료 조건 — Connection Lost / Telemetry Stale / Low Battery / GPS Lost / Command Failed 모두 앱에서 표현, 실기기 확인. Week4를 main에 ff 머지 (2026-11-09)

**Week 4 핵심 질문 — 답 (실험 근거)**
1. Telemetry가 끊겼는데 마지막 값을 계속 보여줘도 되는가? → **보여주되 "현재 값"처럼 보이면 안 된다.** 값을 지우면 운용자가 마지막 상태(고도·위치)를 잃으므로 유지하고, 회색 + 수신 시각 + 경과 시간(`Last update 17:36:45 (10s ago)`)으로 과거 값임을 표시. 판단 기준은 연결 여부가 아니라 **필드별 수신 시각** — 연결(HEARTBEAT)은 살아 있는데 위치 스트림만 끊길 수 있음(Scenario 2: CONNECTED인데 Altitude/위경도만 stale)
2. 마지막 GPS는 현재 위치인가 과거 위치인가? → **과거 위치다.** 게다가 "계속 들어오는 위경도"도 현재 위치라는 보장이 없다: GPS가 꺼져도 EKF 추정치가 계속 와서 stale 판정에 안 걸림(Scenario 3). 그래서 fix 없음/`isGlobalPositionOk == false`면 들어오는 값을 버리고 마지막 GPS 위치를 시각과 함께 고정 표시
3. Connection Lost와 Drone Lost는 같은 의미인가? → **다르다.** Connection Lost는 "GCS가 기체 상태를 모른다"일 뿐 기체는 계속 비행한다. 브리지만 끊었을 때 앱은 HOLD/Armed(last known)를 보여줬지만 실제 기체는 `NAV_DLL_ACT=2`에 따라 RTL → 착륙·Disarm까지 끝냄. 끊긴 동안 GCS가 할 수 있는 건 기체에 미리 설정된 failsafe로 **예상**하는 것뿐(`LinkLossOutlook`, "Unconfirmed until reconnected"), 확인은 재연결 후
4. Command가 Timeout이면 Drone이 명령을 수행하지 않은 것인가? → **모른다(응답만 잃었을 수 있다).** PX4 SIGSTOP 중 ARM → 앱 TIMEOUT → SIGCONT 하자 쌓여 있던 명령이 실행돼 Armed TRUE. 그래서 Timeout을 "실패"로 단정하지 않고 Telemetry로 실제 상태를 확인하며, 파라미터 쓰기도 결과와 무관하게 다시 읽어 기체 값으로 판정

## Phase 2 진행 상태

### Week 1 — Baseline & C++ 기초 ① (브랜치 `phase2-week1`)

- [x] Android 기준선 분석 (2026-11-09) — `docs/architecture.md`(구조·데이터 흐름·명령·연결 끊김·테스트 현황·Phase 2 진단), `docs/environment.md`(버전)
  - 기존 단위 테스트 18개 통과(`./gradlew testDebugUnitTest`). 테스트는 `core.model` 순수 함수만 다룬다. Repository/ViewModel은 DataSource가 구체 클래스 + MAVSDK 타입 반환 + `SystemClock`/`Log` 직접 호출이라 JVM 테스트 불가 → W5/W6에서 필요할 때 interface/Clock 추출 (지금은 안 고침)
  - 앱의 mavsdk_server는 logcat에 `MAVSDK version: 0.0.0`으로 찍힘 (AAR 빌드에 버전 태그 없음). 버전은 Gradle 좌표 4.1.0으로 관리
- [ ] C++ 기초 ① — 진행 상세·결과는 `tools/cpp-link-monitor/learn/week1/NOTES.md` (단계 1~5 표)
  - 에디터: Cursor + clangd 확장 (learn/에서는 Cursor Tab 끄기 권장). `compile_flags.txt`는 아직 안 만듦(제안만 함)
  - 1단계 컴파일/링크: 실습 완료 (2026-11-09) — 빈 파일→링크 에러 `_main`, 오타→컴파일 에러, 정상→`.ii` 83,248줄·`nm` T/U·`otool -L`. **다음 세션: 확인 질문 2개 답변 받고 → 2단계(header/source 분리 + 일부러 링크 에러)**
  - 사용자가 코드를 직접 작성, Claude는 설명·리뷰만. 사용자가 설명 줄까지 터미널에 붙여 넣는 일이 있었음 → 명령은 주석 없는 코드 블록으로만 준다
- [ ] 주말 — SITL 연결, Takeoff/Land, Connection Loss 각 3회, C++ 프로그램 빌드·실행
