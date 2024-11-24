package engine.physics.systems;

import engine.core.logging.Logger;
import engine.ecs.Entity;
import engine.ecs.System;
import engine.event.CollisionEvent;
import engine.event.EventBus;
import engine.physics.collision.CollisionResolver;
import engine.physics.collision.ContactPoint;
import engine.physics.components.MovementComponent;
import engine.physics.components.RigidBodyComponent;
import engine.physics.components.Transform2DComponent;
import engine.physics.components.CollisionComponent;
import engine.physics.forces.ForceGenerator;
import engine.physics.shapes.Shape;
import game.entities.tanks.BaseTank;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Main physics simulation system
 */
public class PhysicsSystem extends System {
    private static final float MIN_DELTA_TIME = 1.0f / 60.0f;
    private static final int MAX_ITERATIONS = 10;

    private final EventBus eventBus;
    private final CollisionResolver collisionResolver;
    private final List<ForceGenerator> forceGenerators;
    private final List<ContactPoint> contacts;

    private static final Logger logger = Logger.getLogger(PhysicsSystem.class);

    public PhysicsSystem(EventBus eventBus) {
        this.eventBus = eventBus;
        this.collisionResolver = new CollisionResolver();
        this.forceGenerators = new ArrayList<>();
        this.contacts = new ArrayList<>();

        // Add default force generators (gravity, drag, etc)
        initializeForceGenerators();
    }

    private void initializeForceGenerators() {
        forceGenerators.add(new ForceGenerator() {
            private static final float DRAG_COEFFICIENT = 0.1f;

            @Override
            public void applyForce(RigidBodyComponent body, float dt) {
                float velSquared = body.getVelocity().lengthSquared();
                if (velSquared > 0) {
                    Vector2f dragForce = new Vector2f(body.getVelocity())
                            .normalize()
                            .mul(-velSquared * DRAG_COEFFICIENT);
                    body.applyForce(dragForce);  // Just uses single argument version
                }
            }
        });
    }

    @Override
    public void update(float dt) {
        dt = Math.min(dt, MIN_DELTA_TIME);

        var bodies = world.getEntitiesWithComponents(
                RigidBodyComponent.class,
                Transform2DComponent.class
        );

        logger.debug("Physics update - dt: {}, bodies: {}", dt, bodies.size());

        // Pre-integration updates
        for (Entity entity : bodies) {
            RigidBodyComponent body = entity.getComponent(RigidBodyComponent.class);
            Transform2DComponent transform = entity.getComponent(Transform2DComponent.class);

            // Log current state
            logger.trace("Pre-update {} - pos: {}, vel: {}, force: {}",
                    entity.getClass().getSimpleName(),
                    body.getPosition(),
                    body.getVelocity(),
                    body.getAccumulatedForce());
        }

        // Update control forces
        updateControlForces(dt);

        // Physics integration
        for (Entity entity : bodies) {
            RigidBodyComponent body = entity.getComponent(RigidBodyComponent.class);
            Transform2DComponent transform = entity.getComponent(Transform2DComponent.class);

            body.integrate(dt);
            transform.setPosition(body.getPosition());

            logger.debug("Post-integration state - pos: {}, vel: {}, force: {}",
                    body.getPosition(), body.getVelocity(), body.getAccumulatedForce());
        }
    }


    private void detectCollisions(Set<Entity> bodies) {
        // Simple N^2 collision detection for now
        // Can be optimized with spatial partitioning later
        for (Entity entityA : bodies) {
            for (Entity entityB : bodies) {
                if (entityA == entityB) continue;

                CollisionComponent colliderA = entityA.getComponent(CollisionComponent.class);
                CollisionComponent colliderB = entityB.getComponent(CollisionComponent.class);

                // Check collision layers
                if (!colliderA.canCollideWith(colliderB)) continue;

                // Check each shape pair
                for (Shape shapeA : colliderA.getColliders()) {
                    for (Shape shapeB : colliderB.getColliders()) {
                        if (shapeA.intersects(shapeB)) {
                            ContactPoint contact = generateContact(
                                    entityA, entityB, shapeA, shapeB);
                            if (contact != null) {
                                contacts.add(contact);
                                eventBus.emit(new CollisionEvent(
                                        entityA, entityB, contact));
                            }
                        }
                    }
                }
            }
        }
    }

    private ContactPoint generateContact(Entity entityA, Entity entityB,
                                         Shape shapeA, Shape shapeB) {
        // Simple contact generation for now
        // Can be improved with GJK/EPA algorithms later
        Vector2f centerA = shapeA.getCenter();
        Vector2f centerB = shapeB.getCenter();

        Vector2f normal = new Vector2f(centerB).sub(centerA);
        float distance = normal.length();

        if (distance < 0.0001f) {
            // Centers too close, use fallback normal
            normal.set(1, 0);
        } else {
            normal.div(distance); // Normalize
        }

        float penetration = (shapeA.getBoundingRadius() + shapeB.getBoundingRadius())
                - distance;

        if (penetration > 0) {
            Vector2f contact = new Vector2f(centerA).add(
                    new Vector2f(normal).mul(shapeA.getBoundingRadius())
            );

            return new ContactPoint(entityA, entityB, contact, normal, penetration);
        }

        return null;
    }

    private void updateControlForces(float dt) {
        var controlledEntities = world.getEntitiesWithComponents(
                MovementComponent.class,
                RigidBodyComponent.class
        );

        for (Entity entity : controlledEntities) {
            MovementComponent movement = entity.getComponent(MovementComponent.class);
            RigidBodyComponent body = entity.getComponent(RigidBodyComponent.class);

            // Apply current movement force
            Vector2f force = movement.getCurrentForce();
            if (force.lengthSquared() > 0) {
                logger.debug("Applying movement force {} to {}",
                        force, entity.getClass().getSimpleName());
                body.applyForce(force);
            }

            // Add damping force to slow down when no input
            Vector2f velocity = body.getVelocity();
            if (velocity.lengthSquared() > 0) {
                float dampingCoeff = 3.0f; // Adjust this value to control how quickly it stops
                Vector2f dampingForce = new Vector2f(velocity).mul(-dampingCoeff * body.getMass());
                body.applyForce(dampingForce);
                logger.debug("Applied damping force {} to velocity {}",
                        dampingForce, velocity);
            }
        }
    }


    public void addForceGenerator(ForceGenerator generator) {
        forceGenerators.add(generator);
    }

    public void removeForceGenerator(ForceGenerator generator) {
        forceGenerators.remove(generator);
    }

    @Override
    public boolean isPhysicsSystem() {
        return true;
    }
}