package com.github.damontecres.wholphin.util

import android.content.Context
import com.github.damontecres.wholphin.data.MostRecentServer
import com.github.damontecres.wholphin.services.hilt.AppModule
import com.github.damontecres.wholphin.services.hilt.DeviceModule
import com.google.auto.service.AutoService
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.acra.ReportField
import org.acra.config.CoreConfiguration
import org.acra.data.CrashReportData
import org.acra.sender.ReportSender
import org.acra.sender.ReportSenderException
import org.acra.sender.ReportSenderFactory
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.systemApi
import org.jellyfin.sdk.api.okhttp.OkHttpFactory
import org.jellyfin.sdk.createJellyfin
import org.json.JSONObject

internal object CrashReports {
    const val PREFERENCES_NAME = "wholphin_crash_reporting"
    const val ENABLED = "acra.enable"

    val fields = listOf(
        ReportField.ANDROID_VERSION,
        ReportField.APP_VERSION_CODE,
        ReportField.APP_VERSION_NAME,
        ReportField.BRAND,
        ReportField.PHONE_MODEL,
        ReportField.STACK_TRACE,
    )

    fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = preferences(context).getBoolean(ENABLED, false)

    // Throwable messages can contain URLs or media paths. Keep exception types and stack frames.
    private val exceptionType = Regex("""^((?:Caused by: |Suppressed: )?(?:[\w$]+\.)+[\w$]+)(?::.*)?$""")
    private val stackFrame = Regex("""^at [\w.$<>]+\((?:[\w.$-]+(?::\d+)?|Native Method|Unknown Source)\)$""")

    fun sanitizeStackTrace(trace: String): String =
        trace.lineSequence()
            .mapNotNull { line ->
                val trimmed = line.trim()
                exceptionType.matchEntire(trimmed)?.groupValues?.get(1)
                    ?: trimmed.takeIf(stackFrame::matches)
            }.joinToString("\n")
}

/**
 * Sends a crash report to the current server
 */
@AutoService(ReportSenderFactory::class)
class CrashReportSenderFactory : ReportSenderFactory {
    override fun create(
        context: Context,
        config: CoreConfiguration,
    ): ReportSender = CrashReportSender()

    override fun enabled(config: CoreConfiguration): Boolean = true
}

class CrashReportSender : ReportSender {
    override fun send(
        context: Context,
        errorContent: CrashReportData,
    ) {
        if (!CrashReports.isEnabled(context)) {
            throw ReportSenderException("Crash reporting is disabled")
        }
        try {
            val recentServer = AppModule.mostRecentServerProvider(context).get()
            val api = recentServer.createApi(context)
            if (api != null) {
                val obj = JSONObject()
                val report = errorContent.toMap()
                CrashReports.fields.forEach { field ->
                    val value = report[field.name]?.toString() ?: return@forEach
                    obj.put(field.name, if (field == ReportField.STACK_TRACE) CrashReports.sanitizeStackTrace(value) else value)
                }
                runBlocking {
                    api.systemApi.logFile(obj.toString(2))
                }
            } else {
                throw ReportSenderException("Could not find valid server and/or credentials to use")
            }
        } catch (ex: Exception) {
            throw ReportSenderException("Exception while sending crash report", ex)
        }
    }
}

private fun MostRecentServer.createApi(context: Context): ApiClient? =
    (this as? MostRecentServer.ServerAndUser)?.let {
        val okHttpClient =
            OkHttpClient
                .Builder()
                .build()
        val okHttpFactory = OkHttpFactory(okHttpClient)
        val api =
            createJellyfin {
                this.context = context
                clientInfo = AppModule.clientInfo(context)
                deviceInfo = DeviceModule.deviceInfo(context)
                apiClientFactory = okHttpFactory
                socketConnectionFactory = okHttpFactory
                minimumServerVersion = Jellyfin.minimumVersion
            }.createApi(baseUrl = serverUrl, accessToken = accessToken)
        api
    }
