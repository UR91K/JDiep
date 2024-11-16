package engine.physics.shapes;

import org.joml.Vector2f;

public class RectangleShape extends RegularPolygonShape {
    private float width;
    private float height;

    /**
     * Create a rectangle shape with equal width and height.
     */
    public RectangleShape(float size) {
        this(size, size);
    }

    /**
     * Create a rectangle shape with specified width and height.
     */
    public RectangleShape(float width, float height) {
        super(4, Math.max(width, height) / 2);
        this.width = width;
        this.height = height;
        initializeRectVertices();
    }

    private void initializeRectVertices() {
        float halfWidth = width / 2;
        float halfHeight = height / 2;

        vertices[0] = new Vector2f(-halfWidth, -halfHeight);
        vertices[1] = new Vector2f(halfWidth, -halfHeight);
        vertices[2] = new Vector2f(halfWidth, halfHeight);
        vertices[3] = new Vector2f(-halfWidth, halfHeight);

        // Initialize world vertices
        for (int i = 0; i < 4; i++) {
            worldVertices[i] = new Vector2f(vertices[i]);
        }

        updateTransform();
    }

    @Override
    public Shape clone() {
        RectangleShape clone = new RectangleShape(this.width, this.height);
        clone.setCenter(new Vector2f(this.center));
        clone.rotate(this.rotation);
        return clone;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    @Override
    public float getArea() {
        return width * height;
    }
}
