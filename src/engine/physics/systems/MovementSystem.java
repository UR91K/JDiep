package engine.physics.systems;

import engine.ecs.Entity;
import engine.ecs.System;
import engine.physics.components.MovementComponent;
import engine.physics.components.Transform2DComponent;
import org.joml.Vector2f;

public class MovementSystem extends System {
    @Override
    public void update(float deltaTime) {
        for (Entity entity : world.getEntitiesWithComponents(
                MovementComponent.class, Transform2DComponent.class)) {
            MovementComponent movement = entity.getComponent(MovementComponent.class);
            Transform2DComponent transform = entity.getComponent(Transform2DComponent.class);

            // Apply friction
            Vector2f velocity = movement.getVelocityDirect();
            if (velocity.lengthSquared() > 0) {
                float speed = velocity.length();
                float drop = speed * movement.getFriction() * deltaTime;
                float newSpeed = Math.max(0, speed - drop);
                if (newSpeed != speed) {
                    velocity.mul(newSpeed / speed);
                }
            }

            // Update position
            transform.getPositionDirect().add(
                    new Vector2f(velocity).mul(deltaTime)
            );
        }
    }
}