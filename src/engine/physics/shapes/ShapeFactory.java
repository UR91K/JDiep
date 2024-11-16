package engine.physics.shapes;

import org.joml.Vector2f;

public class ShapeFactory {
    public static PolygonShape createRegularPolygon(int sides, float radius) {
        Vector2f[] vertices = new Vector2f[sides];
        float angleStep = 2 * (float) Math.PI / sides;

        for (int i = 0; i < sides; i++) {
            float angle = i * angleStep;
            float x = radius * (float) Math.cos(angle);
            float y = radius * (float) Math.sin(angle);
            vertices[i] = new Vector2f(x, y);
        }

        return new PolygonShape(vertices);
    }

    public static PolygonShape createRectangle(float width, float height) {
        Vector2f[] vertices = new Vector2f[4];
        float halfWidth = width / 2;
        float halfHeight = height / 2;

        vertices[0] = new Vector2f(-halfWidth, -halfHeight);
        vertices[1] = new Vector2f(halfWidth, -halfHeight);
        vertices[2] = new Vector2f(halfWidth, halfHeight);
        vertices[3] = new Vector2f(-halfWidth, halfHeight);

        return new PolygonShape(vertices);
    }

    public static CircleShape createCircle(float radius) {
        return new CircleShape(radius);
    }

    // Helper methods for diep.io style shapes
    public static PolygonShape createTriangle(float radius) {
        return createRegularPolygon(3, radius);
    }

    public static PolygonShape createPentagon(float radius) {
        return createRegularPolygon(5, radius);
    }
}