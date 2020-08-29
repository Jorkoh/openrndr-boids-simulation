package simulation

import AGENT_SPAWN_MARGIN
import AREA_HEIGHT
import AREA_WIDTH
import angle
import clampAngleChange
import clampLength
import org.openrndr.math.Vector2
import setLength
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

        const val MAX_TURN_RATE = 30.0
        const val MINIMUM_SPEED = 2.0
        const val MAXIMUM_SPEED = 8.0

        fun createRandomBoid() = Boid(
            Vector2(
                Random.nextDouble(AGENT_SPAWN_MARGIN, (AREA_WIDTH - AGENT_SPAWN_MARGIN)),
                Random.nextDouble(AGENT_SPAWN_MARGIN, (AREA_HEIGHT - AGENT_SPAWN_MARGIN))
            ),
            Vector2.unitWithAngle(Random.nextDouble(0.0, 360.0))
                .setLength(Random.nextDouble(MINIMUM_SPEED, MAXIMUM_SPEED))
        )
    }

    override fun interact(agents: List<Agent>) {
        forces.clear()

        forces.add(avoidWallsForce())

        val visibleAgents = agents.visibleToAgent(PERCEPTION_RADIUS, PERCEPTION_CONE_DEGREES)
        if (visibleAgents.isNotEmpty()) {
            forces.add(separationRuleForce(visibleAgents))
            forces.add(alignmentRuleForce(visibleAgents))
            forces.add(cohesionRuleForce(visibleAgents))
        }

        velocity = calculateNewVelocity()
    }

    private fun avoidWallsForce(): Vector2 {
        var force = Vector2.ZERO

        val dstToLeft = max(position.x, 0.000001)
        force += Vector2(1.0, 0.0) * (1 / (dstToLeft * dstToLeft)) * Simulation.Settings.WALL_AVOIDANCE_FACTOR
        val dstToRight = max(AREA_WIDTH - position.x, 0.000001)
        force += Vector2(-1.0, 0.0) * (1 / (dstToRight * dstToRight)) * Simulation.Settings.WALL_AVOIDANCE_FACTOR
        val dstToBottom = max(position.y, 0.000001)
        force += Vector2(0.0, 1.0) * (1 / (dstToBottom * dstToBottom)) * Simulation.Settings.WALL_AVOIDANCE_FACTOR
        val dstToTop = max(AREA_HEIGHT - position.y, 0.000001)
        force += Vector2(0.0, -1.0) * (1 / (dstToTop * dstToTop)) * Simulation.Settings.WALL_AVOIDANCE_FACTOR

        return force
    }

    private fun separationRuleForce(visibleAgents: List<Agent>): Vector2 {
        var separationForce = Vector2.ZERO
        visibleAgents.forEach { otherAgent ->
            val positionDifference = position - otherAgent.position
            val positionDistance = positionDifference.length
            separationForce += positionDifference.normalized() * (1 / (positionDistance * positionDistance))
        }
        return separationForce * Simulation.Settings.SEPARATION_FACTOR
    }

    private fun alignmentRuleForce(visibleAgents: List<Agent>): Vector2 {
        var alignmentForce = Vector2.ZERO
        visibleAgents.forEach { otherAgent ->
            alignmentForce += Vector2.unitWithAngle(otherAgent.velocity.angle())
        }
        return alignmentForce.normalized() * Simulation.Settings.ALIGNMENT_FACTOR
    }

    private fun cohesionRuleForce(visibleAgents: List<Agent>): Vector2 {
        var cohesionForce = Vector2.ZERO
        visibleAgents.forEach { otherAgent ->
            cohesionForce += otherAgent.position
        }
        return (cohesionForce / visibleAgents.size.toDouble() - position) * Simulation.Settings.COHESION_FACTOR
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

    override fun move() {
        position += velocity
    }
}