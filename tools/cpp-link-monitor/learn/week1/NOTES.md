# C++ 기초 ① 학습 노트 (Phase 2 Week 1)

환경: Apple clang 16.0.0 (arm64), `-std=c++20 -Wall -Wextra`. 에디터는 Cursor + clangd 확장 (`learn/` 폴더에서는 Cursor Tab 끄기 권장).

| 단계 | 내용 | 상태 |
| --- | --- | --- |
| 1 | 컴파일과 링크 (`01-compile-link/`) | 실습 완료, **확인 질문 답변 대기** |
| 2 | Header / Source 분리 + 일부러 링크 에러 내기 | 다음 |
| 3 | 변수 / 타입 / 함수 / `const` (Kotlin `val`/`var` 비교) | |
| 4 | 참조와 포인터 (값/참조/포인터 전달) | |
| 5 | 콘솔 프로그램 — `DroneWarningPolicy`를 C++로 옮기기 | |

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

### 확인 질문 (답변 대기)
1. 빈 `hello.cpp`는 컴파일은 됐는데 링크에서 실패했다. 왜 컴파일러는 이 문제를 잡지 못했나?
2. `.o`가 바로 실행 파일이 되지 못하는 이유는? `U`를 예로 들어 설명.

내 답:

-
-
