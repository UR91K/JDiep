package engine.physics.systems;

import engine.ecs.Entity;
import engine.ecs.System;
import engine.event.CollisionEvent;
import engine.event.EventBus;
import engine.physics.components.CollisionComponent;
import engine.physics.components.Transform2DComponent;
import engine.physics.shapes.CircleShape;
import engine.physics.shapes.RegularPolygonShape;
import engine.physics.shapes.Shape;
import engine.physics.collision.GJKCollision;
import engine.physics.collision.ContactPoint;
import engine.physics.collision.Simplex;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class CollisionSystem extends System {
    private final List<CollisionPair> collisionPairs = new ArrayList<>();
    private final EventBus eventBus;
    private final GJKCollision gjk;

    public CollisionSystem(EventBus eventBus) {
        this.eventBus = eventBus;
        this.gjk = new GJKCollision();
    }

    @Override
    public void update(float deltaTime) {
        collisionPairs.clear();

        // Get all entities with collision components
        var entities = world.getEntitiesWithComponents(
                CollisionComponent.class,
                Transform2DComponent.class
        );

        // Broad phase: collect potential collisions using bounding radius
        for (Entity entityA : entities) {
            for (Entity entityB : entities) {
                // Skip self and already checked pairs
                if (entityA == entityB || isPairChecked(entityA, entityB)) {
                    continue;
                }

                CollisionComponent colliderA = entityA.getComponent(CollisionComponent.class);
                CollisionComponent colliderB = entityB.getComponent(CollisionComponent.class);

                // Skip if collision layers don't interact
                if (!colliderA.canCollideWith(colliderB)) {
                    continue;
                }

                Transform2DComponent transformA = entityA.getComponent(Transform2DComponent.class);
                Transform2DComponent transformB = entityB.getComponent(Transform2DComponent.class);

                // Broad phase check using bounding radii
                float radiusSum = getBoundingRadius(colliderA) + getBoundingRadius(colliderB);
                if (transformA.getPosition().distance(transformB.getPosition()) <= radiusSum) {
                    collisionPairs.add(new CollisionPair(entityA, entityB));
                }
            }
        }

        // Narrow phase: detailed collision detection using GJK/EPA
        for (CollisionPair pair : collisionPairs) {
            CollisionComponent colliderA = pair.entityA.getComponent(CollisionComponent.class);
            CollisionComponent colliderB = pair.entityB.getComponent(CollisionComponent.class);

            // Check all shape pairs for collision
            for (Shape shapeA : colliderA.getColliders()) {
                for (Shape shapeB : colliderB.getColliders()) {
                    // Use GJK to detect collision
                    Simplex simplex = gjk.getIntersectionSimplex(
                            dir -> getSupport(shapeA, dir),
                            dir -> getSupport(shapeB, dir)
                    );

                    if (simplex != null) {
                        // Use EPA to get contact information
                        ContactPoint contact = gjk.getContactPoint(
                                dir -> getSupport(shapeA, dir),
                                dir -> getSupport(shapeB, dir),
                                simplex
                        );

                        if (contact != null && !colliderA.isTrigger() && !colliderB.isTrigger()) {
                            // Update contact with the actual entities
                            ContactPoint finalContact = new ContactPoint(
                                    pair.entityA,
                                    pair.entityB,
                                    contact.point,
                                    contact.normal,
                                    contact.penetration
                            );
                            eventBus.emit(new CollisionEvent(pair.entityA, pair.entityB, finalContact));
                        }
                    }
                }
            }
        }
    }

    private Vector2f getSupport(Shape shape, Vector2f direction) {
        if (shape instanceof CircleShape) {
            return getSupportCircle((CircleShape) shape, direction);
        } else if (shape instanceof RegularPolygonShape) {
            return getSupportPolygon((RegularPolygonShape) shape, direction);
        }
        throw new UnsupportedOperationException("Unsupported shape type: " + shape.getClass());
    }

    private Vector2f getSupportCircle(CircleShape circle, Vector2f direction) {
        Vector2f normalizedDir = new Vector2f(direction).normalize();
        return new Vector2f(circle.getCenter()).add(
                normalizedDir.mul(circle.getRadius())
        );
    }

    private Vector2f getSupportPolygon(RegularPolygonShape polygon, Vector2f direction) {
        Vector2f furthestPoint = null;
        float maxDistance = Float.NEGATIVE_INFINITY;

        for (Vector2f vertex : polygon.getWorldVertices()) {
            float distance = vertex.dot(direction);
            if (distance > maxDistance) {
                maxDistance = distance;
                furthestPoint = vertex;
            }
        }

        return new Vector2f(furthestPoint);
    }

    private boolean isPairChecked(Entity a, Entity b) {
        for (CollisionPair pair : collisionPairs) {
            if ((pair.entityA == a && pair.entityB == b) ||
                    (pair.entityA == b && pair.entityB == a)) {
                return true;
            }
        }
        return false;
    }

    private float getBoundingRadius(CollisionComponent collider) {
        float maxRadius = 0;
        for (Shape shape : collider.getColliders()) {
            maxRadius = Math.max(maxRadius, shape.getBoundingRadius());
        }
        return maxRadius;
    }

    private static class CollisionPair {
        final Entity entityA;
        final Entity entityB;

        CollisionPair(Entity entityA, Entity entityB) {
            this.entityA = entityA;
            this.entityB = entityB;
        }
    }
}