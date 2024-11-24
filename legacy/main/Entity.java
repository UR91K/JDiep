// Entity.java
package main;

import org.joml.Vector2f;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class Entity {
    // Change protected to public or provide getters/setters
    private UUID id;
    private EntityType type;
    Vector2f position;
    Vector2f velocity;
    float mass;
    private float health;
    private List<Component> components;
    private boolean isActive;
    float boundingRadius;

    public Entity(Vector2f position, float mass) {
        this.id = UUID.randomUUID();
        this.position = new Vector2f(position);
        this.velocity = new Vector2f(0, 0);
        this.mass = mass;
        this.health = mass * 10; // Base health scaling with mass
        this.components = new ArrayList<>();
        this.isActive = true;
    }

    public void update(float deltaTime) {
        if (!isActive) return;

        // Update all enabled components
        for (Component component : components) {
            if (component.isEnabled()) {
                component.update(deltaTime);
            }
        }
    }

    public void addComponent(Component component) {
        component.setOwner(this);
        components.add(component);
    }

    public <T extends Component> T getComponent(Class<T> componentClass) {
        for (Component component : components) {
            if (componentClass.isInstance(component)) {
                return componentClass.cast(component);
            }
        }
        return null;
    }

    public void onCollision(Entity other, CollisionInfo info) {
        for (Component component : components) {
            if (component.isEnabled()) {
                component.onCollision(other, info);
            }
        }
    }

    // Getters and setters
    public UUID getId() { return id; }

    public EntityType getType() { return type; }
    protected void setType(EntityType type) { this.type = type; }

    public Vector2f getPosition() { return new Vector2f(position); }
    public void setPosition(Vector2f newPos) { position.set(newPos); }

    public Vector2f getVelocity() { return new Vector2f(velocity); }
    public void setVelocity(Vector2f newVel) { velocity.set(newVel); }

    public float getMass() { return mass; }
    public void setMass(float newMass) {
        this.mass = newMass;
        this.health = newMass * 10; // Update health with mass
    }

    public float getBoundingRadius() { return boundingRadius; }
    protected void setBoundingRadius(float radius) { this.boundingRadius = radius; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }

    // Protected getters for direct field access by subclasses
    protected Vector2f getPositionDirect() { return position; }
    protected Vector2f getVelocityDirect() { return velocity; }
}