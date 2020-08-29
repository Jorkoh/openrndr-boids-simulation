package simulation

import angleDifference
import org.openrndr.math.Vector2
import kotlin.math.abs

interface Agent {
    val position: Vector2
    val velocity: Vector2
    val forces: List<Vector2>

    fun interact(agents: List<Agent>)
    fun move()

    // TODO Optimize this with region algorithm
    fun List<Agent>.visibleToAgent(perceptionRadius: Double, perceptionConeDegrees: Double) = filter { agent ->
        agent != this@Agent && agent.position.distanceTo(position) <= perceptionRadius
                && abs(velocity.angleDifference(agent.position - position)) <= perceptionConeDegrees / 2f
    }
}