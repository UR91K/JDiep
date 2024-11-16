package engine.physics.collision;

import engine.physics.collision.Simplex;

import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * GJK/EPA implementation for accurate collision detection and contact generation.
 */
public class GJKCollision {
    private static final int MAX_ITERATIONS = 32;
    private static final float EPSILON = 0.0001f;

    /**
     * Support point calculation for shapes
     */
    public interface SupportPoint {
        Vector2f support(Vector2f direction);
    }

    /**
     * Support point calculation interface
     */
    @FunctionalInterface
    public interface SupportFunction {
        Vector2f support(Vector2f direction);
    }

    /**
     * Determines if two shapes are intersecting
     * @return The final simplex if shapes intersect, null otherwise
     */
    public boolean intersect(SupportFunction shapeA, SupportFunction shapeB) {
        return getIntersectionSimplex(shapeA, shapeB) != null;
    }

    /**
     * Get intersection simplex if shapes intersect
     */
    public Simplex getIntersectionSimplex(SupportFunction shapeA, SupportFunction shapeB) {
        Simplex simplex = new Simplex();

        // Get initial support point in any direction
        Vector2f direction = new Vector2f(1, 0);
        Vector2f firstPoint = getMinkowskiSupport(shapeA, shapeB, direction);
        simplex.add(firstPoint);

        // Next direction is towards origin
        direction.set(firstPoint).negate();

        // Main GJK loop
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            Vector2f newPoint = getMinkowskiSupport(shapeA, shapeB, direction);

            if (newPoint.dot(direction) < 0) {
                return null;
            }

            simplex.add(newPoint);

            if (processSimplex(simplex, direction)) {
                return simplex;
            }
        }

