package engine.physics.collision;

import engine.ecs.Entity;
import engine.physics.components.RigidBodyComponent;
import org.joml.Vector2f;

/**
 * Handles collision resolution between physics bodies
 */
public class CollisionResolver {
    private static final float POSITION_CORRECTION_FACTOR = 0.2f; // Baumgarte scalar
    private static final float POSITION_CORRECTION_SLOP = 0.01f;  // Penetration allowance

    public void resolveContact(ContactPoint contact, float dt) {
        Entity entityA = contact.entityA;
        Entity entityB = contact.entityB;

        RigidBodyComponent bodyA = entityA.getComponent(RigidBodyComponent.class);
        RigidBodyComponent bodyB = entityB.getComponent(RigidBodyComponent.class);

        // Skip if both bodies are static
        if (bodyA.isStatic() && bodyB.isStatic()) return;

        resolveVelocity(contact, bodyA, bodyB, dt);
        resolvePosition(contact, bodyA, bodyB);
    }

    private void resolveVelocity(ContactPoint contact,
                                 RigidBodyComponent bodyA,
                                 RigidBodyComponent bodyB,
                                 float dt) {
        Vector2f relativeVel = new Vector2f(bodyB.getVelocity())
                .sub(bodyA.getVelocity());

        // Calculate relative velocity along normal
        float normalVelocity = relativeVel.dot(contact.normal);

        // No impulse needed if objects are separating
        if (normalVelocity > 0) return;

        // Calculate impulse scalar
        float totalInverseMass = bodyA.getInverseMass() + bodyB.getInverseMass();
        if (totalInverseMass <= 0) return;

        // Calculate impulse magnitude
        float j = -(1.0f + contact.restitution) * normalVelocity / totalInverseMass;

        // Apply normal impulse
        Vector2f impulse = new Vector2f(contact.normal).mul(j);
        if (!bodyA.isStatic()) {
            bodyA.getVelocityDirect().sub(new Vector2f(impulse).mul(bodyA.getInverseMass()));
        }
        if (!bodyB.isStatic()) {
            bodyB.getVelocityDirect().add(new Vector2f(impulse).mul(bodyB.getInverseMass()));
        }

        // Calculate and apply friction impulse
        relativeVel = new Vector2f(bodyB.getVelocity()).sub(bodyA.getVelocity());
        Vector2f tangent = new Vector2f(relativeVel).sub(
                new Vector2f(contact.normal).mul(relativeVel.dot(contact.normal))
        );

        if (tangent.lengthSquared() > 0.0001f) {
            tangent.normalize();
            float jt = -relativeVel.dot(tangent) / totalInverseMass;

            // Clamp friction impulse
            float maxFriction = j * contact.friction;
            jt = Math.max(-maxFriction, Math.min(jt, maxFriction));

            Vector2f frictionImpulse = new Vector2f(tangent).mul(jt);
            if (!bodyA.isStatic()) {
                bodyA.getVelocityDirect().sub(
                        new Vector2f(frictionImpulse).mul(bodyA.getInverseMass())
                );
            }
            if (!bodyB.isStatic()) {
                bodyB.getVelocityDirect().add(
                        new Vector2f(frictionImpulse).mul(bodyB.getInverseMass())
                );
            }
        }
    }

    private void resolvePosition(ContactPoint contact,
                                 RigidBodyComponent bodyA,
                                 RigidBodyComponent bodyB) {
        float totalInverseMass = bodyA.getInverseMass() + bodyB.getInverseMass();
        if (totalInverseMass <= 0) return;

        try {
            // Mark start of collision resolution
            bodyA.startCollisionResolution();
            bodyB.startCollisionResolution();

            // Calculate separation required
            float penetration = Math.max(
                    contact.penetration - POSITION_CORRECTION_SLOP,
                    0.0f
            );
            float correctionMagnitude = (penetration / totalInverseMass)
                    * POSITION_CORRECTION_FACTOR;
            Vector2f correction = new Vector2f(contact.normal).mul(correctionMagnitude);

            // Apply position corrections using collision-specific access
            if (!bodyA.isStatic()) {
                Vector2f posA = bodyA.getPositionForCollision();
                posA.sub(new Vector2f(correction).mul(bodyA.getInverseMass()));
            }
            if (!bodyB.isStatic()) {
                Vector2f posB = bodyB.getPositionForCollision();
                posB.add(new Vector2f(correction).mul(bodyB.getInverseMass()));
            }
        } finally {
            // Always ensure we end collision resolution
            bodyA.endCollisionResolution();
            bodyB.endCollisionResolution();
        }
    }
}

