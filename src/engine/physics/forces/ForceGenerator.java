package engine.physics.forces;

import engine.physics.components.RigidBodyComponent;

/**
 * Interface for generating forces on physics bodies
 */
public interface ForceGenerator {
    void applyForce(RigidBodyComponent body, float dt);
}
