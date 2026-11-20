package center.app

import center.feature.financials.ClassOption
import center.feature.financials.StudentOption
import center.ui.components.MainNavigation
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.replaceCurrent

enum class Destination { Dashboard, Financials, Reception, Billing, Students, Courses, Reports, Teachers, Settings }

sealed interface ReceptionShortcut {
    data object AddStudent : ReceptionShortcut

    data class CollectSubscription(
        val studentCode: String? = null,
    ) : ReceptionShortcut

    data class PayOverdue(
        val student: StudentOption,
        val group: ClassOption,
        val remainingMinor: Long,
    ) : ReceptionShortcut

    data object RecordExpense : ReceptionShortcut
}

data class AppRoute(
    val destination: Destination,
    val shortcut: ReceptionShortcut? = null,
)

class RootComponent(
    context: ComponentContext,
) : ComponentContext by context {
    private val navigation = StackNavigation<AppRoute>()
    val stack =
        childStack(
            source = navigation,
            serializer = null,
            initialConfiguration = AppRoute(Destination.Dashboard),
            childFactory = { route, _ -> route.destination },
        )

    fun navigate(destination: Destination) {
        navigation.replaceCurrent(AppRoute(destination))
    }

    internal fun navigate(destination: MainNavigation) {
        navigate(
            when (destination) {
                MainNavigation.Reception -> Destination.Dashboard
                MainNavigation.Students -> Destination.Students
                MainNavigation.Teachers -> Destination.Teachers
                MainNavigation.Courses -> Destination.Courses
                MainNavigation.Treasury -> Destination.Financials
                MainNavigation.Settings -> Destination.Settings
            },
        )
    }

    internal fun open(shortcut: ReceptionShortcut) {
        val destination =
            when (shortcut) {
                ReceptionShortcut.AddStudent -> Destination.Students
                is ReceptionShortcut.CollectSubscription, is ReceptionShortcut.PayOverdue, ReceptionShortcut.RecordExpense ->
                    Destination.Financials
            }
        navigation.replaceCurrent(AppRoute(destination, shortcut))
    }
}
