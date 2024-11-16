package engine.physics.collision;

import org.joml.Vector2f;

/**
 * Rectangle bounds for quadtree regions and entities
 */
public class Bounds {
    public float x, y;           // Center position
    public float halfWidth;      // Half-width for efficient containment tests
    public float halfHeight;     // Half-height for efficient containment tests

    public Bounds(float x, float y, float halfWidth, float halfHeight) {
        this.x = x;
        this.y = y;
        this.halfWidth = halfWidth;
        this.halfHeight = halfHeight;
    }

    public boolean contains(Vector2f point) {
        return point.x >= x - halfWidth && point.x <= x + halfWidth &&
                point.y >= y - halfHeight && point.y <= y + halfHeight;
    }

    public boolean intersects(Bounds other) {
        return Math.abs(x - other.x) <= (halfWidth + other.halfWidth) &&
                Math.abs(y - other.y) <= (halfHeight + other.halfHeight);
    }

    public boolean containsCompletely(Bounds other) {
        return Math.abs(x - other.x) + other.halfWidth <= halfWidth &&
                Math.abs(y - other.y) + other.halfHeight <= halfHeight;
    }
}
