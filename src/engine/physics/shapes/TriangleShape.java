package engine.physics.shapes;

import org.joml.Vector2f;

public class TriangleShape extends RegularPolygonShape {
    public TriangleShape(float radius) {
        super(3, radius);
    }

    @Override
    public Shape clone() {
        TriangleShape clone = new TriangleShape(this.boundingRadius);
        clone.setCenter(new Vector2f(this.center));
        clone.rotate(this.rotation);
        return clone;
    }
}
