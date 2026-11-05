#!/usr/bin/env bash
# PX4 SITL에 장애를 주입한다. 휴대폰이 아니라 "시뮬레이션된 드론" 쪽을 망가뜨린다.
#
# SITL이 어느 터미널에서 실행 중이든 동작한다. PX4 SITL은 px4 데몬에 붙는 클라이언트 명령
# (build/px4_sitl_default/bin/px4-<module>)을 제공하므로, 이 스크립트는 그걸 호출할 뿐이다.
# pxh> 프롬프트에 직접 쳐도 같은 명령이다 (예: failure gps off).
#
#   tools/sitl_fault.sh <command>
#
set -euo pipefail

PX4_DIR="${PX4_DIR:-$HOME/PX4-Autopilot}"
BIN="$PX4_DIR/build/px4_sitl_default/bin"
ROOTFS="$PX4_DIR/build/px4_sitl_default/rootfs"
REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ONBOARD_PORT=14580   # 앱(MAVSDK)이 붙는 PX4 Onboard 링크의 PX4 쪽 포트

px4() { (cd "$ROOTFS" && "$BIN/px4-$1" "${@:2}"); }
px4_pid() { pgrep -x px4 | head -1; }
bridge_running() { pgrep -f mavlink_udp_tcp_bridge >/dev/null; }

usage() {
  cat <<'EOF'
GPS (드론의 GPS 수신기 고장 — 휴대폰 GPS와 무관)
  gps-off            GPS 끊기                  → 앱: GPS SIGNAL LOST, 위경도 고정
  gps-on             GPS 복구

통신 (드론은 계속 비행, 앱↔드론 링크만 끊김)
  link-cut           브리지 종료               → 앱: CONNECTION LOST + 예상 동작
  link-restore       브리지 재시작             → 앱: 자동 재연결
  dll <0|1|2|3>      통신 두절 시 PX4 행동 (0=없음[기본] 1=Hold 2=RTL 3=Land)
                     ※ 앱은 연결될 때 이 값을 읽으므로, 바꾼 뒤 link-cut/restore 한 번 하면 반영됨

Telemetry (연결은 살아 있고 위치 데이터만 안 옴)
  telemetry-off      위치 메시지 전송 중단     → 앱: TELEMETRY LOST
  telemetry-on       복구

드론 프로세스 전체 정지 (드론 자체가 멈춤 — Timeout 실험용)
  freeze             PX4 일시정지 (이 상태에서 앱 버튼을 누르면 NO RESPONSE)
  resume             재개 (쌓여 있던 명령이 뒤늦게 실행됨)

배터리
  battery-low        빨리 닳고 10%까지 내려가게 (이륙해야 닳음) → 앱: LOW BATTERY
  battery-reset      원래대로 (50%에서 멈춤, 60초)

비행 (앱 버튼 대신)
  takeoff | land | rtl

기타
  status             GPS/통신/파라미터 상태 요약
  reset              모든 장애 해제 + 파라미터 원복
EOF
}

set_param() { px4 param set "$1" "$2" >/dev/null && echo "$1 = $2"; }

cmd="${1:-}"
case "$cmd" in
  gps-off)       set_param SYS_FAILURE_EN 1; px4 failure gps off ;;
  gps-on)        px4 failure gps ok ;;

  link-cut)      if bridge_running; then pkill -f mavlink_udp_tcp_bridge; echo "bridge stopped"; else echo "bridge not running"; fi ;;
  link-restore)
    if bridge_running; then echo "bridge already running"; exit 0; fi
    nohup python3 "$REPO_DIR/tools/mavlink_udp_tcp_bridge.py" >/tmp/gcs_bridge.log 2>&1 &
    echo "bridge started (log: /tmp/gcs_bridge.log)" ;;
  dll)           set_param NAV_DLL_ACT "${2:?usage: dll <0|1|2|3>}" ;;

  telemetry-off) px4 mavlink stream -u "$ONBOARD_PORT" -s GLOBAL_POSITION_INT -r 0 && echo "GLOBAL_POSITION_INT stopped on :$ONBOARD_PORT" ;;
  telemetry-on)  px4 mavlink stream -u "$ONBOARD_PORT" -s GLOBAL_POSITION_INT -r 50 && echo "GLOBAL_POSITION_INT 50Hz on :$ONBOARD_PORT" ;;

  freeze)        kill -STOP "$(px4_pid)" && echo "PX4 frozen (pid $(px4_pid))" ;;
  resume)        kill -CONT "$(px4_pid)" && echo "PX4 resumed" ;;

  battery-low)   set_param SIM_BAT_MIN_PCT 10; set_param SIM_BAT_DRAIN 20 ;;
  battery-reset) set_param SIM_BAT_MIN_PCT 50; set_param SIM_BAT_DRAIN 60 ;;

  takeoff)       px4 commander takeoff ;;
  land)          px4 commander land ;;
  rtl)           px4 commander mode auto:rtl ;;

  status)
    echo "PX4 pid:  $(px4_pid || echo 'not running')"
    if bridge_running; then echo "bridge:   running"; else echo "bridge:   STOPPED (link cut)"; fi
    for p in NAV_DLL_ACT COM_DL_LOSS_T SYS_FAILURE_EN SIM_BAT_MIN_PCT SIM_BAT_DRAIN; do
      px4 param show "$p" | grep -E "^\S.*\b$p\b" | sed 's/^/param:    /'
    done ;;

  reset)
    kill -CONT "$(px4_pid)" 2>/dev/null || true
    px4 failure gps ok >/dev/null 2>&1 || true
    px4 mavlink stream -u "$ONBOARD_PORT" -s GLOBAL_POSITION_INT -r 50 >/dev/null
    set_param SYS_FAILURE_EN 0; set_param NAV_DLL_ACT 0
    set_param SIM_BAT_MIN_PCT 50; set_param SIM_BAT_DRAIN 60
    "$0" link-restore ;;

  ""|-h|--help|help) usage ;;
  *) echo "unknown command: $cmd" >&2; usage; exit 1 ;;
esac
