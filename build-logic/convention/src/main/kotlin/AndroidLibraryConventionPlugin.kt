import com.android.build.api.dsl.LibraryExtension
import io.github.hoonjaegui.px4gcs.configureKotlinAndroid
import io.github.hoonjaegui.px4gcs.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** Android 라이브러리 모듈(:core:data:*, :core:designsystem, :feature:* 등) 공통. */
abstract class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.library")

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                testOptions.targetSdk = 36
                lint.targetSdk = 36
            }
            dependencies {
                "testImplementation"(libs.findLibrary("junit").get())
            }
        }
    }
}
