import org.openrndr.application
import org.openrndr.extra.gui.GUI
import simulation.Simulation
import simulation.Simulation.Settings.AREA_HEIGHT
import simulation.Simulation.Settings.AREA_WIDTH

fun main() = application {
    configure {
        width = AREA_WIDTH.toInt()
        height = AREA_HEIGHT.toInt()
        windowResizable = false
    }

    program {
        // (Optional) for performance checks
//        var secondsLastPrint = 0.0

        // Install a gui for changing settings on the fly
        extend(GUI().apply {
            compartmentsCollapsedByDefault = false
            add(Simulation.Settings, "Simulation settings")
            add(SimulationRenderer.Settings, "Rendering settings")
        })

        // Initialize the simulation renderer
        SimulationRenderer.init(this)

        // Add a mouse listener to highlight specific agents
        mouse.clicked.listen { mouseEvent ->
            if (Simulation.selectedAgent == null) {
                Simulation.selectedAgent = Simulation.getClosestAgent(mouseEvent.position)
            } else {
                Simulation.selectedAgent = null
            }
        }

        // (Optional) Install a screen recorder to get a video
//        extend(ScreenRecorder().apply { frameRate = 60 })

        // Install the rendering loop
        extend {
            Simulation.update()
            SimulationRenderer.activeComposition.draw(drawer)

            // (Optional) for performance checks
//            if (frameCount % 375 == 0) {
//                println("FPS: ${375 / (seconds - secondsLastPrint)}")
//                secondsLastPrint = seconds
//            }
        }
    }
}