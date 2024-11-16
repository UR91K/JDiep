package game.systems;

import engine.ecs.System;
import engine.ecs.Entity;
import engine.physics.components.Transform2DComponent;
import game.components.LocalPlayerControlComponent;
import game.entities.tanks.BaseTank;
import engine.core.logging.Logger;
import engine.physics.components.MovementComponent;
import engine.physics.components.RigidBodyComponent;
import org.joml.Vector2f;

public class TankControlSystem extends System {
    private static final Logger logger = Logger.getLogger(TankControlSystem.class);

    @Override
    public void update(float deltaTime) {
        var entities = world.getEntitiesWithComponents(
                LocalPlayerControlComponent.class,
                MovementComponent.class,
                RigidBodyComponent.class
        );

        logger.debug("Found {} entities with required components", entities.size());

        for (Entity entity : entities) {
            if (!(entity instanceof BaseTank)) {
                logger.warn("Entity {} has control components but is not a BaseTank",
                        entity.getClass().getSimpleName());
                continue;
            }

            BaseTank tank = (BaseTank)entity;
            LocalPlayerControlComponent control = entity.getComponent(LocalPlayerControlComponent.class);
            MovementComponent movement = entity.getComponent(MovementComponent.class);

            logger.debug("PRE-MOVEMENT - Tank position in transform: {}, in rigidbody: {}",
                    tank.getComponent(Transform2DComponent.class).getPosition(),
                    tank.getComponent(RigidBodyComponent.class).getPosition());

            if (!control.isEnabled()) {
                logger.trace("Control disabled for tank: {}", tank.getClass().getSimpleName());
                continue;
            }

            // Process input
            control.processInput();
            var moveDir = control.getMoveDirection();

            // Apply movement through tank
            if (moveDir.lengthSquared() > 0) {
                logger.debug("Applying tank movement - Direction: {}, Speed: {}",
                        moveDir, movement.getMoveSpeed());

                // Use tank's movement method instead of direct force application
                tank.applyMovement(moveDir);

                // Log movement state
                RigidBodyComponent body = tank.getComponent(RigidBodyComponent.class);
                logger.debug("Post movement state - Velocity: {}, Force: {}",
                        body.getVelocity(), body.getAccumulatedForce());
            }

            // Update rotation
            float targetRotation = control.getTargetRotation();
            float currentRotation = tank.getTurretRotation();
            if (Math.abs(targetRotation - currentRotation) > 0.01f) {
                logger.trace("Updating turret rotation from {} to {}",
                        currentRotation, targetRotation);
                tank.setTargetTurretRotation(targetRotation);
            }

            if (moveDir.lengthSquared() > 0) {
                tank.applyMovement(moveDir);
                logger.debug("POST-MOVEMENT - Tank position in transform: {}, in rigidbody: {}",
                        tank.getComponent(Transform2DComponent.class).getPosition(),
                        tank.getComponent(RigidBodyComponent.class).getPosition());
            }
        }
    }

    @Override
    public boolean isPhysicsSystem() {
        return false;
    }
}