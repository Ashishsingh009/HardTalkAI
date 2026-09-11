package ai.hardtalk.source

import ai.hardtalk.source.billing.PurchaseActivityHolder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        PurchaseActivityHolder.attach(this)

        val entitlements = (application as HardTalkApplication).entitlements
        setContent {
            App(
                apiBaseUrl = BuildConfig.API_BASE_URL,
                entitlements = entitlements,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        PurchaseActivityHolder.attach(this)
    }

    override fun onDestroy() {
        PurchaseActivityHolder.detach(this)
        super.onDestroy()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
