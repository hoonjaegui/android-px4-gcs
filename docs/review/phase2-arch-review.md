# 멀티모듈 전환 리뷰 인계 (2026-11-20)

> 다른 컴퓨터에서 리뷰를 이어가기 위한 문서. 작업은 끝났고 **리뷰 대기 중**이다.
> 리뷰가 끝나면 `phase2-arch` → `phase2-week2` 머지 → C++ 1단계 시작.

## 상태 요약

| 항목 | 상태 |
|---|---|
| 브랜치 | `phase2-arch` (`phase2-week2`에서 분기, 원격에 푸시됨) |
| 커밋 | 14개 = 전환 14단계 (아래 표), 마지막 `4fa1800` 이후 이 문서 |
| 빌드 / 테스트 | `./gradlew assembleDebug test` 통과, 테스트 86개 (전환 전 56개) |
| 실기기 회귀 | 완료 — [verification/phase2-arch-regression.md](../verification/phase2-arch-regression.md) |
| 머지 | **안 함** (리뷰 후) |
| 남은 Week 2 작업 | C++ 1~5단계 + C++ 객체 수명 주말 검증 (이번 주 안에 하기로 함) |

배경·결정: [decisions/0001](../decisions/0001-multi-module-clean-architecture.md) / 현재 구조: [architecture.md](../architecture.md)

## 새 컴퓨터에서 시작

```bash
git fetch origin
git switch phase2-arch
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug test
```

- 패키지가 `io.github.hoonjaegui.px4gcs`로 바뀌었다. 기기에 예전 앱(`com.example.android_px4_gcs`)이 있으면 `adb uninstall com.example.android_px4_gcs`
- 패키지 변경 직후 첫 빌드에서 Hilt가 옛 패키지를 찾으며 실패하면 `./gradlew clean`
- 커밋 하나씩 보기: `git log --oneline phase2-week2..phase2-arch` → `git show <커밋>` (또는 Android Studio Git 창에서 커밋별 diff)

## 리뷰 순서 (커밋 1개 = 1단계)

| # | 커밋 | 단계 | 리뷰 포인트 | 리뷰 |
|---|---|---|---|---|
| 1 | `af1154f` | build-logic | convention plugin 6개가 각각 무엇을 넣는지. 전환 전후 의존성·APK 동일 확인함 | [ ] |
| 2 | `c5822c1` | 패키지명 | 이름 변경뿐인지 (36파일) | [ ] |
| 3 | `5be61fa` | `:core:model` | `flightMode: String` → `FlightMode` enum, `CommandState`를 dashboard로 | [ ] |
| 4 | `d338358` | `:core:common` | ApplicationScope·Dispatcher·Clock. data → di 역방향 의존 해소 | [ ] |
| 5 | `f436155` | **`:core:domain`** | **가장 중요.** UseCase 10개, ViewModel이 UseCase만 받도록 | [ ] |
| 6 | `95b7aa0` | `:core:testing` | Fake 저장소, TestClock(가상 시간) | [ ] |
| 7 | `8348979` | `:core:data:mission` | Hilt 바인딩을 data 모듈로 | [ ] |
| 8 | `027c566` | `:core:data:drone` | MAVSDK가 여기만, `MavsdkMappers` 분리, jniLibs 이동 | [ ] |
| 9 | `094fce0` | `:core:designsystem` | `StatusColors`, `Notice`, `WarningBanner` | [ ] |
| 10 | `accf5da` | Navigation 3 | NIA `NavigationState`/`Navigator` 이식 | [ ] |
| 11 | `10d07aa` | feature api/impl | feature convention plugin 2개, EntryProvider | [ ] |
| 12 | `e1a2244` | app 정리 | 권한을 쓰는 모듈로 이동 | [ ] |
| 13 | `0f06bd5` | Konsist + 문서 | 아키텍처 규칙 7개, 문서 | [ ] |
| 14 | `4fa1800` | 회귀 검증 | 결과 기록 | [ ] |

## 리뷰 때 결정해 줄 것 (작업 중 Claude가 판단한 부분)

1. **기체 상태 UseCase 통합** — 계획의 `ObserveDroneState` + `ObserveDroneWarnings` 대신 `ObserveDroneStatusUseCase` 하나 (Dashboard가 상태·경고·현재 시각을 항상 같이 씀). 그래서 UseCase 11 → 10개
2. **새 규칙** — `SetLinkLossActionUseCase`가 TERMINATE/DISARM/UNKNOWN을 `IllegalArgumentException`으로 거부 (전에는 화면이 선택지를 안 보여주는 것으로만 막음)
3. **동작 변경** — Navigation 3(NIA 규칙)에서 Dashboard 탭으로 가면 Mission 화면이 기록에서 빠진다. 돌아오면 거부 메시지·지도 카메라 위치가 초기화됨 (plan은 Repository라 유지). 유지가 필요하면 Navigator의 `goToTopLevel` 규칙 변경
4. **버전** — navigation3 1.2.0 (1.2.2는 2026-11-19 배포라 보류), Konsist 0.18.1 (JVM 17 필요 → `:konsist` 모듈만 17), `hiltViewModel()`은 정식 1.2.0 유지 (NIA의 alpha 불필요 확인)
5. **`DefaultDroneRepository` 분리 범위** — MAVSDK 변환만 분리(254 → 204줄). 명령·파라미터 처리 분리는 동작 변경 위험 때문에 보류

## 작업 중 발견 (기록만)

- Konsist 테스트가 다른 모듈 소스를 입력으로 몰라 UP-TO-DATE로 건너뛰던 문제 → `konsist/build.gradle.kts`에서 전체 `.kt`를 입력으로 등록해 해결, 규칙 7개 각각 위반 시 실패 확인
- PX4 v1.18.0-rc1은 `SYS_FAILURE_EN`이 재부팅 후 적용 → `tools/sitl_fault.sh gps-off`가 실패할 수 있음. 순서: `param set SYS_FAILURE_EN 1` → SITL 재시작 → `failure gps off`
- 병합 APK의 위치 권한(`ACCESS_FINE/COARSE_LOCATION`)은 MapLibre AAR에서 옴 (A4-2부터, 이번 변경 아님)
- 착지 후 고도 `-0.0m`류 표시는 기존 이슈 그대로

## 리뷰 후 할 일

1. 리뷰 코멘트 반영 (있으면)
2. `phase2-arch` → `phase2-week2` 머지, 푸시
3. C++ 1단계: class / 생성자·소멸자 / 객체 수명 (`tools/cpp-link-monitor/learn/week2/`, 사용자가 직접 작성)
