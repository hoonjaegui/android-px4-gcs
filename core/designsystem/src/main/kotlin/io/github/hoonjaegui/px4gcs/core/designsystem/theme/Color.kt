package io.github.hoonjaegui.px4gcs.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.util.Locale

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

/**
 * GCS 상태 색. 의미가 정해져 있으므로 화면마다 따로 정하지 않는다.
 * 지도(MapLibre)처럼 Compose Color를 못 쓰는 곳은 [hex]를 쓴다.
 */
object StatusColors {
    /** 장애·위험 (연결 끊김, 저전압, 거부). */
    val Danger = Color(0xFFC62828)

    /** 주의 (마지막 수신 후 시간이 지남 등). */
    val Warning = Color(0xFFE65100)

    /** 과거 값 — 현재 값처럼 보이면 안 되는 값. */
    val Stale = Color(0xFF9E9E9E)

    /** "#9E9E9E" 형식. */
    fun hex(color: Color): String = String.format(Locale.US, "#%06X", 0xFFFFFF and color.toArgb())
}
