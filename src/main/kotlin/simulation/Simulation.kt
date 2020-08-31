package simulation

import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.math.Vector2

object Simulation {
    object Settings {
        const val BOIDS_AMOUNT = 1000
        const val PREDATOR_AMOUNT = 2
        const val AREA_WIDTH = 1600.0
        const val AREA_HEIGHT = 900.0
        const val AGENT_SPAWN_MARGIN = 100.0

        const val WALL_AVOIDANCE_FACTOR = 5e3
        const val PREDATOR_AVOIDANCE_FACTOR = 5e6
        const val BOID_CHASING_FACTOR = 1.5
        const val RIVAL_AVOIDANCE_FACTOR = 5e6

        @DoubleParameter("separation", 0.0, 2500.0)
        var SEPARATION_FACTOR = 250.0

        @DoubleParameter("alignment", 0.0, 5.0)
        var ALIGNMENT_FACTOR = 0.75

        @DoubleParameter("cohesion", 0.0, 0.5)
        var COHESION_FACTOR = 0.05
    }

    private val agents
        get() = boids + predators
    val boids = mutableListOf<Boid>()
    val predators = mutableListOf<Predator>()
    var selectedAgent: Agent? = null

    fun init() {
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
}