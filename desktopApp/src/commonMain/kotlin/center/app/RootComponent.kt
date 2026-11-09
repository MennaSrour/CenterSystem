package center.app

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.replaceCurrent

enum class Destination { Dashboard, Financials, Reception, Billing, Students, Courses, Reports }

class RootComponent(
    context: ComponentContext,
) : ComponentContext by context {
    private val navigation = StackNavigation<Destination>()
    val stack =
        childStack(
            source = navigation,
            serializer = null,
            initialConfiguration = Destination.Dashboard,
            childFactory = { destination, _ -> destination },
        )

    fun navigate(destination: Destination) {
        navigation.replaceCurrent(destination)
    }
}
