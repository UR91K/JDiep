package engine.physics.forces;

import engine.physics.components.RigidBodyComponent;
import org.joml.Vector2f;

/**
 * Applies velocity-dependent drag force
 */
public class TankDragForce implements ForceGenerator {
    private final float linearDragCoeff;
    private final float rotationalDragCoeff;

    public TankDragForce(float linearDrag, float rotationalDrag) {
        this.linearDragCoeff = linearDrag;
        this.rotationalDragCoeff = rotationalDrag;
    }

    @Override
    public void applyForce(RigidBodyComponent body, float dt) {
        // Linear drag (proportional to velocity squared)
        Vector2f velocity = body.getVelocity();
        float speedSquared = velocity.lengthSquared();

        if (speedSquared > 0) {
            Vector2f dragForce = new Vector2f(velocity)
                    .normalize()
                    .mul(-speedSquared * linearDragCoeff);
            body.applyForce(dragForce);  // Just uses single argument version
        }

        // Rotational drag
        float angularVel = body.getAngularVelocity();
        if (angularVel != 0) {
            float torque = -Math.signum(angularVel) *
                    angularVel * angularVel *
                    rotationalDragCoeff;
            body.applyTorque(torque);
        }
    }
}
