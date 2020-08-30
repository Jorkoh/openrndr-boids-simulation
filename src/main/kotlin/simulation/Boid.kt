package simulation

import angle
import clampAngleChange
import clampLength
import org.openrndr.math.Vector2
import setLength
import simulation.Simulation.Settings.AGENT_SPAWN_MARGIN
import simulation.Simulation.Settings.ALIGNMENT_FACTOR
import simulation.Simulation.Settings.AREA_HEIGHT
import simulation.Simulation.Settings.AREA_WIDTH
import simulation.Simulation.Settings.COHESION_FACTOR
import simulation.Simulation.Settings.PREDATOR_AVOIDANCE_FACTOR
import simulation.Simulation.Settings.WALL_AVOIDANCE_FACTOR
import unitWithAngle
import kotlin.math.max
import kotlin.random.Random

class Boid(
    override var position: Vector2,
    override var velocity: Vector2,
    override var forces: MutableList<Vector2> = mutableListOf()
) : Agent {
    companion object {
        const val PERCEPTION_RADIUS = 60.0
        const val PERCEPTION_CONE_DEGREES = 360.0

        const val MAX_TURN_RATE = 15.0
        const val MINIMUM_SPEED = 1.0
        const val MAXIMUM_SPEED = 4.0

        fun createRandomBoid() = Boid(
            Vector2(
                Random.nextDouble(AGENT_SPAWN_MARGIN, (AREA_WIDTH - AGENT_SPAWN_MARGIN)),
                Random.nextDouble(AGENT_SPAWN_MARGIN, (AREA_HEIGHT - AGENT_SPAWN_MARGIN))
            ),
            Vector2.unitWithAngle(Random.nextDouble(0.0, 360.0))
                .setLength(Random.nextDouble(MINIMUM_SPEED, MAXIMUM_SPEED))
        )
    }

    override fun interact(sameSpecies: List<Agent>, differentSpecies: List<Agent>) {
        val visibleBoids = sameSpecies.visibleToAgent(PERCEPTION_RADIUS, PERCEPTION_CONE_DEGREES)
        val visiblePredators = differentSpecies.visibleToAgent(PERCEPTION_RADIUS, PERCEPTION_CONE_DEGREES)

        forces.clear()
        forces.add(wallAvoidanceForce())
        if (visibleBoids.isNotEmpty()) {
            forces.add(separationRuleForce(visibleBoids))
            forces.add(alignmentRuleForce(visibleBoids))
            forces.add(cohesionRuleForce(visibleBoids))
        }
        forces.add(predatorAvoidanceForce(visiblePredators))
        // TODO repulsion from predators

        velocity = calculateNewVelocity()
    }

    private fun wallAvoidanceForce(): Vector2 {
        var force = Vector2.ZERO

        // Left wall
        val dstToLeft = max(position.x, 0.000001)
        force += Vector2(1.0, 0.0) * (1 / (dstToLeft * dstToLeft)) * WALL_AVOIDANCE_FACTOR
        // Right wall
        val dstToRight = max(AREA_WIDTH - position.x, 0.000001)
        force += Vector2(-1.0, 0.0) * (1 / (dstToRight * dstToRight)) * WALL_AVOIDANCE_FACTOR
        // Bottom wall
        val dstToBottom = max(position.y, 0.000001)
        force += Vector2(0.0, 1.0) * (1 / (dstToBottom * dstToBottom)) * WALL_AVOIDANCE_FACTOR
        // Top wall
        val dstToTop = max(AREA_HEIGHT - position.y, 0.000001)
        force += Vector2(0.0, -1.0) * (1 / (dstToTop * dstToTop)) * WALL_AVOIDANCE_FACTOR

        return force
    }

    private fun separationRuleForce(visibleBoids: List<Agent>): Vector2 {
        var force = Vector2.ZERO
        visibleBoids.forEach { otherAgent ->
            val positionDifference = position - otherAgent.position
            force += positionDifference.normalized() * (1 / positionDifference.squaredLength)
        }
        return force * Simulation.Settings.SEPARATION_FACTOR
    }

    private fun alignmentRuleForce(visibleBoids: List<Agent>): Vector2 {
        var force = Vector2.ZERO
        visibleBoids.forEach { otherAgent ->
            force += Vector2.unitWithAngle(otherAgent.velocity.angle())
        }
        return force.normalized() * ALIGNMENT_FACTOR
    }

    private fun cohesionRuleForce(visibleBoids: List<Agent>): Vector2 {
        var force = Vector2.ZERO
        visibleBoids.forEach { otherAgent ->
            force += otherAgent.position
        }
        return (force / visibleBoids.size.toDouble() - position) * COHESION_FACTOR
    }

    private fun predatorAvoidanceForce(visiblePredators: List<Agent>): Vector2 {
        var force = Vector2.ZERO
        visiblePredators.forEach { predator ->
            val positionDifference = position - predator.position
            force += positionDifference.normalized() * (1 / positionDifference.squaredLength)
        }
        return force * PREDATOR_AVOIDANCE_FACTOR
    }

    private fun calculateNewVelocity(): Vector2 {
        var newVelocity = velocity.copy()
        // Add the forces
        for (force in forces) {
            newVelocity += force
        }
        // Clamp the turn rate
        newVelocity = newVelocity.clampAngleChange(velocity, MAX_TURN_RATE)
        // Clamp the speed
        newVelocity = newVelocity.clampLength(MINIMUM_SPEED, MAXIMUM_SPEED)

        return newVelocity
    }
}