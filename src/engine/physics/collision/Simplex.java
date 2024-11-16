package engine.physics.collision;

import org.joml.Vector2f;
import java.util.ArrayList;
import java.util.List;

/**
 * The Simplex class represents a shape used in the GJK algorithm to determine if two convex shapes intersect.
 * In 2D, a simplex is a geometric shape that can be:
 * - A point (1 vertex)
 * - A line segment (2 vertices)
 * - A triangle (3 vertices)
 *
 * The GJK algorithm builds this simplex incrementally, attempting to enclose the origin.
 * If the simplex can enclose the origin, the shapes are intersecting.
 */
public class Simplex {
    // Store up to 3 points (vertices) for 2D collision detection
    private final List<Vector2f> points;

    public Simplex() {
        // ArrayList is fine since we'll never have more than 3 points
        this.points = new ArrayList<>(3);
    }

    /**
     * Adds a new point to the front of the simplex.
     * We add to the front because GJK always uses the most recently added point first.
     *
     * @param point The new support point to add
     */
    public void add(Vector2f point) {
        points.add(0, new Vector2f(point));
    }

    /**
     * Gets a point from the simplex.
     * Index 0 is always the most recently added point.
     *
     * @param index The index of the point to get
     * @return The point at the specified index
     */
    public Vector2f get(int index) {
        return new Vector2f(points.get(index));
    }

    /**
     * Gets the number of points currently in the simplex.
     * In 2D GJK, this will be 1, 2, or 3.
     *
     * @return The number of points in the simplex
     */
    public int size() {
        return points.size();
    }

    /**
     * Removes a point from the simplex.
     * Used when we need to modify the simplex during GJK iteration.
     *
     * @param index The index of the point to remove
     */
    public void remove(int index) {
        points.remove(index);
    }

    /**
     * Gets all points in the simplex.
     * Used primarily for EPA after GJK finds an intersection.
     *
     * @return The list of points in the simplex
     */
    public List<Vector2f> getPoints() {
        List<Vector2f> result = new ArrayList<>(points.size());
        for (Vector2f point : points) {
            result.add(new Vector2f(point));
        }
        return result;
    }

    /**
     * Sets the winding order of the simplex to counter-clockwise.
     * This is important for EPA to work correctly.
     */
    public void setCounterClockwiseOrder() {
        if (points.size() == 3) {
            Vector2f a = points.get(0);
            Vector2f b = points.get(1);
            Vector2f c = points.get(2);

            // Check if current winding is clockwise
            float cross = (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
            if (cross < 0) {
                // Swap b and c to make counter-clockwise
                points.set(1, c);
                points.set(2, b);
            }
        }
    }

    /**
     * Gets the furthest point in a given direction.
     * Used during EPA to expand the polytope.
     *
     * @param direction The direction to check
     * @return The point furthest in the given direction
     */
    public Vector2f getFurthestPoint(Vector2f direction) {
        Vector2f furthest = null;
        float maxDistance = Float.NEGATIVE_INFINITY;

        for (Vector2f point : points) {
            float distance = point.dot(direction);
            if (distance > maxDistance) {
                maxDistance = distance;
                furthest = point;
            }
        }

        return furthest != null ? new Vector2f(furthest) : null;
    }

    /**
     * Calculates whether the origin is contained within the simplex.
     * This is the core test of the GJK algorithm.
     *
     * @return true if the origin is contained within the simplex
     */
    public boolean containsOrigin() {
        switch (size()) {
            case 1:
                // A single point cannot contain the origin
                return false;
            case 2: {
                // A line cannot contain the origin
                return false;
            }
            case 3: {
                // For a triangle, check if the origin is on the correct side of each edge
                Vector2f a = get(0);
                Vector2f b = get(1);
                Vector2f c = get(2);

                Vector2f ab = new Vector2f(b).sub(a);
                Vector2f bc = new Vector2f(c).sub(b);
                Vector2f ca = new Vector2f(a).sub(c);

                Vector2f ao = new Vector2f(a).negate();
                Vector2f bo = new Vector2f(b).negate();
                Vector2f co = new Vector2f(c).negate();

                // Calculate perpendicular vectors pointing inward
                Vector2f abPerp = new Vector2f(-ab.y, ab.x);
                Vector2f bcPerp = new Vector2f(-bc.y, bc.x);
                Vector2f caPerp = new Vector2f(-ca.y, ca.x);

                // Origin must be on the inside of all edges
                return abPerp.dot(ao) >= 0 &&
                        bcPerp.dot(bo) >= 0 &&
                        caPerp.dot(co) >= 0;
            }
            default:
                // Should never happen in 2D
                return false;
        }
    }
}