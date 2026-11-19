package com.origin.launcher.Launcher

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.origin.launcher.manager.LogcatOverlayManager
import com.origin.launcher.utils.FeatureSettings

class LauncherApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        FeatureSettings.init(applicationContext)
        LogcatOverlayManager.init(this)
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
    }

    companion object {
        @JvmStatic
        lateinit var context: Context
            private set

        @JvmStatic
        lateinit var preferences: SharedPreferences
            private set
    }
}
