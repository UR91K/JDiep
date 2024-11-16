package engine.physics.shapes;

import org.joml.Vector2f;

public class PentagonShape extends RegularPolygonShape {
    public PentagonShape(float radius) {
        super(5, radius);
    }

    @Override
    public Shape clone() {
        PentagonShape clone = new PentagonShape(this.boundingRadius);
        clone.setCenter(new Vector2f(this.center));
        clone.rotate(this.rotation);
        return clone;
    }
}
