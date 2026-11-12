# C++ 기초 ① 학습 노트 (Phase 2 Week 1)

환경: Apple clang 16.0.0 (arm64), `-std=c++20 -Wall -Wextra`. 에디터는 Cursor + clangd 확장 (`learn/` 폴더에서는 Cursor Tab 끄기 권장).

| 단계 | 내용 | 상태 |
| --- | --- | --- |
| 1 | 컴파일과 링크 (`01-compile-link/`) | 완료 (2026-11-10) |
| 2 | Header / Source 분리 + 일부러 링크 에러 내기 (`02-header-source/`) | 완료 (2026-11-10) |
| 3 | 변수 / 타입 / `const` (Kotlin `val`/`var` 비교) (`03-types/`) | 완료 (2026-11-10) |
| 4 | 참조와 포인터 (`04-reference-pointer/`) | 완료 (2026-11-12) |
| 5 | 콘솔 프로그램 — `DroneWarningPolicy`를 C++로 옮기기 (`05-warning-policy/`) | 5-1 완료 (2026-11-12), 5-2·5-3 남음 |

---

## 1단계 — 컴파일과 링크 (2026-11-09)

```
hello.cpp ─①전처리(-E)─▶ hello.ii ─②컴파일(-c)─▶ hello.o ─③링크─▶ hello
                                                        ▲
                                               libc++ / libSystem (dylib)
```

```bash
clang++ -std=c++20 -E hello.cpp -o hello.ii                 # ① 전처리만
clang++ -std=c++20 -Wall -Wextra -c hello.cpp -o hello.o    # ② 컴파일만
nm -C hello.o | grep -E " T | U "                            # 심볼 확인
clang++ hello.o -o hello && ./hello                          # ③ 링크 + 실행
otool -L hello                                               # 의존 dylib
```

### 실험 결과

| 시도 | 결과 | 배운 것 |
| --- | --- | --- |
| 빈 `hello.cpp` (저장 안 됨) | 컴파일 **성공**, 링크 실패 `Undefined symbols: "_main"` | 컴파일러는 파일 하나만 본다 → main이 없어도 모름. 프로그램 전체를 보는 링커만 잡는다 |
| `return 0:` 오타 | 전처리 **성공**, 컴파일 실패 `hello.cpp:5:13: error: expected ';'` | 전처리기는 문법을 모르는 텍스트 치환기 |
| 정상 | 5줄 → `hello.ii` **83,248줄**, 실행 `PX4 Connected` | `#include`는 헤더를 텍스트로 통째로 붙여 넣는다 (Kotlin `import`와 다름) |

에러 구분:
- `파일:줄:칸: error:` → **컴파일 에러** (한 파일 안의 문제)
- `ld:` / `Undefined symbols` → **링크 에러** (파일끼리 연결 문제)

### `nm` 심볼
- `T` = 이 `.o` 안에 정의(코드)가 있음. 내가 안 쓴 `std::` 함수도 많음 → 헤더에 본문이 있는 inline/템플릿 함수가 include한 쪽 `.o`에 같이 컴파일됨
- `U` = 쓰지만 여기엔 없음 → 링커가 채워야 함 (`locale::use_facet`, `ios_base::getloc`, `__Unwind_Resume`)
- `main`은 `T _main` (macOS는 C 심볼 앞에 `_`를 붙임)

### `otool -L` → 동적 링크
```
/usr/lib/libc++.1.dylib      C++ 표준 라이브러리
/usr/lib/libSystem.B.dylib   macOS 시스템 라이브러리
```
링커는 "이 `U`는 이 dylib에 있다"만 확인해 실행 파일에 적어 두고, 실제 주소 연결은 실행 순간 `dyld`가 한다 (JVM이 실행 중 클래스를 찾는 것과 비슷). 라이브러리 코드를 실행 파일에 복사해 넣으면 정적 링크.

Android 연결: Phase 1의 `libmavsdk_server.so` 크래시는 `libc++_shared.so`를 실행 시점에 못 찾아서 `U` 심볼을 못 채운 것 → jniLibs에 번들해서 해결.

### 확인 질문 (2026-11-10 답변)
1. 빈 `hello.cpp`는 컴파일은 됐는데 링크에서 실패했다. 왜 컴파일러는 이 문제를 잡지 못했나?
2. `.o`가 바로 실행 파일이 되지 못하는 이유는? `U`를 예로 들어 설명.

