package center.app

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlin.test.Test
import kotlin.test.assertEquals

class RootComponentTest {
    @Test
    fun switchesBetweenTypedDestinationsWithoutGrowingStack() {
        val root = RootComponent(DefaultComponentContext(LifecycleRegistry()))
        assertEquals(Destination.Dashboard, root.stack.value.active.instance)
        Destination.entries.forEach { destination ->
            root.navigate(destination)
            assertEquals(destination, root.stack.value.active.instance)
            assertEquals(0, root.stack.value.backStack.size)
        }
    }

    @Test
    fun receptionShortcutsCarryTheirInitialActionWithoutGrowingTheStack() {
        val root = RootComponent(DefaultComponentContext(LifecycleRegistry()))
        val payment = ReceptionShortcut.CollectSubscription("10234")
        root.open(payment)
        assertEquals(AppRoute(Destination.Financials, payment), root.stack.value.active.configuration)
        root.open(ReceptionShortcut.RecordExpense)
        assertEquals(AppRoute(Destination.Financials, ReceptionShortcut.RecordExpense), root.stack.value.active.configuration)
        root.open(ReceptionShortcut.AddStudent)
        assertEquals(AppRoute(Destination.Students, ReceptionShortcut.AddStudent), root.stack.value.active.configuration)
        root.navigate(Destination.Students)
        assertEquals(AppRoute(Destination.Students), root.stack.value.active.configuration)
        assertEquals(0, root.stack.value.backStack.size)
    }
}
