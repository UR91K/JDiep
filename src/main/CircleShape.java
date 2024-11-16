// CircleShape.java
//LEGACY IMPLEMENTATION
//USE THIS FOR REFERENCE
package main;

import org.joml.Vector2f;

public class CircleShape implements Shape {
    private float radius;
    private Vector2f center;

    public CircleShape(float radius) {
        this.radius = radius;
        this.center = new Vector2f(0, 0);
    }

    @Override
    public boolean intersects(Shape other) {
        if (other instanceof CircleShape) {
            return intersectsCircle((CircleShape) other);
        } else if (other instanceof PolygonShape) {
            return ((PolygonShape) other).intersects(this);
        }
        return false;
    }

    private boolean intersectsCircle(CircleShape other) {
        float distanceSquared = center.distanceSquared(other.center);
        float radiusSum = radius + other.radius;
        return distanceSquared <= radiusSum * radiusSum;
    }

    @Override
    public float getBoundingRadius() {
        return radius;
    }

    @Override
    public Vector2f getCenter() {
        return new Vector2f(center);
    }

    @Override
    public void setCenter(Vector2f center) {
        this.center.set(center);
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

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }
}
