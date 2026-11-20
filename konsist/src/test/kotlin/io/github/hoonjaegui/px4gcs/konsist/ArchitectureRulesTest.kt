package io.github.hoonjaegui.px4gcs.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

/**
 * docs/decisions/0001의 규칙. 모듈 의존성(컴파일러)으로 막지 못하는 것만 여기서 검사한다.
 * 예: Repository interface는 :core:domain에 있어서 feature가 의존할 수는 있지만, ViewModel이 직접 받으면 안 된다.
 */
class ArchitectureRulesTest {

    private val project = Konsist.scopeFromProject()
    private val productionFiles = project.files.filterNot { it.path.contains("/src/test/") || it.path.contains("/build-logic/") }

    @Test
    fun `view models depend only on use cases`() {
        project.classes()
            .withNameEndingWith("ViewModel")
            .assertTrue { viewModel ->
                viewModel.primaryConstructor?.parameters.orEmpty().all { it.type.name.endsWith("UseCase") }
            }
    }

    @Test
    fun `use cases live in domain and expose a single operator invoke`() {
        project.classes()
            .withNameEndingWith("UseCase")
            .assertTrue { useCase ->
                useCase.resideInPackage("..core.domain..") &&
                    useCase.functions().count { it.name == "invoke" && it.hasOperatorModifier } == 1
            }
    }

    @Test
    fun `repository interfaces live in domain`() {
        project.interfaces()
            .withNameEndingWith("Repository")
            .assertTrue { it.resideInPackage("..core.domain..") }
    }

    @Test
    fun `features do not import the data layer`() {
        productionFiles
            .filter { it.path.contains("/feature/") }
            .assertFalse { file -> file.imports.any { it.name.startsWith("$ROOT.core.data") } }
    }

    @Test
    fun `a feature does not import another feature's impl`() {
        productionFiles
            .filter { it.path.contains("/feature/") }
            .assertFalse { file ->
                val ownFeature = file.path.substringAfter("/feature/").substringBefore("/")
                file.imports.any { import ->
                    import.name.startsWith("$ROOT.feature.") &&
                        import.name.contains(".impl") &&
                        !import.name.startsWith("$ROOT.feature.$ownFeature.")
                }
            }
    }

    @Test
    fun `only core data drone knows MAVSDK`() {
        productionFiles
            .filterNot { it.path.contains("/core/data/drone/") }
            .assertFalse { file -> file.imports.any { it.name.startsWith("io.mavsdk") } }
    }

    @Test
    fun `domain and model are pure kotlin`() {
        productionFiles
            .filter { it.path.contains("/core/domain/") || it.path.contains("/core/model/") }
            .assertFalse { file -> file.imports.any { it.name.startsWith("android") } }
    }

    private companion object {
        const val ROOT = "io.github.hoonjaegui.px4gcs"
    }
}
