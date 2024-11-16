package engine.physics.collision;

import engine.ecs.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Quadtree node for spatial partitioning
 */
public class QuadTreeNode {
    private static final int MAX_ENTITIES = 8;    // Maximum entities before splitting
    private static final int MAX_DEPTH = 6;       // Maximum tree depth
    private static final float MIN_SIZE = 50.0f;  // Minimum node size

    final Bounds bounds;
    private final int depth;
    private final List<Entity> entities;
    private QuadTreeNode[] children;
    private boolean isLeaf;

    public QuadTreeNode(Bounds bounds, int depth) {
        this.bounds = bounds;
        this.depth = depth;
        this.entities = new ArrayList<>();
        this.isLeaf = true;
    }

    public void insert(Entity entity, Bounds entityBounds) {
        // If we're not at a leaf, insert into appropriate children
        if (!isLeaf) {
            int index = getQuadrant(entityBounds);
            if (index != -1) {
                children[index].insert(entity, entityBounds);
                return;
            }
        }

        // Add to this node's entities
        entities.add(entity);

        // Split if necessary
        if (isLeaf && entities.size() > MAX_ENTITIES && depth < MAX_DEPTH &&
                bounds.halfWidth > MIN_SIZE) {
            split();
        }
    }

    private void split() {
        isLeaf = false;
        float quarterWidth = bounds.halfWidth * 0.5f;
        float quarterHeight = bounds.halfHeight * 0.5f;

        children = new QuadTreeNode[4];
        children[0] = new QuadTreeNode(
                new Bounds(bounds.x - quarterWidth, bounds.y + quarterHeight,
                        quarterWidth, quarterHeight),
                depth + 1
        );
        children[1] = new QuadTreeNode(
                new Bounds(bounds.x + quarterWidth, bounds.y + quarterHeight,
                        quarterWidth, quarterHeight),
                depth + 1
        );
        children[2] = new QuadTreeNode(
                new Bounds(bounds.x - quarterWidth, bounds.y - quarterHeight,
                        quarterWidth, quarterHeight),
                depth + 1
        );
        children[3] = new QuadTreeNode(
                new Bounds(bounds.x + quarterWidth, bounds.y - quarterHeight,
                        quarterWidth, quarterHeight),
                depth + 1
        );

        // Redistribute entities to children
        List<Entity> oldEntities = new ArrayList<>(entities);
        entities.clear();

        for (Entity entity : oldEntities) {
            Bounds entityBounds = getBoundsForEntity(entity);
            int index = getQuadrant(entityBounds);
            if (index != -1) {
                children[index].insert(entity, entityBounds);
            } else {
                entities.add(entity);
            }
        }
    }

    private int getQuadrant(Bounds entityBounds) {
        if (!bounds.containsCompletely(entityBounds)) {
            return -1;
        }

        boolean top = entityBounds.y > bounds.y;
        boolean left = entityBounds.x < bounds.x;

        if (top && left) return 0;
        if (top && !left) return 1;
        if (!top && left) return 2;
        return 3;
    }

    public void getPotentialCollisions(List<CollisionPair> pairs, Bounds checkBounds) {
        // Check against entities in this node
        for (int i = 0; i < entities.size(); i++) {
            Entity entityA = entities.get(i);
            Bounds boundsA = getBoundsForEntity(entityA);

            // Check against other entities in this node
            for (int j = i + 1; j < entities.size(); j++) {
                Entity entityB = entities.get(j);
                Bounds boundsB = getBoundsForEntity(entityB);

                if (boundsA.intersects(boundsB)) {
                    pairs.add(new CollisionPair(entityA, entityB));
                }
            }
        }

        // If we're not a leaf, check children
        if (!isLeaf) {
            for (QuadTreeNode child : children) {
                if (child.bounds.intersects(checkBounds)) {
                    child.getPotentialCollisions(pairs, checkBounds);
                }
            }
        }
    }

    public void clear() {
        entities.clear();
        if (!isLeaf) {
            for (QuadTreeNode child : children) {
                child.clear();
            }
        }
    }

    private Bounds getBoundsForEntity(Entity entity) {
        // Get AABB from entity's collision shapes
        // This would need to be implemented based on your specific needs
        return null; // Placeholder
    }
}
