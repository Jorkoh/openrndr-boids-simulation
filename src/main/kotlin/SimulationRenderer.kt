import org.openrndr.Program
import org.openrndr.color.ColorHSVa
import org.openrndr.color.ColorRGBa
import org.openrndr.color.ColorXSLa
import org.openrndr.color.mix
import org.openrndr.draw.Drawer
import org.openrndr.extra.compositor.*
import org.openrndr.extra.fx.blur.FrameBlur
import org.openrndr.extra.fx.blur.GaussianBloom
import org.openrndr.extra.fx.blur.HashBlur
import org.openrndr.extra.fx.color.ChromaticAberration
import org.openrndr.extra.fx.distort.Perturb
import org.openrndr.extra.noise.simplex
import org.openrndr.extra.parameters.OptionParameter
import org.openrndr.math.Matrix44
import org.openrndr.math.transforms.rotateZ
import org.openrndr.math.transforms.scale
import org.openrndr.math.transforms.translate
import org.openrndr.shape.Circle
import org.openrndr.shape.Segment
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import simulation.Agent
import simulation.Boid
import simulation.Predator
import simulation.Simulation
import utils.QuadTree
import java.lang.Math.PI
import kotlin.math.cos

object SimulationRenderer {
    object Settings {
        enum class CompositionType {
            Debug,
            Night,
            Colorful
        }

        @OptionParameter("Composition")
        var activeCompositionType = CompositionType.Debug
            set(value) {
                if (value == field) return
                activeComposition = when (value) {
                    CompositionType.Debug -> program.debugComposition()
                    CompositionType.Night -> program.nightComposition()
                    CompositionType.Colorful -> program.colorfulComposition()
                }
                field = value
            }
    }

    private lateinit var program: Program

    lateinit var activeComposition: Composite
        private set


    fun init(program: Program) {
        this.program = program
        activeComposition = program.debugComposition()
    }

    fun Program.debugComposition() = compose {
        draw {
            drawer.clear(ColorRGBa.WHITE)

            // Quad tree quads
            drawer.fill = null
            drawer.stroke = ColorRGBa.BLACK
            drawer.strokeWeight = 0.4
            Simulation.coroutinedBoidsQuad.children.draw(drawer)

            // Selected agent
            Simulation.selectedAgent?.let { agent ->
                drawer.stroke = null
                drawer.fill = ColorRGBa.GRAY.opacify(0.8)
                drawer.strokeWeight = 0.0
                when (agent) {
                    is Boid -> drawer.circle(agent.position, Boid.PERCEPTION_RADIUS)
                    is Predator -> drawer.circle(agent.position, Predator.PERCEPTION_RADIUS)
                }

                drawer.strokeWeight = 4.0
                agent.forces.forEachIndexed { index, force ->
                    drawer.stroke = ColorHSVa(360 * (index / agent.forces.size.toDouble()), 1.0, 1.0).toRGBa()
                    drawer.lineSegment(agent.position, agent.position + force * 60.0)
                }
            }

            // Boids
            val boidBodies = Simulation.boids.map { boid -> Circle(boid.position, 5.0) }
            val boidVelocities = Simulation.boids.map { boid ->
                Segment(boid.position, boid.position + boid.velocity.vector * 4.0)
            }
            drawer.fill = ColorRGBa.BLACK
            drawer.stroke = null
            drawer.strokeWeight = 0.0
            drawer.circles(boidBodies)

            drawer.fill = null
            drawer.stroke = ColorRGBa.BLACK
            drawer.strokeWeight = 1.0
            drawer.segments(boidVelocities)

            // Predators
            val predatorBodies = Simulation.predators.map { predator -> Circle(predator.position, 15.0) }
            val predatorVelocities = Simulation.predators.map { predator ->
                Segment(predator.position, predator.position + predator.velocity.vector * 10.0)
            }
            drawer.fill = ColorRGBa.BLACK
            drawer.stroke = null
            drawer.strokeWeight = 0.0
            drawer.circles(predatorBodies)

            drawer.fill = null
            drawer.stroke = ColorRGBa.BLACK
            drawer.strokeWeight = 1.0
            drawer.segments(predatorVelocities)
        }
    }

    private fun List<QuadTree<Boid>>.draw(drawer: Drawer) {
        forEach { child ->
            if (child.children.isNotEmpty()) {
                child.children.draw(drawer)
            } else {
                drawer.fill = mix(ColorRGBa.WHITE, ColorRGBa.RED, child.size / child.maxRegionCapacity.toDouble())
                drawer.rectangle(child.bounds)
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