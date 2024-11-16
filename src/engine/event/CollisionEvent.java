// engine/event/CollisionEvent.java
package engine.event;

import engine.ecs.Entity;
import engine.physics.collision.ContactPoint;

public class CollisionEvent extends Event {
    private final Entity entityA;
    private final Entity entityB;
    private final ContactPoint contact;  // Changed from CollisionInfo

    public CollisionEvent(Entity entityA, Entity entityB, ContactPoint contact) {
        this.entityA = entityA;
        this.entityB = entityB;
        this.contact = contact;
    }

    public Entity getEntityA() { return entityA; }
    public Entity getEntityB() { return entityB; }
    public ContactPoint getContact() { return contact; }  // Changed from getCollisionInfo
}