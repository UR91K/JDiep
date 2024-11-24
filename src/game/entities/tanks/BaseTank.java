package game.entities.tanks;

import engine.core.logging.Logger;
import engine.ecs.Entity;
import engine.physics.components.*;
import engine.physics.shapes.*;
import engine.event.EventBus;
import game.components.TankRendererComponent;
import org.joml.Vector2f;

/**
 * Base class for all tank entities. Handles core tank physics and behaviors.
 */
public abstract class BaseTank extends Entity {
    private static final Logger logger = Logger.getLogger(BaseTank.class);

    protected final Transform2DComponent transform;
    protected final RigidBodyComponent bodyRigidbody;
    protected final RigidBodyComponent turretRigidbody;
    protected final CollisionComponent collision;
    protected final MovementComponent movement;
    protected final TankRendererComponent renderer;  // Add reference to renderer

    protected final CircleShape bodyShape;
    protected final PolygonShape turretShape;

    protected float targetTurretRotation = 0.0f;
    private float currentTurretRotation = 0.0f; // Track actual rotation
    protected final TankStats stats;
    private Vector2f lastPhysicsPosition = new Vector2f();


    protected BaseTank(Vector2f position, TankStats stats) {
        this.stats = stats;

        // Create core components
        transform = addComponent(new Transform2DComponent(position));

        // Body physics setup
        bodyRigidbody = addComponent(new RigidBodyComponent(stats.mass));
        bodyRigidbody.setPosition(position);
        bodyRigidbody.setFriction(stats.friction);
        bodyRigidbody.setRestitution(stats.restitution);

        // Turret physics setup
        turretRigidbody = addComponent(new RigidBodyComponent(stats.mass * stats.turretMassRatio));
        turretRigidbody.setPosition(position);
        updateTurretInertia();

        // Movement component
        movement = addComponent(new MovementComponent(
                stats.moveSpeed,
                stats.friction,
                stats.mass,
                stats.restitution
        ));

        // Collision setup
        collision = addComponent(new CollisionComponent());
        bodyShape = createBodyShape();
        turretShape = createTurretShape();
        collision.addCollider(bodyShape);
        collision.addCollider(turretShape);

        // Add renderer component - add this at the end after all other components are set up
        renderer = addComponent(new TankRendererComponent(this));
    }

    protected void updateTurretInertia() {
        // Calculate moment of inertia for the turret rectangle
        float width = stats.radius * stats.turretWidthRatio;
        float length = stats.radius * stats.turretLengthRatio;
        float inertia = (turretRigidbody.getMass() * (width * width + length * length)) / 12.0f;
        turretRigidbody.updateInertia(inertia);
    }

    protected CircleShape createBodyShape() {
        CircleShape shape = new CircleShape(stats.radius);
        shape.setCenter(transform.getPosition());
        return shape;
    }

    protected PolygonShape createTurretShape() {
        float width = stats.radius * stats.turretWidthRatio;
        float length = stats.radius * stats.turretLengthRatio;

        // Calculate turret offset from tank edge to prevent visual overlap
        float offset = calculateTurretOffset(stats.radius, width);
        float startX = stats.radius - offset;
        float endX = startX + length;

        // Create turret vertices
        Vector2f[] vertices = new Vector2f[4];
        vertices[0] = new Vector2f(startX, -width/2);
        vertices[1] = new Vector2f(endX, -width/2);
        vertices[2] = new Vector2f(endX, width/2);
        vertices[3] = new Vector2f(startX, width/2);

        PolygonShape shape = new PolygonShape(vertices);
        shape.setCenter(transform.getPosition());
        return shape;
    }

    protected float calculateTurretOffset(float radius, float width) {
        // Use Pythagorean theorem to calculate offset where turret meets circle
        return radius - (float)Math.sqrt(radius * radius - (width/2) * (width/2));
    }

    public void update(float deltaTime) {
        logger.debug("START TANK UPDATE - transform pos: {}, rigidbody pos: {}",
                transform.getPosition(), bodyRigidbody.getPosition());

        // Important: Don't get position from physics body, use the transform's position
        // since PhysicsSystem has already synced it
        Vector2f currentPos = transform.getPosition();

        // Update collision shapes with current position
        bodyShape.setCenter(new Vector2f(currentPos));
        turretShape.setCenter(new Vector2f(currentPos));

        // Handle turret rotation
        updateTurretRotation(deltaTime);

        logger.debug("END TANK UPDATE - final transform pos: {}, final rigidbody pos: {}",
                transform.getPosition(), bodyRigidbody.getPosition());
    }

