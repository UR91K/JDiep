package engine.physics.components;

import engine.ecs.Component;
import org.joml.Vector2f;
import engine.core.logging.Logger;

public class RigidBodyComponent extends Component {
    // Core properties
    private float mass;
    private float inverseMass;
    private float momentOfInertia;
    private float inverseMomentOfInertia;

    // State
    private Vector2f position;
    private Vector2f lastPosition;  // For Verlet integration
    private float rotation;
    private float lastRotation;

    // Linear motion
    private Vector2f velocity;
    private Vector2f acceleration;
    private Vector2f accumulatedForce;

    // Angular motion
    private float angularVelocity;
    private float angularAcceleration;
    private float accumulatedTorque;

    // Material properties
    private float restitution;  // Bounciness
    private float friction;     // Surface friction
    private boolean isStatic;   // Immovable objects
    private float boundingRadius; // Added for collision detection

    private static final Logger logger = Logger.getLogger(RigidBodyComponent.class);
    private boolean integrating = false;
    private boolean resolvingCollision = false;
    private boolean initialized = false;

    public RigidBodyComponent(float mass) {
        this.mass = mass;
        this.inverseMass = mass > 0 ? 1.0f / mass : 0.0f;
        this.position = new Vector2f();
        this.lastPosition = new Vector2f();
        this.velocity = new Vector2f();
        this.acceleration = new Vector2f();
        this.accumulatedForce = new Vector2f();
    }

    /**
     * Applies a force to the center of mass
     */
    public void applyForce(Vector2f force) {
        if (!isStatic) {
            logger.trace("Adding force {} to accumulated {}", force, accumulatedForce);
            accumulatedForce.add(force);
            logger.trace("New accumulated force: {}", accumulatedForce);
        }
    }

    public void initializePosition(Vector2f pos) {
        if (!initialized) {
            position.set(pos);
            lastPosition.set(pos);
            initialized = true;
        } else {
            logger.warn("Attempted to initialize position after already initialized: {}", pos);
        }
    }

    /**
     * Applies a force at a specific point
     */
    public void applyForceAtPoint(Vector2f force, Vector2f point) {
        if (!isStatic) {
            accumulatedForce.add(force);

            // Calculate torque: τ = r × F
            Vector2f radius = new Vector2f(point).sub(position);
            float torque = radius.x * force.y - radius.y * force.x;
            accumulatedTorque += torque;
        }
    }

    /**
     * Updates velocity and position using semi-implicit Euler integration
     */
    public void integrate(float dt) {
        if (isStatic) return;
        integrating = true;

        try {
            // Store current state
            lastPosition.set(position);
            lastRotation = rotation;

            // Update linear motion
            acceleration.set(accumulatedForce).mul(inverseMass);
            velocity.add(new Vector2f(acceleration).mul(dt));
            position.add(new Vector2f(velocity).mul(dt));

            // Update angular motion
            if (accumulatedTorque != 0) {
                angularAcceleration = accumulatedTorque * inverseMomentOfInertia;
                angularVelocity += angularAcceleration * dt;
                rotation += angularVelocity * dt;
                rotation = normalizeAngle(rotation);  // Keep rotation in [0, 2π]

                logger.trace("Angular integration - torque: {}, vel: {}, rot: {}",
                        accumulatedTorque, angularVelocity, rotation);
            }

            // Clear accumulated forces and torques
            accumulatedForce.zero();
            accumulatedTorque = 0;
        } finally {
            integrating = false;
        }
    }

    /**
     * Updates the moment of inertia based on the body's shape.
     * This should be called whenever the body's shape changes.
     *
     * @param momentOfInertia The new moment of inertia value
     */
    public void updateInertia(float momentOfInertia) {
        this.momentOfInertia = momentOfInertia;
        this.inverseMomentOfInertia = isStatic ? 0 :
                (momentOfInertia > 0 ? 1.0f / momentOfInertia : 0.0f);
    }

    public float getBoundingRadius() {
        return boundingRadius;
    }

    /**
     * Updates the bounding radius for broad-phase collision detection.
     */
    public void setBoundingRadius(float radius) {
        this.boundingRadius = radius;
    }

    // Direct access for physics system

    // Getters and setters
    public float getMass() {
        return mass;
    }

    public float getInverseMass() {
        return inverseMass;
    }

    public Vector2f getPosition() {
        return new Vector2f(position);
    }

    public void startCollisionResolution() {
        resolvingCollision = true;
    }

    public void endCollisionResolution() {
        resolvingCollision = false;
    }

    public void setPosition(Vector2f newPos) {
        if (!integrating && !resolvingCollision && initialized) {
            logger.warn("Attempted to set position outside of integration/collision step: {}", newPos);
            return;
        }
        position.set(newPos);
        if (!initialized) {
            lastPosition.set(newPos);
            initialized = true;
        }
    }

    public Vector2f getVelocity() {
        return new Vector2f(velocity);
    }

    public void setVelocity(Vector2f velocity) {
        this.velocity.set(velocity);
    }



    public void setRotation(float rotation) {
        this.rotation = rotation;
        this.lastRotation = rotation;
    }



    public void setAngularVelocity(float angularVelocity) {
        this.angularVelocity = angularVelocity;
    }

    public float getRestitution() {
        return restitution;
    }

    public void setRestitution(float restitution) {
        this.restitution = restitution;
    }

    public float getFriction() {
        return friction;
    }

    public void setFriction(float friction) {
        this.friction = friction;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean isStatic) {
        this.isStatic = isStatic;
        this.inverseMass = isStatic ? 0 : 1.0f / mass;
        this.inverseMomentOfInertia = isStatic ? 0 : 1.0f / momentOfInertia;
    }

    public Vector2f getVelocityDirect() {
        return velocity;
    }

    public Vector2f getAccumulatedForce() {
        return new Vector2f(accumulatedForce);
    }

    // Direct position access only for collision resolution
    public Vector2f getPositionForCollision() {
        if (!resolvingCollision) {
            logger.warn("Attempted to get direct position access outside collision resolution");
            return new Vector2f(position);  // Return copy if not in collision resolution
        }
        return position;
    }




    public void applyTorque(float torque) {
        if (!isStatic) {
            accumulatedTorque += torque;
            logger.trace("Applied torque: {}, total torque: {}",
                    torque, accumulatedTorque);
        }
    }

    public float getRotation() { return rotation; }
    public float getAngularVelocity() { return angularVelocity; }

    private float normalizeAngle(float angle) {
        while (angle < 0) angle += 2 * Math.PI;
        while (angle >= 2 * Math.PI) angle -= 2 * Math.PI;
        return angle;
    }
}