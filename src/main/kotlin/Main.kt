import org.openrndr.application
import org.openrndr.extra.gui.GUI
import simulation.Simulation

const val AREA_WIDTH = 1600.0
const val AREA_HEIGHT = 900.0
const val AGENT_SPAWN_MARGIN = 100.0

fun main() = application {
    configure {
        width = AREA_WIDTH.toInt()
        height = AREA_HEIGHT.toInt()
        windowResizable = false
    }

    program {
        Simulation.init()

        extend(GUI().apply {
            compartmentsCollapsedByDefault = false
            add(Simulation.Settings, "Settings")
        })

        mouse.clicked.listen { mouseEvent ->
            if (Simulation.selectedAgent == null) {
                Simulation.selectedAgent = Simulation.getClosestAgent(mouseEvent.position)
            } else {
                Simulation.selectedAgent = null
            }
        }

        extend {
            Simulation.update()
            Simulation.Renderer.render(drawer)
        }
    }
}