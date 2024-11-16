package engine.physics.shapes;

import org.joml.Vector2f;

public class CircleShape extends AbstractShape {
    private float radius;

    public CircleShape(float radius) {
        super();
        this.radius = radius;
        this.boundingRadius = radius;
    }

    @Override
    public boolean intersects(Shape other) {
        if (other instanceof CircleShape) {
            return intersectsCircle((CircleShape) other);
        }
        return other.intersects(this);
    }

    private boolean intersectsCircle(CircleShape other) {
        float distanceSquared = center.distanceSquared(other.center);
        float radiusSum = radius + other.radius;
        return distanceSquared <= radiusSum * radiusSum;
    }

    @Override
    protected void updateTransform() {
        // Circles don't need transform updates for rotation
    }

    @Override
    public ShapeType getType() {
        return ShapeType.CIRCLE;
    }

    @Override
    public Shape clone() {
        CircleShape clone = new CircleShape(radius);
        clone.setCenter(new Vector2f(center));
        return clone;
    }

    @Override
    public float getArea() {
        return (float) (Math.PI * radius * radius);
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
        this.boundingRadius = radius;
    }
}