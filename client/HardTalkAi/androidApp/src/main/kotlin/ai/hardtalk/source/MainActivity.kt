package ai.hardtalk.source

import ai.hardtalk.source.data.billing.AndroidBillingRepository
import ai.hardtalk.source.data.coaching.AndroidRetryCoachingModel
import ai.hardtalk.source.voice.AndroidVoiceHost
import android.graphics.Color
import android.os.Bundle
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        AndroidVoiceHost.attach(this)

        val apiKey = BuildConfig.REVENUECAT_GOOGLE_API_KEY.trim()
        val ungated = BuildConfig.UNGATED_CATALOG
        if (!ungated && apiKey.isNotEmpty()) {
            AndroidBillingRepository.configure(this, apiKey)
        }
        val billing = AndroidBillingRepository(
            ungated = ungated,
            activityProvider = { this },
        )
        val retryCoaching = if (Process.is64Bit()) AndroidRetryCoachingModel(applicationContext) else null

        setContent {
            App(
                apiBaseUrl = BuildConfig.API_BASE_URL,
                billingRepository = billing,
                voiceSecret = BuildConfig.HARDTALK_VOICE_SECRET,
                retryCoachingModel = retryCoaching,
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