    protected void updateTurretRotation(float deltaTime) {
        // Calculate angle difference, handling wraparound
        float angleDiff = getShortestAngleDifference(currentTurretRotation, targetTurretRotation);

        if (Math.abs(angleDiff) > 0.001f) {
            logger.trace("Turret rotation - current: {}, target: {}, diff: {}",
                    currentTurretRotation, targetTurretRotation, angleDiff);

            // Spring-damper model for turret rotation
            float springTorque = stats.turretSpringStiffness * angleDiff;
            float dampingTorque = -stats.turretDamping * turretRigidbody.getAngularVelocity();
            float totalTorque = springTorque + dampingTorque;

            // Clamp torque
            totalTorque = Math.max(-stats.turretMaxTorque,
                    Math.min(totalTorque, stats.turretMaxTorque));

            // Apply torque to turret rigidbody
            turretRigidbody.applyTorque(totalTorque);

            // Update current rotation from physics
            currentTurretRotation = turretRigidbody.getRotation();

            // Normalize rotation to [0, 2π]
            currentTurretRotation = normalizeAngle(currentTurretRotation);

            logger.trace("Applied torque: {}, new rotation: {}",
                    totalTorque, currentTurretRotation);
        }
    }

    protected void applyTurretReactionForces(float turretTorque, float deltaTime) {
        if (Math.abs(turretTorque) < 0.001f) return;

        // Calculate force points and directions
        float turretLength = stats.radius * stats.turretLengthRatio;
        float rotation = turretRigidbody.getRotation();

        Vector2f turretDir = new Vector2f(
                (float)Math.cos(rotation),
                (float)Math.sin(rotation)
        );

        Vector2f position = transform.getPosition();

        // Calculate force application points
        Vector2f turretTip = new Vector2f(position).add(
                new Vector2f(turretDir).mul(turretLength)
        );

        // Calculate force magnitude - divide torque by lever arm length
        float forceMagnitude = turretTorque / (turretLength / 2.0f);

        // Calculate perpendicular force directions
        Vector2f forceDir = new Vector2f(-turretDir.y, turretDir.x);

        // Apply forces at turret tip and base
        Vector2f tipForce = new Vector2f(forceDir).mul(forceMagnitude);
        Vector2f baseForce = new Vector2f(forceDir).mul(-forceMagnitude);

        bodyRigidbody.applyForceAtPoint(tipForce, turretTip);
        bodyRigidbody.applyForceAtPoint(baseForce, position);
    }

    public void setTargetTurretRotation(float rotation) {
        // Normalize incoming rotation
        rotation = normalizeAngle(rotation);

        if (Math.abs(rotation - targetTurretRotation) > 0.001f) {
            logger.trace("Setting target turret rotation from {} to {}",
                    targetTurretRotation, rotation);
            targetTurretRotation = rotation;
        }
    }

    private float normalizeAngle(float angle) {
        while (angle < 0) angle += 2 * Math.PI;
        while (angle >= 2 * Math.PI) angle -= 2 * Math.PI;
        return angle;
    }

    protected float getShortestAngleDifference(float current, float target) {
        float diff = target - current;
        while (diff > Math.PI) diff -= 2 * Math.PI;
        while (diff < -Math.PI) diff += 2 * Math.PI;
        return diff;
    }

    public void aimTurretAt(Vector2f target) {
        Vector2f toTarget = new Vector2f(target).sub(transform.getPosition());
        this.targetTurretRotation = (float)Math.atan2(toTarget.y, toTarget.x);
    }

    public void applyMovement(Vector2f direction) {
        if (direction.lengthSquared() > 0) {
            logger.trace("Applying movement direction: {}", direction);
            Vector2f force = new Vector2f(direction)
                    .normalize()
                    .mul(stats.moveSpeed * bodyRigidbody.getMass());

            bodyRigidbody.applyForce(force);
            movement.setDesiredForce(force);

            logger.trace("Applied force: {}", force);
        }
    }

    /**
     * Returns whether this tank is a dummy tank
     */
    public boolean isDummy() {
        return false;  // Default to false, override in DummyTank
    }

    // Getters
    public Vector2f getPosition() { return transform.getPosition(); }
    public Vector2f getVelocity() { return bodyRigidbody.getVelocity(); }
    public float getTurretRotation() { return turretRigidbody.getRotation(); }
    public float getTurretAngularVelocity() { return turretRigidbody.getAngularVelocity(); }
    public TankStats getStats() { return stats; }
}