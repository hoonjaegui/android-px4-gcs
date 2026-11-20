import io.github.hoonjaegui.px4gcs.configureKotlinJvm
import io.github.hoonjaegui.px4gcs.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/** 순수 Kotlin 모듈(:core:model, :core:domain, :core:common). Android API를 쓰면 컴파일 에러가 난다. */
abstract class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.jvm")
            configureKotlinJvm()
            dependencies {
                "testImplementation"(libs.findLibrary("junit").get())
            }
        }
    }
}
