package io.github.hoonjaegui.px4gcs.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** 한 줄 안내 상자 (거부 사유, plan 문제 등). */
@Composable
fun Notice(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(color = color, shape = MaterialTheme.shapes.small, modifier = modifier.fillMaxWidth()) {
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(8.dp))
    }
}
