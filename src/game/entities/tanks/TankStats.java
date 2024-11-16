package game.entities.tanks;

/**
 * Configuration class for tank properties. Used to define different tank types.
 */
public class TankStats {
    // Body properties
    public float mass = 1.0f;
    public float radius = 3.0f;
    public float moveSpeed = 27.0f;
    public float friction = 1.0f;
    public float restitution = 0.5f;

    // Turret properties
    public float turretMassRatio = 0.3f;     // Ratio of turret mass to body mass
    public float turretWidthRatio = 1f;
    public float turretLengthRatio = 1f;
    public float turretMaxTorque = 1000.0f;
    public float turretSpringStiffness = 80.0f;
    public float turretDamping = 0.0f;

    // Builder pattern methods
    public TankStats setMass(float mass) {
        this.mass = mass;
        return this;
    }

    public TankStats setRadius(float radius) {
        this.radius = radius;
        return this;
    }

    public TankStats setMovement(float speed, float friction) {
        this.moveSpeed = speed;
        this.friction = friction;
        return this;
    }

    public TankStats setTurret(float widthRatio, float lengthRatio, float massRatio) {
        this.turretWidthRatio = widthRatio;
        this.turretLengthRatio = lengthRatio;
        this.turretMassRatio = massRatio;
        return this;
    }

    public TankStats setTurretPhysics(float maxTorque, float stiffness, float damping) {
        this.turretMaxTorque = maxTorque;
        this.turretSpringStiffness = stiffness;
        this.turretDamping = damping;
        return this;
    }

    public static TankStats clone(TankStats other) {
        TankStats stats = new TankStats();
        stats.mass = other.mass;
        stats.radius = other.radius;
        stats.moveSpeed = other.moveSpeed;
        stats.friction = other.friction;
        stats.restitution = other.restitution;
        stats.turretMassRatio = other.turretMassRatio;
        stats.turretWidthRatio = other.turretWidthRatio;
        stats.turretLengthRatio = other.turretLengthRatio;
        stats.turretMaxTorque = other.turretMaxTorque;
        stats.turretSpringStiffness = other.turretSpringStiffness;
        stats.turretDamping = other.turretDamping;
        return stats;
    }
}