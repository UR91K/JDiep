//Shape.java
//LEGACY IMPLEMENTATION
//USE THIS FOR REFERENCE
package main;

import org.joml.Vector2f;

public interface Shape {
    boolean intersects(Shape other);
    float getBoundingRadius();
    Vector2f getCenter();
    ShapeType getType();
    void setCenter(Vector2f center);
    Shape clone();
}
