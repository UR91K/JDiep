package engine.physics.collision;

import engine.ecs.Entity;
import java.util.Objects;

/**
 * Represents a potential collision between two entities.
 * Used in both broad and narrow phase collision detection.
 */
public class CollisionPair {
    public final Entity entityA;
    public final Entity entityB;

    public CollisionPair(Entity entityA, Entity entityB) {
        // Always store entities in consistent order for proper equality checks
        if (entityA.getId().compareTo(entityB.getId()) <= 0) {
            this.entityA = entityA;
            this.entityB = entityB;
        } else {
            this.entityA = entityB;
            this.entityB = entityA;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CollisionPair that = (CollisionPair) o;
        return Objects.equals(entityA, that.entityA) &&
                Objects.equals(entityB, that.entityB);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityA, entityB);
    }
}