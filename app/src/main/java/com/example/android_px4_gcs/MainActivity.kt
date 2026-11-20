package com.example.android_px4_gcs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.android_px4_gcs.navigation.GcsNavGraph
import com.example.android_px4_gcs.ui.theme.Androidpx4gcsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Androidpx4gcsTheme {
                GcsNavGraph()
            }
        }
    }
}
