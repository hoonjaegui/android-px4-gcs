import io.github.hoonjaegui.px4gcs.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/**
 * `:feature:*:api` — 그 feature로 이동하기 위한 NavKey만 둔다. 다른 feature나 app은 api만 의존하고 impl은 모른다.
 */
abstract class AndroidFeatureApiConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "px4gcs.android.library")
            apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
            dependencies {
                "api"(project(":core:navigation"))
                "implementation"(libs.findLibrary("kotlinx.serialization.core").get())
            }
        }
    }
}
