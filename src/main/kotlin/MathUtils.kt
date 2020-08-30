import org.openrndr.math.Vector2
import java.lang.Math.toDegrees
import java.lang.Math.toRadians
import kotlin.math.*

// TODO way too many radians/angle transformations, it's cheap but not free
// TODO floats are not used (because openrndr uses Double) but could be used
fun Vector2.Companion.unitWithAngle(angle: Double): Vector2 {
    val theta = toRadians(angle)
    return Vector2(cos(theta), sin(theta))
}

fun Vector2.crs(secondVector: Vector2) = this.x * secondVector.y - this.y * secondVector.x

fun Vector2.angle() = toDegrees(atan2(y, x))

fun Vector2.setAngle(newAngle: Double): Vector2 {
    val radians = toRadians(newAngle)
    return Vector2(length * cos(radians), length * sin(radians))
}

fun Vector2.setLength(newLength: Double): Vector2 {
    return this * sqrt(newLength * newLength / squaredLength)
}

// https://math.stackexchange.com/a/1649850
fun Double.angleDifference(secondAngle: Double): Double {
    return (this - secondAngle + 540) % 360 - 180
}

fun Vector2.angleDifference(secondVector: Vector2) =
    toDegrees(atan2(this.crs(secondVector), dot(secondVector)))

fun Vector2.clampAngleChange(secondVector: Vector2, maxAngleChange: Double): Vector2 {
    val previousVectorAngle = secondVector.angle()
    val turnRate = angle().angleDifference(previousVectorAngle)
    return when {
        turnRate > maxAngleChange -> {
            setAngle(previousVectorAngle + maxAngleChange)
        }
        turnRate < -maxAngleChange -> {
            setAngle(previousVectorAngle - maxAngleChange)
        }
        else -> this
    }
}

fun Vector2.clampLength(min: Double, max: Double): Vector2 {
    val squaredMax = max * max
    val squaredMin = min * min

    return when {
        squaredLength == 0.0 -> this
        squaredLength > squaredMax -> this * (sqrt(squaredMax / squaredLength))
        squaredLength < squaredMin -> this * (sqrt(squaredMin / squaredLength))
        else -> this
    }
}

data class Vector2WithCachedAngle(val x: Double, val y: Double) {
    var vector = Vector2(x, y)
        set(value) {
            field = value
            angleCache = null
        }

    private var angleCache: Double? = null
    val angle: Double
        get() {
            if (angleCache == null) {
                angleCache = vector.angle()
            }
            // TODO fix this with proper nullability here
            return angleCache!!
        }
}