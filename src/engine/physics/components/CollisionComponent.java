package engine.physics.components;

import engine.ecs.Component;
import engine.physics.shapes.Shape;
import java.util.ArrayList;
import java.util.List;

public class CollisionComponent extends Component {
    private List<Shape> colliders;
    private boolean isTrigger;
    private int collisionLayer;
    private int collisionMask;

    public CollisionComponent() {
        this.colliders = new ArrayList<>();
        this.isTrigger = false;
        this.collisionLayer = 0x0001;  // Default layer
        this.collisionMask = 0xFFFF;   // Collide with all layers by default
    }

    public void addCollider(Shape shape) {
        colliders.add(shape);
    }

    public void removeCollider(Shape shape) {
        colliders.remove(shape);
    }

    public List<Shape> getColliders() {
        return colliders;
    }

    public boolean isTrigger() {
        return isTrigger;
    }

    public void setTrigger(boolean trigger) {
        isTrigger = trigger;
    }

    public int getCollisionLayer() {
        return collisionLayer;
    }

    public void setCollisionLayer(int layer) {
        this.collisionLayer = layer;
    }

    public int getCollisionMask() {
        return collisionMask;
    }

    public void setCollisionMask(int mask) {
        this.collisionMask = mask;
    }

    public boolean canCollideWith(CollisionComponent other) {
        return (this.collisionLayer & other.collisionMask) != 0 &&
                (other.collisionLayer & this.collisionMask) != 0;
    }
}