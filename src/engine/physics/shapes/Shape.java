package engine.physics.shapes;

import org.joml.Vector2f;

public interface Shape {
    /**
     * Check if this shape intersects with another shape.
     */
    boolean intersects(Shape other);

    /**
     * Get the radius of the circle that fully contains this shape.
     */
    float getBoundingRadius();

    /**
     * Get the center point of this shape.
     */
    Vector2f getCenter();

    /**
     * Set the center point of this shape.
     */
    void setCenter(Vector2f center);

    /**
     * Get the type of this shape.
     */
    ShapeType getType();

    /**
     * Create a deep copy of this shape.
     */
    Shape clone();

    /**
     * Apply a rotation to this shape (in radians).
     */
    void rotate(float rotation);

    /**
     * Get the area of this shape.
     */
    float getArea();
}