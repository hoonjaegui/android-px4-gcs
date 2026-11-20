import io.github.hoonjaegui.px4gcs.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/**
 * `:feature:*:impl` — 화면(Compose) + ViewModel(Hilt) + EntryProvider.
 * 모든 feature가 공통으로 쓰는 것만 여기서 넣는다. domain·자기 api 모듈 등은 각 모듈이 직접 선언한다(리뷰 시 보이도록).
 */
abstract class AndroidFeatureImplConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "px4gcs.android.library")
            apply(plugin = "px4gcs.android.library.compose")
            apply(plugin = "px4gcs.hilt")

            dependencies {
                "implementation"(project(":core:designsystem"))
                "implementation"(project(":core:navigation"))
                "implementation"(libs.findLibrary("androidx.lifecycle.runtime.compose").get())
                "implementation"(libs.findLibrary("androidx.lifecycle.viewmodel.compose").get())
                "implementation"(libs.findLibrary("hilt.navigation.compose").get())

                "testImplementation"(project(":core:testing"))
            }
        }
    }
}
