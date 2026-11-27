package com.github.damontecres.wholphin

import android.app.Application
import android.os.Build
import android.os.StrictMode
import android.util.Log
import androidx.compose.runtime.Composer
import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import androidx.core.content.edit
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.github.damontecres.wholphin.util.CrashReports
import dagger.hilt.android.HiltAndroidApp
import org.acra.config.dialog
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.model.ServerVersion
import timber.log.Timber
import javax.inject.Inject

@OptIn(ExperimentalComposeRuntimeApi::class)
@HiltAndroidApp
class WholphinApplication :
    Application(),
    Configuration.Provider {
    init {
        instance = this

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(
                object : Timber.Tree() {
                    override fun isLoggable(
                        tag: String?,
                        priority: Int,
                    ): Boolean = priority >= Log.INFO

                    override fun log(
                        priority: Int,
                        tag: String?,
                        message: String,
                        t: Throwable?,
                    ) {
                        Log.println(priority, tag ?: "Wholphin", message)
                    }
                },
            )
        }

        Composer.setDiagnosticStackTraceEnabled(BuildConfig.DEBUG)
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy
                    .Builder()
                    .detectNetwork()
                    .penaltyLog()
                    .penaltyDeathOnNetwork()
                    .build(),
            )
//            StrictMode.setVmPolicy(
//                StrictMode.VmPolicy
//                    .Builder()
//                    .detectAll()
//                    .penaltyLog()
//                    .build(),
//            )
        }
        val crashPreferences = CrashReports.preferences(this)
        if (!crashPreferences.contains(CrashReports.ENABLED)) {
            crashPreferences.edit { putBoolean(CrashReports.ENABLED, false) }
        }
        initAcra {
            buildConfigClass = BuildConfig::class.java
            sharedPreferencesName = CrashReports.PREFERENCES_NAME
            reportFormat = StringFormat.JSON
            reportContent = CrashReports.fields
            dialog {
                title = getString(R.string.crash_report_title)
                text = getString(R.string.crash_report_confirmation)
                positiveButtonText = getString(R.string.crash_report_send)
                negativeButtonText = getString(R.string.crash_report_decline)
            }
        }
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .build()

    companion object {
        lateinit var instance: WholphinApplication
            private set

        val minimumServerVersion: ServerVersion = Jellyfin.minimumVersion

        val newMinimumServerVersion: ServerVersion = ServerVersion(12, 0, 0)
    }
}
