package simulation

import Vector2withAngleCache
import angleDifference
import org.openrndr.math.Vector2
import kotlin.math.abs

interface Agent {
    var position: Vector2
    var velocity: Vector2withAngleCache
    var forces: MutableList<Vector2>

    fun interact(sameSpecies: List<Agent>, differentSpecies : List<Agent>)
    fun move(){
        position += velocity.vector
    }

    // TODO Optimize this with region algorithm
    fun List<Agent>.visibleToAgent(perceptionRadius: Double, perceptionConeDegrees: Double): List<Agent> {
        val squaredPerceptionRadius = perceptionRadius * perceptionRadius
        return filter { agent ->
            agent != this@Agent && agent.position.squaredDistanceTo(position) <= squaredPerceptionRadius
                    && (perceptionConeDegrees == 360.0
                    || abs(velocity.vector.angleDifference(agent.position - position)) <= perceptionConeDegrees / 2f)
        }
    }
}