내 답:

1. 컴파일러는 파일 하나만 봐서. 링커가 파일 전체를 보고 빈칸을 채운다
2. 본문이 없기 때문. U 빈칸이 있어 링커가 해당 본문을 연결해야 함

피드백:
- 1 정답. 보충: 컴파일러는 이 파일이 프로그램 전체인지 일부인지 모른다 → `main` 없는 `.cpp`가 보통이라 에러로 볼 수 없다
- 2 정답. 보충: `.o`에 본문이 아예 없는 게 아니라(`T _main`은 있음) **쓰는 함수 일부의 본문이 `.o` 밖(libc++ 등)에 있어 주소가 빈칸(`U`)**. 링커가 채워야 실행 파일이 된다

---

## 2단계 — Header / Source 분리 (2026-11-10)

| 파일 | 담는 것 | Kotlin 비유 |
| --- | --- | --- |
| `battery.h` | `#pragma once` + **선언** `bool isLowBattery(int percent);` | interface의 시그니처 |
| `battery.cpp` | `#include "battery.h"` + **정의** `{ return percent < 20; }` | 구현 |
| `main.cpp` | `#include <iostream>`, `#include "battery.h"` 후 호출 | 사용하는 쪽 |

- `#include <...>` 시스템/표준 헤더, `#include "..."` 내 헤더 (현재 폴더부터 찾음)
- C++은 `타입 이름` 순서: Kotlin `fun f(a: Int): Boolean` ↔ C++ `bool f(int a)`

```bash
clang++ -std=c++20 -Wall -Wextra -c battery.cpp -o battery.o
clang++ -std=c++20 -Wall -Wextra -c main.cpp -o main.o
nm -C battery.o | grep isLowBattery
nm -C main.o | grep isLowBattery
clang++ main.o battery.o -o battery_check && ./battery_check
```

### 실험 결과

| 시도 | 결과 | 배운 것 |
| --- | --- | --- |
| 첫 작성: `#pregma once`, `'\n\'` 오타 | `battery.cpp`도 컴파일 실패 (오타는 헤더에만 있었는데) | `#include`가 헤더 텍스트를 붙여 넣으므로 **헤더의 실수는 include한 모든 `.cpp`의 에러**가 된다 |
| `nm` | `battery.o`: `T isLowBattery(int)` / `main.o`: `U isLowBattery(int)` | 본문이 있는 파일 = T, 선언만 보고 호출한 파일 = U(빈칸) |
| `main.o + battery.o` 링크 | 실행 `0`, `1` | 링커가 main.o의 U를 battery.o의 T와 연결 |
| A: `main.o`만 링크 | **링크 에러** `Undefined symbols: "isLowBattery(int)", referenced from: _main in main.o` | U의 짝(T)이 아무 데도 없음. 컴파일은 이미 끝난 상태 |
| B: `#include "battery.h"` 주석 처리 | **컴파일 에러** (호출하는 줄에서 `isLowBattery` 모름) | 선언이 없으면 컴파일러가 그 이름을 모른다 |

**결론: 선언이 없으면 컴파일 에러, 본문(정의)이 없으면 링크 에러.**

### 확인 질문 (2026-11-10 답변)
1. 실험 A는 링크 에러, 실험 B는 컴파일 에러였다. 각각 무엇이 빠져서, 누가(컴파일러/링커) 잡았나?
2. 헤더에만 오타가 있었는데 `battery.cpp`까지 컴파일에 실패한 이유는?

내 답:

1. o 파일이 빠졌고, include가 빠졌다. 각각 링커와 컴파일러가 잡았다
2. battery.cpp에 h 내용을 그대로 복사 붙여넣기 하기 때문에 오타가 나면 여기서도 오타가 난다

피드백:
- 1 정답. 보충: 파일 이름보다 **무엇이** 빠졌는지로 기억 — `.o`가 빠짐 = **본문(T)** 없음 → 링커 / include가 빠짐 = **선언** 없음 → 컴파일러
- 2 정답

---

## 3단계 — 변수 / 타입 / `const` (2026-11-10)

