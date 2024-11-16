package engine.physics.shapes;

import org.joml.Vector2f;

public class PolygonShape implements Shape {
    private Vector2f[] vertices;        // Original vertices in local space
    private Vector2f[] worldVertices;   // Transformed vertices in world space
    private Vector2f[] edges;           // Edges of the polygon
    private Vector2f[] normals;         // Normal vectors of edges
    private Vector2f center;            // Center position
    private float boundingRadius;       // Radius of bounding circle
    private float rotation;             // Current rotation in radians

    public PolygonShape(Vector2f[] vertices) {
        // Store original vertices
        this.vertices = new Vector2f[vertices.length];
        this.worldVertices = new Vector2f[vertices.length];
        for (int i = 0; i < vertices.length; i++) {
            this.vertices[i] = new Vector2f(vertices[i]);
            this.worldVertices[i] = new Vector2f(vertices[i]);
        }

        this.edges = new Vector2f[vertices.length];
        this.normals = new Vector2f[vertices.length];
        this.center = new Vector2f(0, 0);
        this.rotation = 0;

        updateTransform();
        calculateBoundingRadius();
    }

    private void updateTransform() {
        float cos = (float) Math.cos(rotation);
        float sin = (float) Math.sin(rotation);

        // Transform vertices from local to world space
        for (int i = 0; i < vertices.length; i++) {
            float rotatedX = vertices[i].x * cos - vertices[i].y * sin;
            float rotatedY = vertices[i].x * sin + vertices[i].y * cos;

            worldVertices[i].x = rotatedX + center.x;
            worldVertices[i].y = rotatedY + center.y;
        }

        // Update edges and normals
        updateEdgesAndNormals();
    }

    private void updateEdgesAndNormals() {
        for (int i = 0; i < worldVertices.length; i++) {
            int nextIndex = (i + 1) % worldVertices.length;

            edges[i] = new Vector2f(worldVertices[nextIndex]).sub(worldVertices[i]);
            normals[i] = new Vector2f(-edges[i].y, edges[i].x).normalize();
        }
    }

    @Override
    public boolean intersects(Shape other) {
        if (other instanceof CircleShape) {
            return intersectsCircle((CircleShape) other);
        } else if (other instanceof PolygonShape) {
            return intersectsPolygon((PolygonShape) other);
        }
        return false;
    }

    private boolean intersectsCircle(CircleShape circle) {
        // Find closest point on polygon to circle center
        Vector2f closestPoint = findClosestPoint(circle.getCenter());

        // Check if closest point is within circle radius
        float distanceSquared = closestPoint.distanceSquared(circle.getCenter());
        return distanceSquared <= circle.getRadius() * circle.getRadius();
    }

    private boolean intersectsPolygon(PolygonShape other) {
        // SAT (Separating Axis Theorem) implementation
        // Check this polygon's normals
        for (Vector2f normal : this.normals) {
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

    private boolean hasSeperatingAxis(Vector2f axis, PolygonShape poly1, PolygonShape poly2) {
        float[] projection1 = projectOntoAxis(axis, poly1.worldVertices);
        float[] projection2 = projectOntoAxis(axis, poly2.worldVertices);

        return projection1[1] < projection2[0] || projection2[1] < projection1[0];
    }

    private float[] projectOntoAxis(Vector2f axis, Vector2f[] points) {
        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;

        for (Vector2f point : points) {
            float projection = point.dot(axis);
            min = Math.min(min, projection);
            max = Math.max(max, projection);
        }

        return new float[]{min, max};
    }

    private Vector2f findClosestPoint(Vector2f point) {
        Vector2f closest = null;
        float minDistance = Float.POSITIVE_INFINITY;

        for (int i = 0; i < worldVertices.length; i++) {
            Vector2f start = worldVertices[i];
            Vector2f end = worldVertices[(i + 1) % worldVertices.length];
            Vector2f closestOnSegment = closestPointOnLineSegment(start, end, point);

            float distance = closestOnSegment.distanceSquared(point);
            if (distance < minDistance) {
                minDistance = distance;
                closest = closestOnSegment;
            }
        }

        return closest;
    }

    private Vector2f closestPointOnLineSegment(Vector2f start, Vector2f end, Vector2f point) {
        Vector2f line = new Vector2f(end).sub(start);
        float lineLength = line.length();
        line.div(lineLength); // Normalize

        Vector2f startToPoint = new Vector2f(point).sub(start);
        float projection = startToPoint.dot(line);

        if (projection <= 0) {
            return new Vector2f(start);
        }
        if (projection >= lineLength) {
            return new Vector2f(end);
        }

        return new Vector2f(start).add(new Vector2f(line).mul(projection));
    }

    private void calculateBoundingRadius() {
        float maxDistSquared = 0;
        Vector2f centroid = calculateCentroid();
        for (Vector2f vertex : vertices) {
            float distSquared = vertex.distanceSquared(centroid);
            maxDistSquared = Math.max(maxDistSquared, distSquared);
        }
        this.boundingRadius = (float) Math.sqrt(maxDistSquared);
    }

    private Vector2f calculateCentroid() {
        Vector2f centroid = new Vector2f();
        for (Vector2f vertex : vertices) {
            centroid.add(vertex);
        }
        return centroid.div(vertices.length);
    }

    // Interface implementations
    @Override
    public float getArea() {
        float area = 0;
        Vector2f[] verts = getWorldVertices();
        for (int i = 0; i < verts.length; i++) {
            Vector2f curr = verts[i];
            Vector2f next = verts[(i + 1) % verts.length];
            area += curr.x * next.y - curr.y * next.x;
        }
        return Math.abs(area) / 2;
    }

    @Override
    public float getBoundingRadius() {
        return boundingRadius;
    }

    @Override
    public Vector2f getCenter() {
        return new Vector2f(center);
    }

    @Override
    public ShapeType getType() {
        return ShapeType.POLYGON;
    }

    @Override
    public Shape clone() {
        Vector2f[] vertexCopies = new Vector2f[vertices.length];
        for (int i = 0; i < vertices.length; i++) {
            vertexCopies[i] = new Vector2f(vertices[i]);
        }
        PolygonShape clone = new PolygonShape(vertexCopies);
        clone.rotation = this.rotation;
        clone.center.set(this.center);
        return clone;
    }

    @Override
    public void setCenter(Vector2f newCenter) {
        this.center.set(newCenter);
        updateTransform();
    }

    // Additional methods
    public void rotate(float angle) {
        this.rotation = angle;
        updateTransform();
    }

    public Vector2f[] getVertices() {
        return vertices;
    }

    public Vector2f[] getWorldVertices() {
        return worldVertices;
    }

    public float getRotation() {
        return rotation;
    }
}