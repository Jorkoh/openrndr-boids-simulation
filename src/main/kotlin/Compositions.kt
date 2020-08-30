import org.openrndr.Program
import org.openrndr.color.ColorHSVa
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blur.FrameBlur
import org.openrndr.extra.fx.blur.GaussianBloom
import org.openrndr.extra.fx.color.ChromaticAberration
import org.openrndr.extra.noise.simplex
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
import kotlin.math.PI
import kotlin.math.cos

fun Program.simplifiedComposition() = compose {
    draw {
        drawer.fill = ColorRGBa.WHITE
        drawer.stroke = null
        val boidCircles = Simulation.boids.map { boid -> Circle(boid.position, 6.0) }
        drawer.circles(boidCircles)

        drawer.fill = ColorRGBa.RED
        drawer.stroke = null
        val predatorCircles = Simulation.predators.map { predator -> Circle(predator.position, 15.0) }
        drawer.circles(predatorCircles)
    }
}

fun Program.standardComposition() = compose {
    val fishShapeScape = 6.0
    val fishShape = contour {
        moveTo(-1.5, 0.0)
        lineTo(0.4, -0.3)
        lineTo(0.6, 0.0)
        lineTo(0.4, 0.3)
        close()
    }.transform(Matrix44.scale(fishShapeScape, fishShapeScape, 0.0))

    // TODO change this to an actual shark shape
    val sharkShapeScale = 20.0
    val sharkShape = contour {
        moveTo(-1.5, 0.0)
        lineTo(-0.8, 0.0)
        lineTo(0.4, -0.3)
        lineTo(0.6, 0.0)
        lineTo(0.4, 0.3)
        lineTo(-0.8, 0.0)
        close()
    }.transform(Matrix44.scale(sharkShapeScale, sharkShapeScale, 0.0))

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
            val boidShapes = Simulation.boids.map { boid -> fishShape.toAgentPositionAndRotation(boid) }
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
            val predatorShapes = Simulation.predators.map { predator -> sharkShape.toAgentPositionAndRotation(predator) }
            drawer.contours(predatorShapes)
        }
        post(
            GaussianBloom().apply { sigma = 2.0 },
            { gain = cos(seconds * 0.8 * Math.PI + Math.PI) * 8.0 + 2.0 })
    }
    // Debug layer
    layer {
        draw {
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
}

private fun ShapeContour.toAgentPositionAndRotation(agent: Agent) =
    transform(
        Matrix44.translate(agent.position.x, agent.position.y, 0.0)
                * Matrix44.rotateZ(agent.velocity.angle)
    )