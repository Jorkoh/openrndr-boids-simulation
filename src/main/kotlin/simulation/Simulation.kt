package simulation

import org.openrndr.color.ColorHSVa
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.rotateZ
import org.openrndr.math.transforms.scale
import org.openrndr.math.transforms.translate
import org.openrndr.shape.contour
import kotlin.math.atan2

object Simulation {
    private val agents = mutableListOf<Agent>()
    var selectedAgent: Agent? = null

    fun init() {
        agents.clear()
        repeat(500) {
            agents.add(Boid.createRandomBoid())
        }
    }

    fun update() {
        agents.forEach { agent -> agent.interact(agents) }
        agents.forEach { agent -> agent.move() }
    }

    fun getClosestAgent(position: Vector2) = agents.minBy { agent -> agent.position.distanceTo(position) }

    object Renderer {
        private const val FISH_SHAPE_SCALE = 6.0
        private val fishShape = contour {
            moveTo(-1.4, 0.0)
            lineTo(-0.8, 0.0)
            lineTo(0.4, -0.3)
            lineTo(0.6, 0.0)
            lineTo(0.4, 0.3)
            lineTo(-0.8, 0.0)
            close()
        }.transform(Matrix44.scale(FISH_SHAPE_SCALE, FISH_SHAPE_SCALE, 0.0))

        fun render(drawer: Drawer) {
            drawer.fill = ColorRGBa.WHITE
            drawer.stroke = null
            val agentShapes = agents.map { agent -> agent.getShape() }
            drawer.contours(agentShapes)

            selectedAgent?.let { agent ->
                drawer.fill = null
                drawer.stroke = ColorRGBa.GRAY
                drawer.circle(agent.position, Boid.PERCEPTION_RADIUS)

                drawer.strokeWeight = 3.0
                agent.forces.forEachIndexed { index, force ->
                    drawer.stroke = ColorHSVa(360 * (index / agent.forces.size.toDouble()), 1.0, 0.5).toRGBa()
                    drawer.lineSegment(agent.position, agent.position + force * 10.0)
                }
            }
        }
        
        private fun Agent.getShape() = fishShape.transform(
            Matrix44.translate(position.x, position.y, 0.0)
                    * Matrix44.rotateZ(Math.toDegrees(atan2(velocity.y, velocity.x)))
        )
    }

    object Settings {
        const val WALL_AVOIDANCE_FACTOR = 1e4

        @DoubleParameter("separation", 0.0, 5000.0)
        var SEPARATION_FACTOR = 500.0

        @DoubleParameter("alignment", 0.0, 10.0)
        var ALIGNMENT_FACTOR = 1.5

        @DoubleParameter("cohesion", 0.0, 1.0)
        var COHESION_FACTOR = 0.1
    }
}