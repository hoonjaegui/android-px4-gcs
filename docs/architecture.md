# Android GCS 아키텍처 (Phase 1 기준선)

> Phase 2 Week 1 진단 결과 (2026-11-09, `main` = `a1953a8`). Phase 2 기능을 붙이기 전 현재 구조를 기록한다.
> 버전 정보는 [environment.md](environment.md).

## 1. 프로세스 구조

```
PX4 SITL (SIH)
  │  MAVLink v2 / UDP 14540 (Onboard 링크)
  │  [실기기: tools/mavlink_udp_tcp_bridge.py → TCP 5760 → adb reverse]
  ▼
┌──────────────── Android 앱 프로세스 ─────────────────┐
│ mavsdk_server (C++ 네이티브, libmavsdk_server.so)     │
│   MAVLink 송수신, heartbeat timeout, 명령 재전송/ACK   │
│        │ gRPC (127.0.0.1, 임의 포트)                  │
│        ▼                                              │
│ io.mavsdk.System (Java, RxJava Flowable/Completable)  │
│        │ kotlinx-coroutines-rx2 (asFlow / await)      │
│        ▼                                              │
│ MavsdkDroneDataSource → DefaultDroneRepository        │
│        → DashboardViewModel → DashboardScreen         │
└───────────────────────────────────────────────────────┘
```

- `MavsdkServer.run(url)`은 **첫 HEARTBEAT를 받을 때까지 블로킹**된다. 이게 Vehicle Discovery다. 반환된 gRPC 포트로 `System`을 만든다.
- MAVLink 프로토콜 처리(heartbeat, 명령 재전송, ACK 대기, timeout)는 전부 C++ mavsdk_server가 맡는다. 앱(Kotlin)은 gRPC 결과만 받는다.

## 2. 패키지와 책임

| 패키지 | 클래스 | 책임 | MAVSDK 의존 |
| --- | --- | --- | --- |
| `core.model` | `DroneState`, `Timestamp`, `ConnectionStatus`, `TelemetryField` | 도메인 상태. "마지막으로 받은 값 + 수신 시각" | 없음 |
| | `DroneWarningPolicy`, `DroneWarningState` | 상태 + 현재 시각으로 경고 판정 (순수 함수) | 없음 |
| | `DroneCommand`, `CommandResult`, `CommandFailureReason`, `CommandState` | 명령, 결과, 실패 분류 | 없음 |
| | `VehicleMessage`, `LinkLossPolicy`, `LinkLossAction`, `LinkLossOutlook` | STATUSTEXT, 통신 두절 failsafe 설정과 예상 동작 | 없음 |
| `domain.drone` | `DroneRepository` (interface) | ViewModel이 보는 계약 | 없음 |
| `data.drone` | `MavsdkDroneDataSource` | MAVSDK를 직접 다루는 **유일한** 클래스. Rx → Flow, 명령 → suspend | **있음** (반환 타입도 MAVSDK) |
| | `DefaultDroneRepository` | 스트림을 `DroneState`로 합침, MAVSDK 예외 → `CommandResult` | 있음 (enum/예외 변환) |
| `di` | `AppModule`, `DroneModule` | `@ApplicationScope` 스코프, 접속 URL, Repository 바인딩 | 없음 |
| `feature.dashboard` | `DashboardViewModel` | 500ms 시계, 경고 평가, 명령별 진행 상태, 중복 전송 차단 | 없음 |
| | `DashboardScreen` | Compose UI (배너, stale 회색 표시, 버튼, failsafe 설정) | 없음 |

MAVSDK 타입은 `data` 계층 밖으로 나가지 않는다. `core.model`에는 Android/MAVSDK import가 없어서 JVM 단위 테스트가 가능하다.

## 3. Telemetry 흐름

