package engine.ecs;

import engine.core.logging.Logger;

import java.util.*;

public class Entity {
    private static final Logger logger = Logger.getLogger(Entity.class);
    private final UUID id;
    private final Map<Class<? extends Component>, Component> components;
    private boolean active = true;
    private World world;

    public Entity() {
        this.id = UUID.randomUUID();
        this.components = new HashMap<>();
    }

    public <T extends Component> T addComponent(T component) {
        component.setOwner(this);

        // Store component under its concrete class
        components.put(component.getClass(), component);

        // Also store under parent classes up to Component
        Class<?> currentClass = component.getClass().getSuperclass();
        while (Component.class.isAssignableFrom(currentClass)) {
            components.put((Class<? extends Component>) currentClass, component);
            currentClass = currentClass.getSuperclass();
        }

        if (world != null) {
            world.componentAdded(this, component);
        }
        return component;
    }

    @SuppressWarnings("unchecked")
    public <T extends Component> T getComponent(Class<T> componentClass) {
        Component component = components.get(componentClass);
        if (component == null) {
            // If not found directly, check if we have a subclass of the requested type
            for (Component comp : components.values()) {
                if (componentClass.isAssignableFrom(comp.getClass())) {
                    return (T) comp;
                }
            }
        }
        return (T) component;
    }


    public <T extends Component> boolean hasComponent(Class<T> componentClass) {
        if (components.containsKey(componentClass)) {
            return true;
        }
        // Check for subclasses
        for (Component comp : components.values()) {
            if (componentClass.isAssignableFrom(comp.getClass())) {
                return true;
            }
        }
        return false;
    }

    public <T extends Component> T removeComponent(Class<T> componentClass) {
        T component = (T) components.remove(componentClass);
        if (component != null && world != null) {
            world.componentRemoved(this, component);

            // Also remove from parent class mappings
            Class<?> currentClass = component.getClass().getSuperclass();
            while (Component.class.isAssignableFrom(currentClass)) {
                components.remove(currentClass);
                currentClass = currentClass.getSuperclass();
            }
        }
        return component;
    }

    public Collection<Component> getComponents() {
        // Return only concrete component instances (no duplicates from parent class mappings)
        Set<Component> uniqueComponents = new HashSet<>();
        for (Component comp : components.values()) {
            if (comp.getClass().equals(components.get(comp.getClass()).getClass())) {
                uniqueComponents.add(comp);
            }
        }
        return Collections.unmodifiableCollection(uniqueComponents);
    }

    public UUID getId() {
        return id;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    void setWorld(World world) {
        logger.info("Setting world for entity " + this.getClass().getSimpleName());
        if (this.world != world) {
            this.world = world;

            // If we're being added to a world, register all existing components
            if (world != null) {
                logger.info("Registering " + components.size() + " existing " +
                        "components with world");
                for (Component component : components.values()) {
                    world.componentAdded(this, component);
                }
            }
        }
    }

    public World getWorld() {
        return world;
    }
}