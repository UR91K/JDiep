package engine.physics.forces;

import org.joml.Vector2f;
import engine.physics.components.RigidBodyComponent;

/**
 * Applies movement force based on input direction
 */
public class TankMovementForce implements ForceGenerator {
    private final Vector2f moveDirection;
    private float moveSpeed;

    public TankMovementForce() {
        this.moveDirection = new Vector2f();
        this.moveSpeed = 27.0f; // Default from GameConstants
    }

    public void setMoveDirection(Vector2f direction) {
        if (direction.lengthSquared() > 0) {
            moveDirection.set(direction).normalize();
        } else {
            moveDirection.zero();
        }
    }

    public void setMoveSpeed(float speed) {
        this.moveSpeed = speed;
    }

    @Override
    public void applyForce(RigidBodyComponent body, float dt) {
        if (moveDirection.lengthSquared() > 0) {
            Vector2f force = new Vector2f(moveDirection).mul(moveSpeed * body.getMass());
            body.applyForce(force);  // Just uses single argument version
        }
    }
}
