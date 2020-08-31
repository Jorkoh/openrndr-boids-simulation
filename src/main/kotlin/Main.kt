import org.openrndr.Application
import org.openrndr.Fullscreen
import org.openrndr.application
import org.openrndr.extra.gui.GUI
import org.openrndr.ffmpeg.ScreenRecorder
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
        var numFrames = 0
        var secondsLastPrint = 0.0

        // Declare a gui for changing settings on the fly
        val gui = GUI().apply {
            compartmentsCollapsedByDefault = false
            add(Simulation.Settings, "Simulation settings")
            add(SimulationRenderer.Settings, "Rendering settings")
        }

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

        // Install the gui
        extend(gui)

        // (Optional) Install a screen recorder to get a video
//        extend(ScreenRecorder().apply { frameRate = 60 })

        // Install the rendering loop
        extend {
            Simulation.update()
            SimulationRenderer.activeComposition.draw(drawer)

            // (Optional) for performance checks
            numFrames++
            if (numFrames % 375 == 0) {
                println("FPS: ${numFrames / (seconds - secondsLastPrint)}")
                numFrames = 0
                secondsLastPrint = seconds
            }
        }
    }
}