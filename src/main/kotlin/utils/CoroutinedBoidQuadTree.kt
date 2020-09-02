package utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import simulation.Boid

class CoroutinedBoidQuadTree(private val bounds: Rectangle, maxRegionCapacity: Int = 64) {

    enum class Task {
        MOVE,
        REMOVE,
        ADD
    }

    data class TreeWithTasks(
        val tree: QuadTree<Boid>,
        val tasks: MutableList<Pair<Task, Boid>> = mutableListOf()
    )

    private val treesWithTasks: List<TreeWithTasks>

    val children
        get() = treesWithTasks.map { it.tree }

    init {
        val boundsQuadrant = bounds.scaled(0.5, 0.5)

        val quadrantNE = boundsQuadrant.moved(Vector2(boundsQuadrant.width, 0.0))
        val quadrantSE = boundsQuadrant.moved(Vector2(boundsQuadrant.width, boundsQuadrant.height))
        val quadrantSW = boundsQuadrant.moved(Vector2(0.0, boundsQuadrant.height))

        treesWithTasks = listOf(
            TreeWithTasks(QuadTree(boundsQuadrant, maxRegionCapacity)),
            TreeWithTasks(QuadTree(quadrantNE, maxRegionCapacity)),
            TreeWithTasks(QuadTree(quadrantSE, maxRegionCapacity)),
            TreeWithTasks(QuadTree(quadrantSW, maxRegionCapacity))
        )
    }

    fun addBoids(boids: List<Boid>) {
        for (boid in boids) {
            // Outside of the root bounds, ignore
            if (boid.position !in bounds) continue

            for (treeWithTasks in treesWithTasks) {
                if (boid.position in treeWithTasks.tree.bounds) {
                    treeWithTasks.tasks.add(Pair(Task.ADD, boid))
                    break
                }
            }
        }
        processTasks()
    }

    fun moveBoids(boids: List<Boid>) {
        for (boid in boids) {
            // The position hasn't actually changed, ignore
            if (boid.position == boid.oldPosition) continue

            val nowInRootTreeBounds = boid.position in bounds
            val beforeInRootTreeBounds = boid.oldPosition in bounds

            // Moving while outside of the root bounds, ignore
            if (!nowInRootTreeBounds && !beforeInRootTreeBounds) continue

            when {
                // Went outside of the root bounds, only need to remove from specific quad
                !nowInRootTreeBounds -> {
                    for (treeWithTasks in treesWithTasks) {
                        if (boid.oldPosition in treeWithTasks.tree.bounds) {
                            treeWithTasks.tasks.add(Pair(Task.REMOVE, boid))
                            break
                        }
                    }
                }
                // Came from outside of the root bounds, only need to add to specific quad
                !beforeInRootTreeBounds -> {
                    for (treeWithTasks in treesWithTasks) {
                        if (boid.position in treeWithTasks.tree.bounds) {
                            treeWithTasks.tasks.add(Pair(Task.ADD, boid))
                            break
                        }
                    }
                }
                // Move inside one of the trees or between two of them
                else -> {
                    var removed = false
                    var added = false

                    for (treeWithTasks in treesWithTasks) {
                        val nowInTreeBounds = boid.position in treeWithTasks.tree.bounds
                        val oldInTreeBounds = boid.oldPosition in treeWithTasks.tree.bounds

                        // Nothing to do with this tree
                        if (!nowInTreeBounds && !oldInTreeBounds) continue

                        when {
                            nowInTreeBounds && oldInTreeBounds -> {
                                treeWithTasks.tasks.add(Pair(Task.MOVE, boid))
                                removed = true
                                added = true
                            }
                            oldInTreeBounds -> {
                                treeWithTasks.tasks.add(Pair(Task.REMOVE, boid))
                                removed = true
                            }
                            nowInTreeBounds -> {
                                treeWithTasks.tasks.add(Pair(Task.ADD, boid))
                                added = true
                            }
                        }

                        if (removed && added) break
                    }
                }
            }
        }
        processTasks()
    }

    private fun processTasks() {
        runBlocking {
            for (treeWithTask in treesWithTasks) {
                launch {
                    treeWithTask.runTasks()
                }
            }
        }

        // Clear the tasks
        for (treeWithTask in treesWithTasks) {
            treeWithTask.tasks.clear()
        }
    }

    private suspend fun TreeWithTasks.runTasks() = withContext(Dispatchers.Default) {
        for (task in tasks) {
            when (task.first) {
                Task.MOVE -> tree.move(
                    task.second.position,
                    task.second.oldPosition,
                    task.second
                )
                Task.REMOVE -> tree.remove(
                    task.second.oldPosition,
                    task.second
                )
                Task.ADD -> tree.add(
                    task.second.position,
                    task.second
                )
            }
        }
    }

    fun queryRange(rectangle: Rectangle): List<Boid> {
        return treesWithTasks.map { it.tree }.map { it.queryRange(rectangle) }.flatten()
    }
}