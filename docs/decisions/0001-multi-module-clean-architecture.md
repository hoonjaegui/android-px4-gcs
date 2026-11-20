# 0001. 멀티모듈 + Clean Architecture로 전환

- 날짜: 2026-11-20 (Phase 2 Week 2, Android A1~A4 완료 직후 · C++ 시작 전)
- 상태: **결정됨** (세부 항목 일부 미정 — 아래 "미정")

## 배경

계획서(`GCS with PX4 — Phase 2.md`) 9장은 "기존 Android 모듈 구성은 유지한다"고 되어 있다. 이 결정은 그와 다르다.

이유: 이제부터 **코드 리뷰를 사용자가 직접 전부 한다.** 현재는 `:app` 한 모듈(파일 25개, 약 2,000줄)에 레이어가 패키지로만 나뉘어 있어 한눈에 들어오지 않고, 레이어 경계가 관례로만 지켜져 컴파일러가 막아 주지 않는다. 실제로 경계를 어긴 곳이 있다(2026-11-20 구조 점검):

1. `data → di` 역방향 의존 (`@ApplicationScope`, `@MavsdkConnectionUrl`이 `di/`에 정의됨)
2. `domain/`에는 Repository interface 2개뿐, 비즈니스 규칙은 `core/*`와 `MissionPlannerViewModel`(편집 거부 규칙)에 흩어짐
3. UI 상태(`CommandState`)가 `core.model`에, 표시용 문자열 변환(`flightMode` 이름)이 data에 있음
4. `SystemClock`/`Log`를 data·ViewModel에서 직접 호출 (Phase 1에서 기록한 테스트 불가 원인)
5. `DefaultDroneRepository`(244줄)가 상태 병합·명령·파라미터·MAVSDK 오류 변환·STATUSTEXT 대기를 모두 담당
6. DI가 `di/DroneModule.kt` 한 파일, Fake는 app test에만 있음
7. 공통 UI 요소·문구가 각 Screen에 흩어짐 (`DashboardScreen` 351줄)

모듈로 나누면 1·3·4는 컴파일 에러로 드러나고, 리뷰어는 모듈 의존 목록(`build.gradle.kts`)만 보고 경계를 판단할 수 있다.

## 결정

| 항목 | 결정 |
|---|---|
| 구조 | 멀티모듈 + Clean Architecture |
| data 모듈 | **drone / mission으로 분리** (MAVSDK·mavsdk_server 의존성을 drone 쪽에만 가둠) |
| UseCase | **모든 기능에 만든다.** ViewModel은 UseCase만 호출 — 일관된 규칙이 리뷰 기준이 되도록 |
| 빌드 설정 | Now in Android(NIA)의 `build-logic` convention plugin 방식 참고 |
| 진행 원칙 | 모듈 하나 옮길 때마다 단위 테스트 전체 + 실기기 동작 확인 후 다음 모듈 |

## 미정 (NIA 확인 결과 생긴 쟁점)

NIA(`android/nowinandroid`, 2026-10-20 커밋 `2982e63` 기준)는 Android 공식 아키텍처 가이드를 따르며 위 결정과 다른 점이 있다:

- **의존 방향**: NIA는 `core:domain → core:data`(domain이 data에 의존), Repository interface도 `core:data`에 있다. Clean Architecture는 반대로 `data → domain`(interface는 domain, 구현은 data).
- **UseCase**: NIA는 domain을 선택 사항으로 두고 UseCase가 3개뿐, ViewModel이 Repository를 직접 많이 쓴다(`UserDataRepository` 6곳 등). → "모든 기능에 UseCase"와 다름.
- **Navigation**: NIA는 Navigation 3 + `feature:*:api / impl` 분리. 현재 앱은 Grider_Rider와 같은 Navigation Compose 2.8.5(A4-1).
- **패키지명**: `com.example.android_px4_gcs` 변경 여부.

결정되면 이 문서에 추가한다.
