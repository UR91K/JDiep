package engine.physics.shapes;

import org.joml.Vector2f;

public class RegularPolygonShape extends AbstractShape {
    protected Vector2f[] vertices;        // Original vertices in local space
    protected Vector2f[] worldVertices;   // Transformed vertices in world space
    protected Vector2f[] edges;           // Edges between vertices
    protected Vector2f[] normals;         // Normal vectors for edges
    protected final int sides;            // Number of sides

    public RegularPolygonShape(int sides, float radius) {
        super();
        this.sides = sides;
        this.boundingRadius = radius;

        initializeVertices(radius);
        updateTransform();
    }

    private void initializeVertices(float radius) {
        vertices = new Vector2f[sides];
        worldVertices = new Vector2f[sides];
        edges = new Vector2f[sides];
        normals = new Vector2f[sides];

        float angleStep = 2 * (float) Math.PI / sides;

        for (int i = 0; i < sides; i++) {
            float angle = i * angleStep;
            float x = radius * (float) Math.cos(angle);
            float y = radius * (float) Math.sin(angle);
            vertices[i] = new Vector2f(x, y);
            worldVertices[i] = new Vector2f(x, y);
        }
    }

    @Override
    protected void updateTransform() {
        float cos = (float) Math.cos(rotation);
        float sin = (float) Math.sin(rotation);

        // Transform vertices to world space
        for (int i = 0; i < sides; i++) {
            float x = vertices[i].x;
            float y = vertices[i].y;

            worldVertices[i].x = x * cos - y * sin + center.x;
            worldVertices[i].y = x * sin + y * cos + center.y;
        }

        // Update edges and normals
        for (int i = 0; i < sides; i++) {
            int next = (i + 1) % sides;
            edges[i] = new Vector2f(worldVertices[next]).sub(worldVertices[i]);
            normals[i] = new Vector2f(-edges[i].y, edges[i].x).normalize();
        }
    }

    @Override
    public boolean intersects(Shape other) {
        if (other instanceof CircleShape) {
            return intersectsCircle((CircleShape) other);
        } else if (other instanceof RegularPolygonShape) {
            return intersectsPolygon((RegularPolygonShape) other);
        }
        return false;
    }

    private boolean intersectsCircle(CircleShape circle) {
        // Find closest point on polygon to circle center
        Vector2f closestPoint = findClosestPoint(circle.getCenter());
        float distanceSquared = closestPoint.distanceSquared(circle.getCenter());
        return distanceSquared <= circle.getRadius() * circle.getRadius();
    }

    private boolean intersectsPolygon(RegularPolygonShape other) {
        // SAT (Separating Axis Theorem)
        // Check this polygon's normals
        for (Vector2f normal : normals) {
            if (hasSeperatingAxis(normal, this, other)) {
                return false;
            }
        }

        // Check other polygon's normals
        for (Vector2f normal : other.normals) {
            if (hasSeperatingAxis(normal, this, other)) {
                return false;
            }
        }

        return true;
    }

    private Vector2f findClosestPoint(Vector2f point) {
        Vector2f closest = null;
        float minDistance = Float.POSITIVE_INFINITY;

        for (int i = 0; i < sides; i++) {
            int next = (i + 1) % sides;
            Vector2f edgePoint = closestPointOnLine(
                    worldVertices[i],
                    worldVertices[next],
                    point
            );

            float distance = edgePoint.distanceSquared(point);
            if (distance < minDistance) {
                minDistance = distance;
                closest = edgePoint;
            }
        }

        return closest;
    }

    private Vector2f closestPointOnLine(Vector2f start, Vector2f end, Vector2f point) {
        Vector2f edge = new Vector2f(end).sub(start);
        float length = edge.length();
        edge.div(length);

        Vector2f toPoint = new Vector2f(point).sub(start);
        float dot = toPoint.dot(edge);

        if (dot <= 0) return new Vector2f(start);
        if (dot >= length) return new Vector2f(end);

        return new Vector2f(start).add(new Vector2f(edge).mul(dot));
    }

    private boolean hasSeperatingAxis(Vector2f axis, RegularPolygonShape poly1, RegularPolygonShape poly2) {
        float[] p1 = projectOntoAxis(axis, poly1.worldVertices);
        float[] p2 = projectOntoAxis(axis, poly2.worldVertices);
        return p1[1] < p2[0] || p2[1] < p1[0];
    }

    private float[] projectOntoAxis(Vector2f axis, Vector2f[] vertices) {
        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;

        for (Vector2f vertex : vertices) {
            float projection = vertex.dot(axis);
            min = Math.min(min, projection);
            max = Math.max(max, projection);
        }

        return new float[]{min, max};
    }

    @Override
    public float getArea() {
        float apothem = boundingRadius * (float)Math.cos(Math.PI / sides);
        float perimeter = 2 * sides * boundingRadius * (float)Math.sin(Math.PI / sides);
        return 0.5f * apothem * perimeter;
    }

    // ADDED: Missing abstract method implementations
    @Override
    public Shape clone() {
        RegularPolygonShape clone = new RegularPolygonShape(this.sides, this.boundingRadius);
        clone.setCenter(new Vector2f(this.center));
        clone.rotate(this.rotation);
        return clone;
    }

    @Override
    public ShapeType getType() {
        switch (sides) {
            case 3:
                return ShapeType.TRIANGLE;
            case 4:
                return ShapeType.RECTANGLE;
            case 5:
                return ShapeType.PENTAGON;
            default:
                throw new IllegalStateException("Unsupported number of sides: " + sides);
        }
    }

    public Vector2f[] getWorldVertices() {
        return worldVertices;
    }

    public Vector2f[] getNormals() {
        return normals;
    }

    public int getSides() {
        return sides;
    }
}