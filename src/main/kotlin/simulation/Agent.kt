package simulation

import Vector2withAngleCache
import org.openrndr.math.Vector2

interface Agent {
    var position: Vector2
    var velocity: Vector2withAngleCache
    var forces: MutableList<Vector2>

    fun interact(sameSpecies: List<Agent>, differentSpecies: List<Agent>)
    fun move() {
        position += velocity.vector
    }
}