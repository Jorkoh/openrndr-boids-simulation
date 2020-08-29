import org.openrndr.application
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blur.GaussianBloom
import org.openrndr.extra.fx.color.ChromaticAberration
import org.openrndr.extra.gui.GUI
import simulation.Simulation
import simulation.Simulation.Settings.AREA_HEIGHT
import simulation.Simulation.Settings.AREA_WIDTH
import java.lang.Math.PI
import kotlin.math.cos

fun main() = application {
    configure {
        width = AREA_WIDTH.toInt()
        height = AREA_HEIGHT.toInt()
        windowResizable = false
    }

    program {
        // (Optional) for performance checks
//        var numFrames = 0
//        var secondsLastPrint = 0.0

        // Initialize the simulation
        Simulation.init()

        // Declare a gui for changing settings on the fly
        val gui = GUI().apply {
            compartmentsCollapsedByDefault = false
            add(Simulation.Settings, "Simulation settings")
        }

        // Add a mouse listener to highlight specific agents
        mouse.clicked.listen { mouseEvent ->
            if (Simulation.selectedAgent == null) {
                Simulation.selectedAgent = Simulation.getClosestAgent(mouseEvent.position)
            } else {
                Simulation.selectedAgent = null
            }
        }

        // Declare the composition to render
        val composition = compose {
            draw {
                Simulation.Renderer.render(drawer)
            }
            post(GaussianBloom().apply { sigma = 2.0 }, { gain = cos(seconds * 0.8 * PI) * 2.0 + 2.0 })
            post(ChromaticAberration(), { aberrationFactor = cos(seconds * 0.8 * 0.5 * PI) * 4.0 })
        }

        // Install the gui
        extend(gui)

        // (Optional) Install a screen recorder to get a video
//        extend(ScreenRecorder())

        // Install the rendering loop
        extend {
            Simulation.update()
            composition.draw(drawer)

            // (Optional) for performance checks
//            numFrames++
//            if(numFrames % 375 == 0){
//                println("FPS: ${numFrames / (seconds - secondsLastPrint)}")
//                numFrames = 0
//                secondsLastPrint = seconds
//            }
        }
    }
}