```
MAVSDK Flowable ──asFlow──▶ DataSource Flow<T>          (system 발견 전에는 emptyFlow, 에러는 catch 후 로그)
        ▼
Repository: 스트림마다 Update = (DroneState) -> DroneState 로 변환
        ▼
merge(11개 Update 스트림) ─▶ scan(DroneState()) ─▶ stateIn(Eagerly)  = StateFlow<DroneState>
        ▼
ViewModel: combine(droneState, commandStates, linkLossSetting, clock 500ms)
           └─ DroneWarningPolicy.evaluate(drone, now)
        ▼
StateFlow<DashboardUiState> (WhileSubscribed 5s) ─▶ collectAsStateWithLifecycle ─▶ Compose
```

| 스트림 | Update 방식 | 비고 |
| --- | --- | --- |
| connectionState | `withConnection` | SEARCHING → CONNECTED → LOST(+끊긴 시각) |
| battery, gpsInfo, heading | `toTelemetryUpdate` | 값 + `receivedAt[field]` 기록 → stale 판정 대상 |
| position | `DroneState.withPosition` | 고도는 항상 갱신. 위경도는 GPS fix와 EKF가 모두 OK일 때만 갱신 |
| health.isGlobalPositionOk, armed, flightMode | `toStateUpdate` | 시각 기록 없음. 신선도는 HEARTBEAT(connection)가 대표 |
| statusText | `withMessage` | 최신 5개 |
| 연결될 때마다 파라미터 읽기 | `NAV_DLL_ACT`, `COM_DL_LOSS_T` → `linkLossPolicy` | |
| `localUpdates` | Repository가 직접 만든 변경 | 파라미터를 쓴 뒤 다시 읽은 값 |

설계 포인트:
- **"안 오는 것"은 이벤트가 없다.** 그래서 stale 판정은 Repository가 아니라 ViewModel의 시계 tick이 한다.
- 시각은 `Timestamp(elapsedMs, epochMs)` 두 개로 저장한다. 경과 시간 계산에는 monotonic 시계, 화면 표시에는 벽시계를 쓴다.

## 4. Command 흐름

```
버튼 → ViewModel.onCommand(cmd)
         ├─ 같은 명령이 Running이면 무시 (명령별 중복 차단)
         ├─ commandStates[cmd] = Running
         ▼
       Repository.arm()/takeoff()/land()/returnToLaunch()
         └─ runCommand: DataSource 호출 (action.xxx().await())
              ├─ 성공 → Success   (= COMMAND_ACK ACCEPTED. 상태 변화가 아님)
              ├─ ActionException → CommandFailureReason (NOT_CONNECTED/REJECTED/BUSY/TIMEOUT/UNKNOWN)
              │     └─ 보낸 시각 이후 WARNING 이상 STATUSTEXT를 최대 500ms 기다려 vehicleMessage로 첨부
              └─ 아직 Discovery 전 → NOT_CONNECTED
         ▼
       commandStates[cmd] = Success | Failed(reason, detail, vehicleMessage)
```

- 명령 결과(ACK)와 기체 상태(Telemetry)는 **따로 표시**한다. 상태 판단은 Telemetry로 한다.
- TIMEOUT은 "미실행"이 아니다. 응답만 못 받았을 수 있다(Phase 1 Week 4에서 SIGSTOP 실험으로 확인).
- 파라미터 쓰기(`setLinkLossAction`)는 성공/실패와 무관하게 **다시 읽은 값**으로 결과를 판정한다.

## 5. Connection Loss 처리

| 단계 | 담당 | 동작 |
| --- | --- | --- |
| 감지 | mavsdk_server | HEARTBEAT 3초 미수신 → `connectionState.isConnected=false` |
| 상태 | `withConnection` | CONNECTED였으면 LOST + `connectionLostAt` 기록, 처음부터 못 찾았으면 SEARCHING 유지 |
| 경고 | `DroneWarningPolicy` | `connectionLost`. 이때 stale 필드는 telemetry loss로 따로 보고하지 않음 |
| 예상 | `LinkLossOutlook` | Armed 여부와 failsafe 설정으로 Disarmed / NoAction / Pending / Triggered / Unknown |
| UI | Screen | 배너, 모든 값 회색 + 시각, Armed/Mode는 `Last known value`, 명령 버튼 비활성 |
| 복구 | mavsdk_server | HEARTBEAT 다시 수신 → 같은 `System`, 같은 Flow로 자동 재개. 파라미터 다시 읽음 |

