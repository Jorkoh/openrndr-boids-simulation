import org.openrndr.Program
import org.openrndr.color.ColorHSVa
import org.openrndr.color.ColorRGBa
import org.openrndr.color.ColorXSLa
import org.openrndr.extra.compositor.*
import org.openrndr.extra.fx.blur.*
import org.openrndr.extra.fx.color.ChromaticAberration
import org.openrndr.extra.fx.distort.Perturb
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.noise.simplex
import org.openrndr.extra.parameters.OptionParameter
import org.openrndr.math.Matrix44
import org.openrndr.math.transforms.rotateZ
import org.openrndr.math.transforms.scale
import org.openrndr.math.transforms.translate
import org.openrndr.shape.Circle
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import simulation.Agent
import simulation.Boid
import simulation.Predator
import simulation.Simulation
import java.lang.Math.PI
import kotlin.math.cos

object SimulationRenderer {
    object Settings {
        enum class CompositionType {
            Minimal,
            Night,
            Colorful
        }

        @OptionParameter("Composition")
        var activeCompositionType = CompositionType.Night
            set(value) {
                if (value == field) return
                activeComposition = when (value) {
                    CompositionType.Minimal -> program.minimalComposition()
                    CompositionType.Night -> program.nightComposition()
                    CompositionType.Colorful -> program.colorfulComposition()
                }
                field = value
            }
    }

    private lateinit var program: Program
    private lateinit var gui: GUI

    lateinit var activeComposition: Composite
        private set


    fun init(program: Program, gui: GUI) {
        this.program = program
        this.gui = gui
        activeComposition = program.minimalComposition()
    }

    // TODO add the region algorithm and showcase it on this composition
    // TODO add a way to change number of boids on the fly to showcase this composition with more boids
    fun Program.minimalComposition() = compose {
        draw {
            drawer.fill = ColorRGBa.WHITE
            drawer.stroke = null
            val boidCircles = Simulation.boids.map { boid -> Circle(boid.position, 6.0) }
            drawer.circles(boidCircles)

            drawer.fill = ColorRGBa.RED
            drawer.stroke = null
            val predatorCircles = Simulation.predators.map { predator -> Circle(predator.position, 15.0) }
            drawer.circles(predatorCircles)

            Simulation.selectedAgent?.let { agent ->
                drawer.fill = null
                drawer.stroke = ColorRGBa.GRAY
                when (agent) {
                    is Boid -> drawer.circle(agent.position, Boid.PERCEPTION_RADIUS)
                    is Predator -> drawer.circle(agent.position, Predator.PERCEPTION_RADIUS)
                }

                drawer.strokeWeight = 3.0
                agent.forces.forEachIndexed { index, force ->
                    drawer.stroke = ColorHSVa(360 * (index / agent.forces.size.toDouble()), 1.0, 0.5).toRGBa()
                    drawer.lineSegment(agent.position, agent.position + force * 20.0)
                }
            }
        }
    }

    fun Program.nightComposition() = compose {
        // Background layer
        layer {
            draw {
                drawer.fill = ColorRGBa.WHITE
                val resolution = 8
                val points = mutableListOf<Circle>()
                for (y in 0 until height / resolution) {
                    for (x in 0 until width / resolution) {
                        val xDouble = x.toDouble()
                        val yDouble = y.toDouble()

                        val simplex = simplex(100, xDouble + seconds, yDouble + seconds)
                        if (simplex > 0.712) {
                            points.add(Circle(xDouble * resolution, yDouble * resolution, 3.0))
                        }
                    }
                }
                drawer.circles(points)
            }
            post(FrameBlur().apply { blend = 0.1 })
        }
        // Boids layer
        layer {
            draw {
                drawer.fill = ColorRGBa.WHITE
                drawer.stroke = null
                val boidShapes = Simulation.boids.map { boid ->
                    fishShape.toAgentPositionAndRotation(boid)
                }
                drawer.contours(boidShapes)
            }
            post(GaussianBloom().apply { sigma = 2.0 }, { gain = cos(seconds * 0.8 * PI) * 2.0 + 2.0 })
            post(ChromaticAberration(), { aberrationFactor = cos(seconds * 0.8 * 0.5 * PI) * 4.0 })
        }
        // Predators layer
        layer {
            draw {
                drawer.fill = ColorRGBa.RED
                drawer.stroke = null
                val predatorShapes = Simulation.predators.map { predator ->
                    sharkShape.toAgentPositionAndRotation(predator)
                }
                drawer.contours(predatorShapes)
            }
            post(
                GaussianBloom().apply { sigma = 2.0 }, { gain = cos(seconds * 0.8 * PI + PI) * 8.0 + 2.0 })
        }
        // Underwater kind of effect
        post(Perturb().apply {
            scale = 6.0
            gain = 0.005
            decay = 0.0
        }, { phase = (seconds * 0.02) % 4 - 2 })
    }

    fun Program.colorfulComposition() = compose {
        layer {
            draw {
                drawer.stroke = null
                Simulation.boids.forEachIndexed { index, boid ->
                    drawer.fill = ColorXSLa(360 * (index.toDouble() / Simulation.boids.size), 0.85, 0.4, 1.0).toRGBa()
                    drawer.contour(fishShape.toAgentPositionAndRotation(boid))
                }
            }
            post(GaussianBloom().apply { gain = 1.0 })
            post(FrameBlur().apply { blend = 0.4 })
        }
        layer {
            draw {
                drawer.fill = ColorRGBa.WHITE
                drawer.stroke = null
                val predatorShapes = Simulation.predators.map { predator ->
                    sharkShape.toAgentPositionAndRotation(predator)
                }
                drawer.contours(predatorShapes)
            }
            post(GaussianBloom().apply { gain = 7.0 })
            post(HashBlur().apply { radius = 1.0 })
            post(FrameBlur().apply { blend = 0.08 })
        }
    }

    private const val fishShapeScape = 6.0
    private val fishShape = contour {
        moveTo(-1.5, 0.0)
        lineTo(0.4, -0.3)
        lineTo(0.6, 0.0)
        lineTo(0.4, 0.3)
        close()
    }.transform(Matrix44.scale(fishShapeScape, fishShapeScape, 0.0))

    // TODO change this to an actual shark shape
    private const val sharkShapeScale = 20.0
    private val sharkShape = contour {
        moveTo(-1.5, 0.0)
        lineTo(-0.8, 0.0)
        lineTo(0.4, -0.3)
        lineTo(0.6, 0.0)
        lineTo(0.4, 0.3)
        lineTo(-0.8, 0.0)
        close()
    }.transform(Matrix44.scale(sharkShapeScale, sharkShapeScale, 0.0))

    private fun ShapeContour.toAgentPositionAndRotation(agent: Agent) =
        transform(
            Matrix44.translate(agent.position.x, agent.position.y, 0.0)
                    * Matrix44.rotateZ(agent.velocity.angle)
        )
}