        return null;
    }

    /**
     * Gets contact information using EPA (Expanding Polytope Algorithm)
     */
    public ContactPoint getContactPoint(SupportFunction shapeA, SupportFunction shapeB, Simplex simplex) {
        // Initialize polytope from the simplex
        List<Vector2f> polytope = new ArrayList<>(simplex.getPoints());
        List<Vector2f> normals = new ArrayList<>();

        // Ensure CCW winding order for consistent normal direction
        ensureCounterClockwise(polytope);

        // EPA loop - expand polytope until we find the edge closest to origin
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            // Find the closest edge to the origin
            EdgeInfo closest = findClosestEdge(polytope);

            // Cache the normals for better robustness
            normals.clear();
            calculateNormals(polytope, normals);

            // Get new support point in direction of edge normal
            Vector2f support = getMinkowskiSupport(shapeA, shapeB, closest.normal);
            float supportDist = support.dot(closest.normal);

            // Check if we're done
            float difference = supportDist - closest.distance;
            if (difference < EPSILON) {
                // We've found the edge of minimum penetration
                Vector2f contactNormal = new Vector2f(closest.normal);
                float penetration = supportDist;

                // Get actual contact points on both shapes
                Vector2f pointA = shapeA.support(contactNormal);
                Vector2f pointB = shapeB.support(new Vector2f(contactNormal).negate());

                // Calculate the actual contact point
                Vector2f contactPoint = new Vector2f(pointA).add(pointB).mul(0.5f);

                // Ensure normal points from A to B
                if (contactNormal.dot(new Vector2f(pointB).sub(pointA)) < 0) {
                    contactNormal.negate();
                    penetration = -penetration;
                }

                return new ContactPoint(null, null, contactPoint, contactNormal, penetration);
            }

            // Insert the new point to expand the polytope
            insertPoint(polytope, closest.index, support);
        }

        // If we get here, EPA didn't converge - use best guess
        EdgeInfo closest = findClosestEdge(polytope);
        Vector2f pointA = shapeA.support(closest.normal);
        Vector2f pointB = shapeB.support(new Vector2f(closest.normal).negate());

        return new ContactPoint(
                null, null,
                new Vector2f(pointA).add(pointB).mul(0.5f),
                closest.normal,
                closest.distance
        );
    }

    /**
     * Ensures polytope vertices are in counter-clockwise order
     */
    private void ensureCounterClockwise(List<Vector2f> polytope) {
        // Calculate signed area
        float area = 0;
        for (int i = 0; i < polytope.size(); i++) {
            Vector2f current = polytope.get(i);
            Vector2f next = polytope.get((i + 1) % polytope.size());
            area += (next.x - current.x) * (next.y + current.y);
        }

        // If clockwise, reverse the list
        if (area > 0) {
            for (int i = 0; i < polytope.size() / 2; i++) {
                int j = polytope.size() - 1 - i;
                Vector2f temp = polytope.get(i);
                polytope.set(i, polytope.get(j));
                polytope.set(j, temp);
            }
        }
    }

    /**
     * Calculate normals for all edges
     */
    private void calculateNormals(List<Vector2f> polytope, List<Vector2f> normals) {
        for (int i = 0; i < polytope.size(); i++) {
            Vector2f current = polytope.get(i);
            Vector2f next = polytope.get((i + 1) % polytope.size());

            // Edge vector
            Vector2f edge = new Vector2f(next).sub(current);

            // Normal (perpendicular to edge, pointing inward)
            Vector2f normal = new Vector2f(-edge.y, edge.x).normalize();
            normals.add(normal);
        }
    }

    private Vector2f getMinkowskiSupport(SupportFunction shapeA, SupportFunction shapeB, Vector2f direction) {
        Vector2f support1 = shapeA.support(direction);
        Vector2f support2 = shapeB.support(new Vector2f(direction).negate());
        return new Vector2f(support1).sub(support2);
    }

    private boolean processSimplex(Simplex simplex, Vector2f direction) {
        switch (simplex.size()) {
            case 2: return handleLine(simplex, direction);
            case 3: return handleTriangle(simplex, direction);
            default: return false;
        }
    }

    private boolean handleLine(Simplex simplex, Vector2f direction) {
        Vector2f a = simplex.get(0); // Latest point
        Vector2f b = simplex.get(1); // Previous point
        Vector2f ab = new Vector2f(b).sub(a);    // Vector from A to B
        Vector2f ao = new Vector2f(a).negate();  // Vector from A to Origin

        // Get perpendicular direction to AB towards origin
        Vector2f perp = tripleProduct(ab, ao, ab);

        if (perp.lengthSquared() == 0) {
            // Line segment contains origin or direction is degenerate
            perp.set(-ab.y, ab.x);
        }

        direction.set(perp);
        return false;
    }

    private boolean handleTriangle(Simplex simplex, Vector2f direction) {
        Vector2f a = simplex.get(0); // Latest point
        Vector2f b = simplex.get(1);
        Vector2f c = simplex.get(2);

        Vector2f ab = new Vector2f(b).sub(a);
        Vector2f ac = new Vector2f(c).sub(a);
        Vector2f ao = new Vector2f(a).negate();

        // Get perpendicular vectors
        Vector2f abPerp = tripleProduct(ac, ab, ab);
        Vector2f acPerp = tripleProduct(ab, ac, ac);

        // Determine which region the origin is in
        if (abPerp.dot(ao) > 0) {
            // Origin is on the right of AB
            simplex.remove(2); // Remove C
            direction.set(abPerp);
            return false;
        } else if (acPerp.dot(ao) > 0) {
            // Origin is on the left of AC
            simplex.remove(1); // Remove B
            direction.set(acPerp);
            return false;
        }

        // Origin is inside triangle
        return true;
    }

    private Vector2f tripleProduct(Vector2f a, Vector2f b, Vector2f c) {
        // Compute (A × B) × C = B(C·A) - A(C·B)
        float dotCA = c.dot(a);
        float dotCB = c.dot(b);
        return new Vector2f(b).mul(dotCA).sub(new Vector2f(a).mul(dotCB));
    }

    private static class EdgeInfo {
        final int index;
        final Vector2f normal;
        final float distance;

        EdgeInfo(int index, Vector2f normal, float distance) {
            this.index = index;
            this.normal = normal;
            this.distance = distance;
        }
    }

    /**
     * Improved edge finding with cached normals
     */
    private EdgeInfo findClosestEdge(List<Vector2f> polytope) {
        int closestIndex = 0;
        float closestDistance = Float.POSITIVE_INFINITY;
        Vector2f closestNormal = new Vector2f();

        for (int i = 0; i < polytope.size(); i++) {
            Vector2f current = polytope.get(i);
            Vector2f next = polytope.get((i + 1) % polytope.size());

            // Edge vector
            Vector2f edge = new Vector2f(next).sub(current);

            // Normal (perpendicular to edge, pointing inward)
            Vector2f normal = new Vector2f(-edge.y, edge.x).normalize();

            // Distance from origin to edge along normal
            float distance = Math.abs(normal.dot(current));

            if (distance < closestDistance) {
                closestDistance = distance;
                closestIndex = i;
                closestNormal.set(normal);
            }
        }

        return new EdgeInfo(closestIndex, closestNormal, closestDistance);
    }

    private void insertPoint(List<Vector2f> polytope, int index, Vector2f point) {
        polytope.add(index + 1, point);
    }
}
