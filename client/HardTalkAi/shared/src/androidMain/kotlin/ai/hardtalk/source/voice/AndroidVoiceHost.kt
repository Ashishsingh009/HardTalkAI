package ai.hardtalk.source.voice

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import java.lang.ref.WeakReference

object AndroidVoiceHost {
    @Volatile
    var applicationContext: Context? = null
        private set

    fun attach(activity: ComponentActivity) {
        applicationContext = activity.applicationContext
        MicrophonePermissionBridge.attach(activity)
    }
}

object MicrophonePermissionBridge {
    private var activityRef: WeakReference<Activity>? = null
    private var launcher: ActivityResultLauncher<String>? = null
    private var pending: CompletableDeferred<Boolean>? = null

    fun attach(activity: ComponentActivity) {
        activityRef = WeakReference(activity)
        launcher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            pending?.complete(granted)
            pending = null
        }
    }

    suspend fun ensure(): Boolean {
        val activity = activityRef?.get() ?: return false
        val granted = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) return true
        val request = CompletableDeferred<Boolean>()
        pending = request
        val currentLauncher = launcher ?: return false
        currentLauncher.launch(Manifest.permission.RECORD_AUDIO)
        return request.await()
    }
}
