package engine.physics.collision;

import engine.core.logging.Logger;
import engine.ecs.Entity;
import engine.ecs.System;
import engine.event.CollisionEvent;
import engine.event.EventBus;
import engine.physics.components.CollisionComponent;
import engine.physics.components.RigidBodyComponent;
import engine.physics.components.Transform2DComponent;
import engine.physics.collision.Simplex;
import engine.physics.shapes.CircleShape;
import engine.physics.shapes.RegularPolygonShape;
import engine.physics.shapes.Shape;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * Main collision detection system using quadtree for broad phase
 */
public class CollisionSystem extends System {
    private final QuadTreeNode quadTree;
    private final List<CollisionPair> potentialCollisions;
    private final EventBus eventBus;
    private final GJKCollision gjk;
    private static final Logger logger = Logger.getLogger(CollisionSystem.class);

    public CollisionSystem(EventBus eventBus) {
        // Initialize with world bounds
        this.quadTree = new QuadTreeNode(
                new Bounds(0, 0, 500, 500),  // Adjust size based on your world
                0
        );
        this.potentialCollisions = new ArrayList<>();
        this.eventBus = eventBus;
        this.gjk = new GJKCollision();
    }

    @Override
    public void update(float dt) {
        try {

            // Clear and rebuild quadtree
            quadTree.clear();
            var entities = world.getEntitiesWithComponents(
                    CollisionComponent.class,
                    Transform2DComponent.class
            );

            // Insert entities into quadtree
            for (Entity entity : entities) {
                Bounds bounds = calculateEntityBounds(entity);
                quadTree.insert(entity, bounds);
            }

            // Get potential collisions from quadtree
            potentialCollisions.clear();
            quadTree.getPotentialCollisions(potentialCollisions, quadTree.bounds);

            // Narrow phase using GJK
            for (CollisionPair pair : potentialCollisions) {
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
                            // Use EPA to get base contact information
                            ContactPoint baseContact = gjk.getContactPoint(
                                    dir -> getSupport(shapeA, dir),
                                    dir -> getSupport(shapeB, dir),
                                    simplex
                            );

                            if (baseContact != null && !colliderA.isTrigger() && !colliderB.isTrigger()) {
                                // Create full contact point with entities
                                ContactPoint finalContact = new ContactPoint(
                                        pair.entityA,
                                        pair.entityB,
                                        baseContact.point,
                                        baseContact.normal,
                                        baseContact.penetration
                                );
                                eventBus.emit(new CollisionEvent(pair.entityA, pair.entityB, finalContact));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error during collision detection", e);
        }
    }

    private boolean checkCollision(CollisionPair pair) {
        // Use GJK to check for actual collision
        CollisionComponent colliderA = pair.entityA.getComponent(CollisionComponent.class);
        CollisionComponent colliderB = pair.entityB.getComponent(CollisionComponent.class);

        // Skip if collision layers don't interact
        if (!colliderA.canCollideWith(colliderB)) {
            return false;
        }

        // For each shape pair, use GJK
        for (Shape shapeA : colliderA.getColliders()) {
            for (Shape shapeB : colliderB.getColliders()) {
                if (gjk.intersect(
                        dir -> getSupport(shapeA, dir),
                        dir -> getSupport(shapeB, dir)
                )) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Gets the support point for a shape in a given direction
     */
    private Vector2f getSupport(Shape shape, Vector2f direction) {
        if (shape instanceof CircleShape) {
            return getSupportCircle((CircleShape) shape, direction);
        } else if (shape instanceof RegularPolygonShape) {
            return getSupportPolygon((RegularPolygonShape) shape, direction);
        }
        throw new UnsupportedOperationException("Unsupported shape type: " + shape.getClass());
    }

    private Vector2f getSupportCircle(CircleShape circle, Vector2f direction) {
        // For a circle, the support point is the center plus the normalized direction times radius
        Vector2f normalizedDir = new Vector2f(direction).normalize();
        return new Vector2f(circle.getCenter()).add(
                normalizedDir.mul(circle.getRadius())
        );
    }

    private Vector2f getSupportPolygon(RegularPolygonShape polygon, Vector2f direction) {
        Vector2f furthestPoint = null;
        float maxDistance = Float.NEGATIVE_INFINITY;

        // Check all vertices to find the furthest in the given direction
        for (Vector2f vertex : polygon.getWorldVertices()) {
            float distance = vertex.dot(direction);
            if (distance > maxDistance) {
                maxDistance = distance;
                furthestPoint = vertex;
            }
        }

        return new Vector2f(furthestPoint);
    }

    /**
     * Generates contact information for colliding shapes
     */
    private ContactPoint generateContact(CollisionPair pair) {
        CollisionComponent colliderA = pair.entityA.getComponent(CollisionComponent.class);
        CollisionComponent colliderB = pair.entityB.getComponent(CollisionComponent.class);

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

                    if (contact != null) {
                        return new ContactPoint(
                                pair.entityA,
                                pair.entityB,
                                contact.point,
                                contact.normal,
                                contact.penetration
                        );
                    }
                }
            }
        }

        return null;
    }

    /**
     * Calculates the AABB bounds for an entity based on its collision shapes
     */
    private Bounds calculateEntityBounds(Entity entity) {
        CollisionComponent collision = entity.getComponent(CollisionComponent.class);
        Transform2DComponent transform = entity.getComponent(Transform2DComponent.class);

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;

        // Check each shape's bounds
        for (Shape shape : collision.getColliders()) {
            if (shape instanceof CircleShape) {
                CircleShape circle = (CircleShape) shape;
                Vector2f center = circle.getCenter();
                float radius = circle.getRadius();

                minX = Math.min(minX, center.x - radius);
                minY = Math.min(minY, center.y - radius);
                maxX = Math.max(maxX, center.x + radius);
                maxY = Math.max(maxY, center.y + radius);
            }
            else if (shape instanceof RegularPolygonShape) {
                RegularPolygonShape polygon = (RegularPolygonShape) shape;

                // Check all vertices
                for (Vector2f vertex : polygon.getWorldVertices()) {
                    minX = Math.min(minX, vertex.x);
                    minY = Math.min(minY, vertex.y);
                    maxX = Math.max(maxX, vertex.x);
                    maxY = Math.max(maxY, vertex.y);
                }
            }
        }

        // Add a small padding for numerical stability
        float padding = 0.1f;
        minX -= padding;
        minY -= padding;
        maxX += padding;
        maxY += padding;

        // Calculate center and half-size for Bounds
        float centerX = (minX + maxX) * 0.5f;
        float centerY = (minY + maxY) * 0.5f;
        float halfWidth = (maxX - minX) * 0.5f;
        float halfHeight = (maxY - minY) * 0.5f;

        return new Bounds(centerX, centerY, halfWidth, halfHeight);
    }

    // Debug helper method to visualize bounds (optional)
    private void debugDrawBounds(Bounds bounds) {
        // This would be implemented with your rendering system
        // Draw the AABB as a rectangle
        float left = bounds.x - bounds.halfWidth;
        float right = bounds.x + bounds.halfWidth;
        float top = bounds.y + bounds.halfHeight;
        float bottom = bounds.y - bounds.halfHeight;

        // Draw lines connecting (left,top) (right,top) (right,bottom) (left,bottom)
        // ... rendering code ...
    }
}

