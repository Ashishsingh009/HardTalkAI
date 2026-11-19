import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class CheckReleaseApiBaseUrlTask : DefaultTask() {
    @get:Input
    abstract val apiBaseUrl: Property<String>

    @TaskAction
    fun verify() {
        val url = apiBaseUrl.get().trim()
        if (!url.startsWith("https://")) {
            throw GradleException(
                "Release builds must use an HTTPS API host. Pass " +
                    "-Phardtalk.apiBaseUrl=https://YOUR_HOST " +
                    "(see docs/backend-hosting-plan.md). Debug still defaults to " +
                    "http://10.0.2.2:3001.",
            )
        }
    }
}
