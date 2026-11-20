# Android GCS 아키텍처

> 2026-11-20 멀티모듈 전환 후 구조 (브랜치 `phase2-arch`). 결정 배경은 [decisions/0001](decisions/0001-multi-module-clean-architecture.md),
> 전환 전 단일 모듈 구조는 [architecture-baseline-phase1.md](architecture-baseline-phase1.md), 버전은 [environment.md](environment.md).

빌드·모듈 구성은 Now in Android(NIA), 의존 방향·UseCase 규칙은 Clean Architecture를 따른다.

## 1. 모듈 의존 그래프

```
                          :app  (Application, MainActivity, 하단 탭 + NavDisplay, AppModule)
          ┌───────────────┬─┴──────────────┬───────────────────┐
          ▼               ▼                ▼                   ▼
 :feature:dashboard  :feature:mission   :core:data:drone   :core:data:mission
   :api   :impl        :api   :impl      (Android, MAVSDK)   (JVM, InMemory)
     │      │            │      │              │                   │
     │      ├────────────┼──────┼──────────────┴─────────┬─────────┘
     │      │            │      │                        ▼
     │      └────────────┼──────┴──────────────▶  :core:domain (JVM)  Repository interface · UseCase · Policy
     ▼                   ▼                               │ api
 :core:navigation ◀──────┘                               ├──▶ :core:model  (JVM)  DroneState · MissionPlan …
 (Navigation 3)                                          └──▶ :core:common (JVM)  ApplicationScope · Dispatcher · Clock

 feature:*:impl 공통 (convention plugin): :core:designsystem, :core:navigation, lifecycle compose, hiltViewModel
 테스트 전용: :core:testing (Fake 저장소, TestClock, MainDispatcherRule) · :konsist (아키텍처 규칙)
```

| 모듈 | 종류 | 의존 (`build.gradle.kts`) | 담는 것 |
|---|---|---|---|
| `:app` | application | feature 4개, data 2개, common, designsystem, navigation | 조립만. Kotlin 5파일 |
| `:feature:*:api` | android.feature.api | (core:navigation) | `NavKey` 하나 |
| `:feature:*:impl` | android.feature.impl | 자기 api, core:domain | Screen, ViewModel, `*Entry()` |
| `:core:domain` | jvm | api: model, common | Repository interface 2, UseCase 10, Policy 3 |
| `:core:model` | jvm | — | 도메인 타입 |
| `:core:common` | jvm + hilt | — | 코루틴 스코프·Dispatcher, `Clock` |
| `:core:data:drone` | android + hilt | core:domain | MAVSDK 구현, `libc++_shared.so` |
| `:core:data:mission` | jvm + hilt | core:domain | InMemory 구현 |
| `:core:designsystem` | android + compose | — | 테마, `StatusColors`, `Notice`, `WarningBanner` |
| `:core:navigation` | android + compose | — | `NavigationState`, `Navigator` (NIA) |
| `:core:testing` | jvm | api: core:domain | 테스트 공용 도구 |
| `:konsist` | jvm (17) | — | 아키텍처 규칙 테스트 |

**의존 방향**: `feature → domain ← data`. feature는 data를 모르고(모듈 의존 없음 → import하면 컴파일 에러), data는 domain의 interface를 구현한다. 어떤 구현을 쓸지는 `:app`이 data 모듈을 의존성에 넣는 것으로 정한다(각 data 모듈의 Hilt `@Binds`).

## 2. 규칙

| 규칙 | 지키는 수단 |
|---|---|
| feature는 data를 import하지 않는다 | 모듈 의존성(컴파일) + Konsist |
| ViewModel은 **UseCase만** 주입받는다 | Konsist (`view models depend only on use cases`) |
| UseCase는 `:core:domain`에 있고 `operator fun invoke` 하나 | Konsist |
| Repository interface는 `:core:domain`에 | Konsist |
| feature끼리는 api(NavKey)만 안다, 다른 feature의 impl은 import 금지 | Konsist |
| MAVSDK(`io.mavsdk`)는 `:core:data:drone`만 | 모듈 의존성 + Konsist |
| domain·model은 Android를 모른다 | JVM 모듈(컴파일) + Konsist |
| 시간은 `Clock`, Dispatcher는 주입 | 리뷰 (`SystemClock`, `Dispatchers.IO` 직접 사용 금지) |

Konsist 규칙 7개는 각각 위반 파일을 넣었을 때 실패하는 것을 확인했다(2026-11-20). `:konsist:test`는 프로젝트의 모든 `.kt`를 입력으로 선언해야 다른 모듈 변경 시 다시 실행된다(`konsist/build.gradle.kts`).

## 3. 흐름

**Telemetry** — `MavsdkDroneDataSource`(Rx → Flow) → `DefaultDroneRepository`(스트림별 Update를 merge+scan, MAVSDK 타입은 `MavsdkMappers.kt`에서 도메인 타입으로) → `StateFlow<DroneState>` → `ObserveDroneStatusUseCase`(500ms ticker와 combine해 stale·경고 판정) → `DashboardViewModel` → `DashboardScreen`

**Command** — 버튼 → `DashboardViewModel.onCommand`(Running 중 중복 차단) → `ExecuteDroneCommandUseCase` → `DroneRepository.arm()` 등 → MAVSDK `action.*().await()` → `CommandResult`(실패 시 STATUSTEXT 사유 첨부) → `CommandState`

**Mission 편집** — 지도 탭/목록 → `MissionPlannerViewModel`(문자열 해석, 거부 표시) → `AddWaypointUseCase` 등(규칙: 잘못된 Waypoint 추가·고도 변경은 거부, 삭제·순서 변경은 허용 후 errors로 표시) → `MissionPlanRepository`(InMemory) → `ObserveMissionPlanUseCase`(plan + 검증) → 화면

**Navigation** — `GcsNavGraph`: `rememberNavigationState`(탭별 back stack) + `NavDisplay`. 각 feature impl이 `dashboardEntry()`/`missionPlannerEntry()`로 자기 화면을 등록. 화면별 ViewModelStore라 탭을 떠나면(Dashboard로 이동 시 Mission 기록 제거) Mission ViewModel도 정리된다 — plan은 Repository라 유지.

## 4. 테스트

`./gradlew test` — 2026-11-20 기준 79 + Konsist 7 = 86개.

| 모듈 | 개수 | 내용 |
|---|---|---|
| core:model | 11 | MissionPlan 편집 |
| core:domain | 49 | Policy 3종, UseCase 10종(Fake 저장소 + 가상 시간 TestClock) |
| core:data:drone | 5 | MAVSDK → 도메인 변환 |
| core:data:mission | 1 | InMemory 저장 |
| core:navigation | 5 | Navigator (탭·뒤로 가기) |
| feature:mission:impl | 8 | ViewModel (입력 해석, 거부 표시, UseCase 연결) |
| konsist | 7 | 아키텍처 규칙 |

빈 곳: `DefaultDroneRepository`(MAVSDK `System`이 구체 클래스라 Fake 불가), `DashboardViewModel`, Compose UI.
