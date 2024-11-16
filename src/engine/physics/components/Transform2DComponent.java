package engine.physics.components;

import engine.ecs.Component;
import org.joml.Vector2f;

public class Transform2DComponent extends Component {
    private Vector2f position;
    private Vector2f scale;
    private float rotation;

    public Transform2DComponent() {
        this.position = new Vector2f();
        this.scale = new Vector2f(1, 1);
        this.rotation = 0;
    }

    public Transform2DComponent(Vector2f position) {
        this();
        this.position.set(position);
    }

    public Vector2f getPosition() {
        return new Vector2f(position);
    }

    public void setPosition(Vector2f position) {
        this.position.set(position);
    }

    public Vector2f getScale() {
        return new Vector2f(scale);
    }

    public void setScale(Vector2f scale) {
        this.scale.set(scale);
    }

    public float getRotation() {
        return rotation;
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    // Direct access for performance when needed
    public Vector2f getPositionDirect() {
        return position;
    }
}