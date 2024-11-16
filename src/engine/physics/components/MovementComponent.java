package engine.physics.components;

import engine.core.logging.Logger;
import engine.ecs.Component;
import org.joml.Vector2f;

public class MovementComponent extends Component {
    private static final Logger logger = Logger.getLogger(MovementComponent.class);

    private Vector2f velocity = new Vector2f();
    private Vector2f currentForce = new Vector2f();  // New field to store desired force
    private float moveSpeed;
    private float friction;
    private float mass;
    private float bounceFactor;

    public MovementComponent(float moveSpeed, float friction, float mass, float bounceFactor) {
        this.moveSpeed = moveSpeed;
        this.friction = friction;
        this.mass = mass;
        this.bounceFactor = bounceFactor;
    }

    public Vector2f getVelocity() {
        return new Vector2f(velocity);
    }

    public void setVelocity(Vector2f velocity) {
        this.velocity.set(velocity);
    }

    public Vector2f getVelocityDirect() {
        return velocity;
    }

    public float getMoveSpeed() {
        return moveSpeed;
    }

    public void setMoveSpeed(float moveSpeed) {
        this.moveSpeed = moveSpeed;
    }

    public float getFriction() {
        return friction;
    }

    public void setFriction(float friction) {
        this.friction = friction;
    }

    public float getMass() {
        return mass;
    }

    public void setMass(float mass) {
        this.mass = mass;
    }

    public float getBounceFactor() {
        return bounceFactor;
    }

    public void setBounceFactor(float bounceFactor) {
        this.bounceFactor = bounceFactor;
    }

    public void setDesiredForce(Vector2f force) {
        logger.trace("Setting desired force: {} (previous: {})", force, currentForce);
        currentForce.set(force);
    }

    public Vector2f getCurrentForce() {
        return new Vector2f(currentForce);
    }

}
