package io.github.hoonjaegui.px4gcs.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.hoonjaegui.px4gcs.core.designsystem.theme.StatusColors

/** 운용자가 바로 알아야 하는 이상 상태 배너 (CONNECTION LOST 등). */
@Composable
fun WarningBanner(message: String, modifier: Modifier = Modifier) {
    Text(
        text = "⚠ $message",
        color = Color.White,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier
            .fillMaxWidth()
            .background(StatusColors.Danger, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}
