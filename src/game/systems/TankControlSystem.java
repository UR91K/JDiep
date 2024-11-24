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

        for (Entity entity : entities) {
            BaseTank tank = (BaseTank)entity;
            LocalPlayerControlComponent control = entity.getComponent(LocalPlayerControlComponent.class);
            MovementComponent movement = entity.getComponent(MovementComponent.class);

            // Process input and get move direction
            control.processInput();
            var moveDir = control.getMoveDirection();

            // IMPORTANT: Clear the movement force if no input
            if (moveDir.lengthSquared() > 0) {
                tank.applyMovement(moveDir);
            } else {
                // Reset movement when no input
                movement.setDesiredForce(new Vector2f());
            }

            // Log movement state
            logger.debug("Tank movement state - moveDir: {}, vel: {}, force: {}",
                    moveDir,
                    tank.getComponent(RigidBodyComponent.class).getVelocity(),
                    movement.getCurrentForce());
        }
    }

    @Override
    public boolean isPhysicsSystem() {
        return false;
    }
}