## 6. 테스트 현황

| 테스트 | 수 | 대상 |
| --- | --- | --- |
| `DroneWarningPolicyTest` | 10 | 연결/stale/GPS/배터리 경고, `withPosition` 위치 고정 |
| `LinkLossOutlookTest` | 7 | failsafe 예상, NAV_DLL_ACT 매핑, 메시지 보관 |
| `ExampleUnitTest`, `ExampleInstrumentedTest` | 1 + 1 | 템플릿 (의미 없음) |

`./gradlew testDebugUnitTest` → 18개 통과 (2026-11-09).

**테스트된 것:** `core.model`의 순수 함수 (경고 판정, 위치 신뢰 정책, failsafe 예상).

**테스트되지 않은 것과 그 이유:**

| 대상 | 막는 요인 |
| --- | --- |
| Repository의 상태 누적 (`withConnection`, 스트림 merge) | `MavsdkDroneDataSource`가 interface가 아닌 구체 클래스이고 생성 시 네이티브 `MavsdkServer`를 만든다. 반환 타입도 MAVSDK 타입이다 |
| `runCommand` 실패 분류, STATUSTEXT 첨부, 파라미터 read-back | 위와 같음 + `SystemClock`, `android.util.Log` 직접 호출 (JVM 테스트에서 "not mocked" 예외) |
| ViewModel 중복 차단, 명령 상태 전이 | 시계가 `SystemClock.elapsedRealtime()` 직접 호출 + `delay` 무한 루프 |
| UI | Compose UI 테스트 없음 |

## 7. Phase 2에 대한 진단

현재 구조는 계층 분리가 잘 되어 있고 Phase 1 범위에는 충분하다. **전면 재작성이나 멀티모듈화는 필요 없다.** Phase 2 주차별로 고쳐야 할 지점만 적는다. 지금 고치지 않는다.

| 관찰 | 영향받는 주차 | 제안 |
| --- | --- | --- |
| DataSource가 interface가 아니고 MAVSDK 타입을 반환 | W3 Mission, W5 Command, W6 Recovery | 필요해지는 시점에 DataSource를 interface로 추출하고 도메인 타입을 반환하게 해서 Fake로 테스트 |
| 시계를 직접 호출 (`SystemClock`, `System.currentTimeMillis`) | W5, W6 (Fake Clock 테스트) | `Clock` 주입 |
| 중복 차단이 **명령별**이다 (TAKEOFF가 Running이어도 LAND는 보낼 수 있음) | W5 Command Reliability | 이게 의도인지 정책으로 정한다 (LAND/RTL은 다른 명령 중에도 허용하는 게 맞을 수 있음) |
| `CommandState.Success`가 그대로 남는다 (명령 결과와 기체 상태가 섞여 보일 여지) | W5 | 명령 이력과 결과 확인(Telemetry 대조)으로 분리 |
| `MavsdkServer.run()`은 앱 수명 동안 한 번만 실행. 서버 프로세스나 소켓이 죽으면 복구 경로가 없음 | W6 Recovery | 재연결은 지금 mavsdk_server 내부 재연결에 의존한다. 서버 재시작 정책 검토 |
| `DroneState`가 단일 기체 전제 | W10 Option A (Multi-Vehicle) | 선택 시 VehicleId 도입 |
| `DashboardScreen` 단일 파일 351줄 | W2 지도/Mission 화면 추가 시 | 화면이 늘어날 때 분리 |
| Mission 관련 코드 없음 | W2~W4 | 신규 |
