package com.example.android_px4_gcs

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.maplibre.android.MapLibre

@HiltAndroidApp
class GcsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // MapView를 만들기 전에 한 번 초기화해야 한다. 타일 서버는 API 키가 필요 없는 OpenFreeMap.
        MapLibre.getInstance(this)
    }
}
