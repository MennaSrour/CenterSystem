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
}
