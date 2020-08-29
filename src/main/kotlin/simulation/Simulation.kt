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
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import java.lang.Math.toDegrees
import kotlin.math.atan2

object Simulation {
    object Settings {
        const val BOIDS_AMOUNT = 1000
        const val PREDATOR_AMOUNT = 2
        const val AREA_WIDTH = 1600.0
        const val AREA_HEIGHT = 900.0
        const val AGENT_SPAWN_MARGIN = 100.0

        const val WALL_AVOIDANCE_FACTOR = 1e4
        const val PREDATOR_AVOIDANCE_FACTOR = 1e7
        const val BOID_CHASING_FACTOR = 3.0
        const val RIVAL_AVOIDANCE_FACTOR = 1e7

        @DoubleParameter("separation", 0.0, 5000.0)
        var SEPARATION_FACTOR = 500.0

        @DoubleParameter("alignment", 0.0, 10.0)
        var ALIGNMENT_FACTOR = 1.5

        @DoubleParameter("cohesion", 0.0, 1.0)
        var COHESION_FACTOR = 0.1
    }

    private val agents
        get() = boids + predators
    private val boids = mutableListOf<Boid>()
    private val predators = mutableListOf<Predator>()
    var selectedAgent: Agent? = null

    fun init() {
        // TODO Idea: boids could have random sizes and colors
        boids.clear()
        repeat(Settings.BOIDS_AMOUNT) {
            boids.add(Boid.createRandomBoid())
        }

        predators.clear()
        repeat(Settings.PREDATOR_AMOUNT) {
            predators.add(Predator.createRandomPredator())
        }
    }

    fun update() {
        boids.forEach { boid -> boid.interact(boids, predators) }
        predators.forEach { predator -> predator.interact(predators, boids) }
        agents.forEach { agent -> agent.move() }
    }

    fun getClosestAgent(position: Vector2) = agents.minBy { agent -> agent.position.distanceTo(position) }

    object Renderer {
        // TODO bug with drawer.contours() https://github.com/openrndr/openrndr/issues/157
        private const val FISH_SHAPE_SCALE = 6.0
        private val fishShape = contour {
            moveTo(-1.5, 0.0)
            lineTo(0.4, -0.3)
            lineTo(0.6, 0.0)
            lineTo(0.4, 0.3)
            lineTo(0.0, 0.0)
            close()
        }.transform(Matrix44.scale(FISH_SHAPE_SCALE, FISH_SHAPE_SCALE, 0.0))

        // TODO change this to an actual shark shape
        // TODO bug with drawer.contours() https://github.com/openrndr/openrndr/issues/157
        private const val SHARK_SHAPE_SCALE = 20.0
        private val sharkShape = contour {
            moveTo(-1.5, 0.0)
            lineTo(-0.8, 0.0)
            lineTo(0.4, -0.3)
            lineTo(0.6, 0.0)
            lineTo(0.4, 0.3)
            lineTo(-0.8, 0.0)
            lineTo(0.0, 0.0)
            close()
        }.transform(Matrix44.scale(SHARK_SHAPE_SCALE, SHARK_SHAPE_SCALE, 0.0))

        fun renderBoids(drawer: Drawer) {
            drawer.fill = ColorRGBa.WHITE
            drawer.stroke = null
            val boidsShapes = boids.map { boid -> fishShape.toAgentPositionAndRotation(boid) }
            drawer.contours(boidsShapes)
        }

        fun renderPredators(drawer: Drawer) {
            drawer.fill = ColorRGBa.RED
            drawer.stroke = null
            val predatorShapes = predators.map { predator -> sharkShape.toAgentPositionAndRotation(predator) }
            drawer.contours(predatorShapes)
        }

        fun renderDebug(drawer: Drawer) {
            selectedAgent?.let { agent ->
                drawer.fill = null
                drawer.stroke = ColorRGBa.GRAY
                when(agent){
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

        // TODO performance: angle of velocity is already calculated at least for the boids, could avoid this atan2
        private fun ShapeContour.toAgentPositionAndRotation(agent: Agent) =
            transform(
                Matrix44.translate(agent.position.x, agent.position.y, 0.0)
                        * Matrix44.rotateZ(toDegrees(atan2(agent.velocity.y, agent.velocity.x)))
            )
    }
}