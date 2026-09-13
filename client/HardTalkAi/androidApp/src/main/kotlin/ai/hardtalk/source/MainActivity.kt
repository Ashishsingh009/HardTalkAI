package ai.hardtalk.source

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ai.hardtalk.source.data.billing.AndroidBillingRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        val apiKey = BuildConfig.REVENUECAT_GOOGLE_API_KEY.trim()
        val debugUngated = BuildConfig.DEBUG && apiKey.isEmpty()
        if (apiKey.isNotEmpty()) {
            AndroidBillingRepository.configure(this, apiKey)
        }
        val billing = AndroidBillingRepository(
            ungated = debugUngated,
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
