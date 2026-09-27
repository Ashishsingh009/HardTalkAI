package ai.hardtalk.source

import ai.hardtalk.source.data.billing.AndroidBillingRepository
import ai.hardtalk.source.voice.AndroidVoiceHost
import android.graphics.Color
import android.os.Bundle
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

        setContent {
            App(
                apiBaseUrl = BuildConfig.API_BASE_URL,
                billingRepository = billing,
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
