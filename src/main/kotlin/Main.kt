import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blur.FrameBlur
import org.openrndr.extra.fx.blur.GaussianBloom
import org.openrndr.extra.fx.color.ChromaticAberration
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.noise.simplex
import org.openrndr.math.Vector2
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
        var numFrames = 0
        var secondsLastPrint = 0.0

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
            layer {
                draw {
                    drawer.fill = ColorRGBa.WHITE

                    val resolution = 8
                    val points = mutableListOf<Vector2>()
                    for (y in 0 until height / resolution) {
                        for (x in 0 until width / resolution) {
                            val xDouble = x.toDouble()
                            val yDouble = y.toDouble()

                            val simplex = simplex(100, xDouble + seconds, yDouble + seconds)
                            if (simplex > 0.712) {
                                points.add(Vector2(xDouble * resolution, yDouble * resolution))
                            }
                        }
                    }
                    drawer.points(points)
                }
                post(FrameBlur().apply { blend = 0.1 })
            }
            layer {
                draw {
                    Simulation.Renderer.renderBoids(drawer)
                }
                post(GaussianBloom().apply { sigma = 2.0 }, { gain = cos(seconds * 0.8 * PI) * 2.0 + 2.0 })
                post(ChromaticAberration(), { aberrationFactor = cos(seconds * 0.8 * 0.5 * PI) * 4.0 })
            }
            layer {
                draw {
                    Simulation.Renderer.renderPredators(drawer)
                }
                post(GaussianBloom().apply { sigma = 2.0 }, { gain = cos(seconds * 0.8 * PI + PI) * 8.0 + 2.0 })
            }
            layer {
                draw {
                    Simulation.Renderer.renderDebug(drawer)
                }
            }
        }

        // Install the gui
        extend(gui)

        // (Optional) Install a screen recorder to get a video
//        extend(ScreenRecorder().apply { frameRate = 60 })

        // Install the rendering loop
        extend {
            Simulation.update()
//            composition.draw(drawer)

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