| Kotlin | C++ | 비고 |
| --- | --- | --- |
| `val x: Int = 10` | `const int x = 10;` | 변경 불가 |
| `var x: Int = 10` | `int x = 10;` | C++은 **기본이 변경 가능** (Kotlin과 반대) |
| `val x = 10` | `const auto x = 10;` | `auto` = 타입 추론 |
| `Int` / `Double` / `Boolean` | `int` / `double` / `bool` | |
| `String` | `std::string` | `#include <string>` |

```bash
clang++ -std=c++20 -Wall -Wextra main.cpp -o types && ./types
```

| 코드 | 내 예측 | 결과 | 배운 것 |
| --- | --- | --- | --- |
| `int percent = 19.9;` | 19, 경고 | `19`, warning `-Wliteral-conversion` | Kotlin은 에러, C++은 **소수점을 버리고 통과** (경고만) |
| `int unknown;` 후 출력 | 경고, 값은 `false`? | warning `-Wuninitialized`, 값 `-1693835264` | 초기화 안 한 변수는 **메모리에 남아 있던 쓰레기 값**. Kotlin은 컴파일 에러 |
| `bool armed = false;` 출력 | | `0` | `bool`은 `cout`으로 `1`/`0` 출력 (`true`/`false`로 보려면 `std::boolalpha`) |
| 경고 2개 | 2개 | 2개, **실행 파일은 생성됨** | 경고는 빌드를 막지 않는다 |

- 경고 위치: `unknown`은 선언한 9줄이 아니라 **사용한 15줄**에서 경고, note로 `= 0` 초기화 제안
- 문자열 안 오타(`"percnet::"`)는 컴파일러가 못 잡는다 — 그냥 데이터이므로
- `-Werror` 추가 → 같은 두 경고가 `error: ... [-Werror,-Wliteral-conversion]`으로 바뀌고 **실행 파일이 안 생김**. 실무에선 경고를 놓치지 않으려고 켠다

### 확인 질문 (2026-11-10 답변)
1. Kotlin에선 에러인데 C++에선 경고만 나고 통과한 두 가지는?
2. 경고가 나도 실행 파일이 만들어지는데, 왜 실무에서 경고를 무시하면 안 되나?

내 답:

1. 초기화하지 않은 값이 그냥 넘어간다, 타입이 맞지 않는데도 넘어간다
2. 덤프값이 들어가면 안 되는 상황이 있기 때문. 따라서 사전에 컴파일 단계에서 막는 게 좋다

피드백: 둘 다 정답. 용어 보충 — "덤프값"이 아니라 **쓰레기 값(garbage value)**. 2번은 `-Werror`가 바로 "컴파일 단계에서 막는" 방법

---

## 4단계 — 참조와 포인터 (2026-11-10)

### 1부: 값 전달 vs 참조 전달 (`main.cpp`)

| | `void drain(int p)` | `void drainRef(int& p)` |
| --- | --- | --- |
| 받는 것 | 값의 **복사본** (새 칸) | **원래 변수 자체** (별명) |
| 함수 안에서 바꾸면 | 복사본만 바뀜 | 호출한 쪽 변수가 바뀜 |
| 결과 (battery=50) | inside 40 → after **50** | inside 40 → after **40** |

- 예측 40/50/40/40 전부 정답, 실행 일치
- Kotlin 매개변수는 `val`이라 호출한 쪽 변수를 못 바꿈 = C++ 값 전달. C++ `int&` 같은 건 Kotlin에 없음
- C++은 `std::string`·큰 객체도 **기본이 복사** (Kotlin은 객체 공유) → 복사 피하려고 `const std::string&`

### 2부: 포인터 (`pointer.cpp`)

| 코드 | 뜻 |
| --- | --- |
| `int* ptr` | int가 있는 곳의 **주소**를 담는 변수 |
| `&battery` | battery의 주소 (타입 자리의 `int&` 참조와 다른 뜻!) |
| `*ptr` | 그 주소에 가서 꺼낸 값 |
| `nullptr` | 아무것도 안 가리킴 (Kotlin `null`). 단 체크 안 하고 `*ptr` 하면 **실행 중 크래시**, 컴파일러가 안 막아 줌 |
| `const int*` | 가리키는 값을 읽기만 함 |

참조 vs 포인터: 참조는 "없음" 표현 불가·대상 변경 불가, 포인터는 `nullptr` 가능·대상 변경 가능

