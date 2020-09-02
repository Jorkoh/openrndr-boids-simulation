package simulation

import utils.CoroutinedBoidQuadTree
import utils.QuadTree
import utils.angleDifference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.math.abs

object Simulation {
    object Settings {
        const val BOIDS_AMOUNT = 1500
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

    // To be fair separating the move tasks by quads to parallelize doesn't really give a performance benefit
    // with boids amount <4000. Calculating the interactions in parallel does provide a real benefit
    val coroutinedBoidsQuad = CoroutinedBoidQuadTree(Rectangle(0.0, 0.0, Settings.AREA_WIDTH, Settings.AREA_HEIGHT), 64)

    private val agents
        get() = boids + predators
    val boids = mutableListOf<Boid>()
    val predators = mutableListOf<Predator>()

    var selectedAgent: Agent? = null

    init {
        repeat(Settings.BOIDS_AMOUNT) {
            boids.add(Boid.createRandomBoid())
        }
        repeat(Settings.PREDATOR_AMOUNT) {
            predators.add(Predator.createRandomPredator())
        }

        coroutinedBoidsQuad.addBoids(boids)
    }

    fun update() {
        coroutinedBoidsQuad.moveItems(boids)

        runBlocking {
            boids.forEach { boid ->
                launch(Dispatchers.Default) {
                    boid.interact(
                        coroutinedBoidsQuad.visibleToAgent(
                            boid,
                            Boid.PERCEPTION_RADIUS,
                            Boid.PERCEPTION_CONE_DEGREES
                        ),
                        predators.visibleToAgent(
                            boid,
                            Boid.PERCEPTION_RADIUS,
                            Boid.PERCEPTION_CONE_DEGREES
                        )
                    )
                }
            }
            predators.forEach { predator ->
                launch(Dispatchers.Default) {
                    predator.interact(
                        predators.visibleToAgent(
                            predator,
                            Predator.PERCEPTION_RADIUS,
                            Predator.PERCEPTION_CONE_DEGREES
                        ),
                        coroutinedBoidsQuad.visibleToAgent(
                            predator,
                            Predator.PERCEPTION_RADIUS,
                            Predator.PERCEPTION_CONE_DEGREES
                        )
                    )
                }
            }
        }

        agents.forEach { agent -> agent.move() }
    }

    private fun List<Agent>.visibleToAgent(agent: Agent, perceptionRadius: Double, perceptionConeDegrees: Double) =
        filter { otherAgent ->
            otherAgent != agent && otherAgent.position.squaredDistanceTo(agent.position) <= perceptionRadius * perceptionRadius
                    && (perceptionConeDegrees == 360.0
                    || abs(agent.velocity.vector.angleDifference(otherAgent.position - agent.position)) <= perceptionConeDegrees / 2f)
        }

    private fun QuadTree<Boid>.visibleToAgent(agent: Agent, perceptionRadius: Double, perceptionConeDegrees: Double) =
        queryRange(
            Rectangle(
                agent.position.x - perceptionRadius,
                agent.position.y - perceptionRadius,
                perceptionRadius * 2,
                perceptionRadius * 2
            )
        ).filter { boid ->
            boid != agent && boid.position.squaredDistanceTo(agent.position) <= perceptionRadius * perceptionRadius
                    && (perceptionConeDegrees == 360.0
                    || abs(agent.velocity.vector.angleDifference(boid.position - agent.position)) <= perceptionConeDegrees / 2f)
        }

    private fun CoroutinedBoidQuadTree.visibleToAgent(
        agent: Agent,
        perceptionRadius: Double,
        perceptionConeDegrees: Double
    ) =
        queryRange(
            Rectangle(
                agent.position.x - perceptionRadius,
                agent.position.y - perceptionRadius,
                perceptionRadius * 2,
                perceptionRadius * 2
            )
        ).filter { boid ->
            boid != agent && boid.position.squaredDistanceTo(agent.position) <= perceptionRadius * perceptionRadius
                    && (perceptionConeDegrees == 360.0
                    || abs(agent.velocity.vector.angleDifference(boid.position - agent.position)) <= perceptionConeDegrees / 2f)
        }

    fun getClosestAgent(position: Vector2) = agents.minBy { agent -> agent.position.distanceTo(position) }
}