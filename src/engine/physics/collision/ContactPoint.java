package engine.physics.collision;

import engine.ecs.Entity;
import engine.physics.components.RigidBodyComponent;
import org.joml.Vector2f;

/**
 * Represents a collision contact point between two bodies
 */
public class ContactPoint {
    public final Entity entityA;
    public final Entity entityB;
    public final Vector2f point;
    public final Vector2f normal;
    public final float penetration;
    public final float restitution;
    public final float friction;

    // Constructor for when we have entities
    public ContactPoint(Entity entityA, Entity entityB, Vector2f point,
                        Vector2f normal, float penetration) {
        this.entityA = entityA;
        this.entityB = entityB;
        this.point = new Vector2f(point);
        this.normal = new Vector2f(normal);
        this.penetration = penetration;

        // Calculate combined material properties if entities exist
        if (entityA != null && entityB != null) {
            RigidBodyComponent bodyA = entityA.getComponent(RigidBodyComponent.class);
            RigidBodyComponent bodyB = entityB.getComponent(RigidBodyComponent.class);
            this.restitution = Math.min(bodyA.getRestitution(), bodyB.getRestitution());
            this.friction = (bodyA.getFriction() + bodyB.getFriction()) * 0.5f;
        } else {
            this.restitution = 0.5f;  // Default values
            this.friction = 0.1f;
        }
    }

    // Constructor for GJK/EPA calculations without entities
    public ContactPoint(Vector2f point, Vector2f normal, float penetration) {
        this.entityA = null;
        this.entityB = null;
        this.point = new Vector2f(point);
        this.normal = new Vector2f(normal);
        this.penetration = penetration;
        this.restitution = 0.5f;  // Default values
        this.friction = 0.1f;
    }
}