실행 결과:
```
address: 0x16bb97328
value: 50
battery after *ptr = 30: 30
battery: 30
battery unknown
```

| 시도 | 결과 | 배운 것 |
| --- | --- | --- |
| 저장 안 된 빈 `main.cpp` | 실행 파일 없음 | 저장(⌘S) 확인 |
| `std:cout` (콜론 1개), `<< *ptr` 누락 | 컴파일 에러 | |
| `printBattery`에서 `*ptr` 대신 `ptr` 출력 | `-Werror`인데도 **컴파일 성공**, `battery: 0x16b...` 주소 출력 | 주소 출력도 올바른 문법 → 컴파일러는 의도를 모름. **포인터 실수는 실행 결과를 봐야 드러난다** |

### 확인 질문
1. `int* ptr = &battery;` 이후 `ptr`과 `*ptr`은 각각 무엇인가?
2. 참조가 있는데 포인터가 필요한 이유 하나는? Kotlin과 연결해서.

내 답:

1. ptr은 주소고 *ptr은 그 값을 가져오라는 것 → **정답**
2. 사용자가 답을 물어봐서 Claude가 알려 줌 (2026-11-12): 참조와 달리 포인터는 **"없음"**을 표현할 수 있다. Kotlin의 **`null`**과 같은 역할

2번 추가 설명 (2026-11-10):
- Phase 1 `DroneState.gpsAvailable: Boolean?` (null = 모름) 처럼 "아직 값이 없음"을 표현해야 할 때가 있다
- 참조 `int&`는 만드는 순간 반드시 실제 변수에 붙어야 함 → "없음" 표현 불가
- 포인터 `int*`는 `nullptr` 가능 = Kotlin `null`. `printBattery(nullptr)` → `battery unknown`이 그 예
- **다음 세션 시작:** 30초 실험 — `main` 맨 위에 `int* p = nullptr;` / `int& r;` 넣고 컴파일 (r은 에러 예상) → 확인 후 두 줄 삭제 → 빈칸 답하기:
  "참조와 달리 포인터는 ____을 표현할 수 있다. Kotlin의 ____와 같은 역할이다."

30초 실험 결과 (2026-11-12): `int* p = nullptr;`는 통과, `int& r;`는 컴파일 에러
```
pointer.cpp:13:10: error: declaration of reference variable 'r' requires an initializer
```
→ 참조는 만들 때 붙일 변수가 반드시 있어야 함. "값 없음"만 필요하면 요즘 C++은 `std::optional<T>`도 씀 (5단계에서 사용)

---

## 5단계 — `DroneWarningPolicy`를 C++로 (2026-11-12)

원본: `app/.../core/model/DroneWarnings.kt`. 세 조각으로 나눔: 5-1 경고 3개 / 5-2 header·source 분리 + `assert` 테스트 / 5-3 필드별 수신 시각으로 stale 판정

### 5-1: 연결 끊김 / GPS 없음 / 배터리 부족 (`main.cpp`)

| Kotlin | C++ |
| --- | --- |
| `enum class ConnectionStatus { SEARCHING, ... }` | `enum class ConnectionStatus { Searching, ... };`, 사용은 `ConnectionStatus::Lost` |
| `data class DroneState(...)` | `struct DroneState { ... };` |
| `Int?` / `Boolean?` | `std::optional<int>` / `std::optional<bool>` (비어 있으면 = 모름) |
| `x?.let { it < 20 } ?: false` | `x.has_value() && *x < 20` |
| `gpsAvailable == false` | 똑같음. 비어 있는 optional은 어떤 값과도 `==`가 false |

- 함정: `if (!state.gpsAvailable)`는 "GPS 없음"이 아니라 **"모름(비어 있음)"**. 컴파일 경고도 없음
- 4단계 연결: `const DroneState&`(복사 피함), `const char* label`(문자열 리터럴은 포인터)

예측 (initial F/F/F, lost T/F/T, noGps F/T/F) 전부 정답, 실행 일치:
```
initial: lost=false gpsUnavailable=false lowBattery=false
lost: lost=true gpsUnavailable=false lowBattery=true
noGps: lost=false gpsUnavailable=true lowBattery=false
```
"optional은 true도 false도 아닌데 어떻게 비교하나?" 질문 → 위 표의 `==` 규칙으로 설명
