package ai.hardtalk.source.billing

import android.app.Activity
import java.lang.ref.WeakReference

/**
 * Holds the foreground Activity so RevenueCat can present the Test Store / Play purchase sheet.
 */
object PurchaseActivityHolder {
    private var ref: WeakReference<Activity>? = null

    fun attach(activity: Activity) {
        ref = WeakReference(activity)
    }

    fun detach(activity: Activity) {
        if (ref?.get() === activity) {
            ref = null
        }
    }

    fun current(): Activity? = ref?.get()
}
