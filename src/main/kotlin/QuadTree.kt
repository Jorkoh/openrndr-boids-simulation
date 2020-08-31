import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.intersects

class QuadTree<T>(val bounds: Rectangle, val maxRegionCapacity: Int = 4) {

    data class QuadTreeEntry<T>(var position: Vector2, val item: T)

    var children: List<QuadTree<T>> = emptyList()
    var entries: MutableList<QuadTreeEntry<T>> = mutableListOf()
    var size = 0; private set

    fun queryRange(rectangle: Rectangle): List<T> {
        return when {
            !intersects(bounds, rectangle) -> emptyList()
            children.isEmpty() -> entries.filter { it.position in rectangle }.map { it.item }
            else -> children.map { it.queryRange(rectangle) }.flatten()
        }
    }

    fun add(position: Vector2, item: T): Boolean {
        return when {
            position !in bounds -> false
            children.isNotEmpty() -> children.any { it.add(position, item) }
            entries.size < maxRegionCapacity -> entries.add(QuadTreeEntry(position, item))
            else -> {
                subdivide()
                children.any { it.add(position, item) }
            }
        }.also { addSucceeded ->
            if (addSucceeded) {
                size++
            }
        }
    }


    fun remove(position: Vector2, item: T): Boolean {
        return when {
            position !in bounds -> false
            children.isEmpty() -> entries.remove(QuadTreeEntry(position, item))
            else -> {
                val removed = children.find { position in it.bounds }?.remove(position, item) ?: false
                if (removed) {
                    if (size - 1 < maxRegionCapacity) {
                        entries = children.flatMap { it.entries }.toMutableList()
                        children = emptyList()
                    }
                }
                removed
            }
        }.also { removed ->
            if (removed) {
                size--
            }
        }
    }

    private fun subdivide() {
        val childWith = bounds.width / 2
        val childHeight = bounds.height / 2
        children = listOf(
            QuadTree(Rectangle(bounds.x, bounds.y, childWith, childHeight), maxRegionCapacity),
            QuadTree(Rectangle(bounds.x + childWith, bounds.y, childWith, childHeight), maxRegionCapacity),
            QuadTree(Rectangle(bounds.x, bounds.y + childHeight, childWith, childHeight), maxRegionCapacity),
            QuadTree(Rectangle(bounds.x + childWith, bounds.y + childHeight, childWith, childHeight), maxRegionCapacity)
        )
        entries.forEach { entry -> children.any { child -> child.add(entry.position, entry.item) } }
        entries.clear()
    }


    enum class MoveResult {
        MOVED_INSIDE_QUAD,
        CHANGED_QUAD,
        NOT_FOUND
    }


    fun move(newPosition: Vector2, oldPosition: Vector2, item: T) {
        // Didn't actually move
        if (newPosition == oldPosition) return

        val nowInBounds = newPosition in bounds
        val beforeInBounds = oldPosition in bounds

        when {
            // Moving outside of the quad bounds, ignore
            !nowInBounds && !beforeInBounds -> return
            // Went outside of the tree bounds, remove
            !nowInBounds -> remove(oldPosition, item)
            // Came from outside of the tree bounds, add
            !beforeInBounds -> add(newPosition, item)
            else -> {
                if (moveEntry(newPosition, oldPosition, item) == MoveResult.CHANGED_QUAD) {
                    remove(oldPosition, item)
                }
            }
        }
    }

    private fun moveEntry(newPosition: Vector2, oldPosition: Vector2, item: T): MoveResult {
        return when {
            newPosition !in bounds -> MoveResult.NOT_FOUND
            children.isEmpty() -> {
                val entry = entries.firstOrNull { it.position == oldPosition }
                if (entry != null) {
                    // It's here so move it
                    entry.position = newPosition
                    MoveResult.MOVED_INSIDE_QUAD
                } else {
                    // It's not here but it should be so add it
                    add(newPosition, item)
                    // Avoid increasing size twice when we increase sizes retroactively
                    size--
                    MoveResult.CHANGED_QUAD
                }
            }
            else -> {
                var resultFinal = MoveResult.NOT_FOUND
                for (child in children) {
                    val result = child.moveEntry(newPosition, oldPosition, item)
                    if (result != MoveResult.NOT_FOUND) {
                        resultFinal = result
                        break
                    }
                }
                resultFinal
            }
        }.also { result ->
            if (result == MoveResult.CHANGED_QUAD) {
                size++
            }
        }
    }

    fun contains(point: Vector2): Boolean {
        return when {
            point !in bounds -> false
            children.isEmpty() -> entries.any { it.position == point }
            else -> children.any { it.contains(point) }
        }
    }

    fun clear() {
        children = emptyList()
        entries.clear()
        size = 0
    }
}