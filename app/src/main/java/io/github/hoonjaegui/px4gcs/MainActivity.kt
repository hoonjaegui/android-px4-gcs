package io.github.hoonjaegui.px4gcs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.hoonjaegui.px4gcs.navigation.GcsNavGraph
import io.github.hoonjaegui.px4gcs.core.designsystem.theme.Px4GcsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Px4GcsTheme {
                GcsNavGraph()
            }
        }
    }
}
