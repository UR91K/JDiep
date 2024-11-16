package engine.physics.shapes;

import org.joml.Vector2f;

public abstract class AbstractShape implements Shape {
    protected Vector2f center;
    protected float rotation;
    protected float boundingRadius;

    protected AbstractShape() {
        this.center = new Vector2f();
        this.rotation = 0.0f;
    }

    @Override
    public abstract Shape clone();

    @Override
    public Vector2f getCenter() {
        return new Vector2f(center);
    }

    @Override
    public void setCenter(Vector2f center) {
        this.center.set(center);
    }

    @Override
    public float getBoundingRadius() {
        return boundingRadius;
    }

    @Override
    public void rotate(float rotation) {
        this.rotation = rotation;
        updateTransform();
    }

    /**
     * Update the shape's transform after position/rotation changes.
     * Must be implemented by concrete shapes.
     */
    protected abstract void updateTransform();
}