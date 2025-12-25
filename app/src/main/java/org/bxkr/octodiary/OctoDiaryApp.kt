package org.bxkr.octodiary

import android.app.Application
import android.util.Log
import org.bxkr.octodiary.BuildConfig
import timber.log.Timber
import org.bxkr.octodiary.network.NetworkService
import org.bxkr.octodiary.data.AuthRepository

class OctoDiaryApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NetworkService.init(this) // Инициализация NetworkService с контентом
        val authRepository = AuthRepository(this, DataService)
        DataService.init(this, authRepository) // Инициализация DataService (БД)
        DataService.authRepository = authRepository // Записывает принудительно в DataService
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        Log.d("OctoDiaryApp", "Application onCreate called")
        
        // Инициализация основных компонентов
        try {
            // DataService это object (синглтон), не нужно его инициализировать, просто установим subsystem
            DataService.subsystem = Diary.MES
        } catch (e: Exception) {
            Log.e("OctoDiaryApp", "Failed to initialize components", e)
        }
    }
    
    companion object {
        const val TAG = "OctoDiaryApp"
    }
}
