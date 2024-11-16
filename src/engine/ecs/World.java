package engine.ecs;

import engine.core.logging.Logger;

import java.util.*;

public class World {
    private final Map<UUID, Entity> entities = new HashMap<>();
    private final Map<Class<? extends Component>, Set<Entity>> componentEntities = new HashMap<>();
    private final List<System> systems = new ArrayList<>();
    private final Queue<Runnable> entityUpdates = new LinkedList<>();
    private boolean updating = false;
    private static final Logger logger = Logger.getLogger(World.class);

    /**
     * Creates a new empty entity
     */
    public Entity createEntity() {
        Entity entity = new Entity();
        addEntity(entity);
        return entity;
    }

    /**
     * Adds an existing entity to the world
     */
    public void addEntity(Entity entity) {
        logger.debug("Adding entity to world: " + entity.getClass().getSimpleName());

        if (entity.getWorld() != null && entity.getWorld() != this) {
            throw new IllegalStateException("Entity already belongs to another world");
        }

        entity.setWorld(this);
        entityUpdates.add(() -> {
            entities.put(entity.getId(), entity);

            // Add to component tracking for any existing components
            for (Component component : entity.getComponents()) {
                logger.debug("Processing existing component: " +
                        component.getClass().getSimpleName());
                componentEntities
                        .computeIfAbsent(component.getClass(), k -> new HashSet<>())
                        .add(entity);
            }
        });
    }

    /**
     * Removes an entity from the world
     */
    public void removeEntity(Entity entity) {
        if (entity.getWorld() != this) {
            throw new IllegalStateException("Entity does not belong to this world");
        }

        entityUpdates.add(() -> {
            entities.remove(entity.getId());
            for (Set<Entity> entitySet : componentEntities.values()) {
                entitySet.remove(entity);
            }
            entity.setWorld(null);
        });
    }

    public void destroyEntity(Entity entity) {
        entityUpdates.add(() -> {
            entities.remove(entity.getId());
            for (Set<Entity> entitySet : componentEntities.values()) {
                entitySet.remove(entity);
            }
        });
    }

    public Entity getEntity(UUID id) {
        return entities.get(id);
    }

    void componentAdded(Entity entity, Component component) {
        if (entity == null || component == null) {
            logger.error("Attempted to add null entity or component");
            return;
        }

        logger.debug("Adding component {} to entity {}",
                component.getClass().getSimpleName(),
                entity.getClass().getSimpleName());

        // Register for the concrete class
        addEntityForComponent(entity, component.getClass());

        // Register for all parent classes up to Component
        Class<?> currentClass = component.getClass().getSuperclass();
        while (currentClass != null && Component.class.isAssignableFrom(currentClass)) {
            addEntityForComponent(entity, (Class<? extends Component>) currentClass);
            currentClass = currentClass.getSuperclass();
        }
    }

    private void addEntityForComponent(Entity entity, Class<? extends Component> componentClass) {
        componentEntities.computeIfAbsent(componentClass, k -> new HashSet<>()).add(entity);
        logger.debug("Registered entity for component type: {}", componentClass.getSimpleName());
    }

    void componentRemoved(Entity entity, Component component) {
        entityUpdates.add(() -> {
            Set<Entity> entities = componentEntities.get(component.getClass());
            if (entities != null) {
                entities.remove(entity);
                if (entities.isEmpty()) {
                    componentEntities.remove(component.getClass());
                }
            }
        });
    }

    public <T extends Component> Set<Entity> getEntitiesWithComponent(Class<T> componentClass) {
        if (componentClass == null) {
            logger.error("Attempted to query with null component class");
            return Collections.emptySet();
        }

        Set<Entity> entities = componentEntities.get(componentClass);
        logger.debug("Found {} entities with component {}",
                entities != null ? entities.size() : 0,
                componentClass.getSimpleName());

        return entities != null ? Collections.unmodifiableSet(entities) : Collections.emptySet();
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public final <T extends Component> Set<Entity> getEntitiesWithComponents(Class<? extends Component>... componentClasses) {
        if (componentClasses.length == 0) {
            return Collections.emptySet();
        }

        Set<Entity> result = new HashSet<>(getEntitiesWithComponent(componentClasses[0]));
        for (int i = 1; i < componentClasses.length; i++) {
            result.retainAll(getEntitiesWithComponent(componentClasses[i]));
        }
        return Collections.unmodifiableSet(result);
    }

    public void addSystem(System system) {
        system.initialize(this);
        systems.add(system);
    }

    public void removeSystem(System system) {
        systems.remove(system);
    }

    public void update(float deltaTime) {
        updating = true;

        // Process any pending entity updates
        while (!entityUpdates.isEmpty()) {
            entityUpdates.poll().run();
        }

        // Update all enabled systems
        for (System system : systems) {
            if (system.isEnabled()) {
                system.update(deltaTime);
            }
        }

        updating = false;

        // Process any updates that were queued during system updates
        while (!entityUpdates.isEmpty()) {
            entityUpdates.poll().run();
        }
    }

    public Collection<Entity> getAllEntities() {
        return Collections.unmodifiableCollection(entities.values());
    }

    /**
     * Get all registered systems
     */
    public List<System> getSystems() {
        return Collections.unmodifiableList(systems);
    }

    /**
     * Get all registered component types
     */
    public Set<Class<? extends Component>> getComponentTypes() {
        return Collections.unmodifiableSet(componentEntities.keySet());
    }

    public void cleanup() {
        entities.clear();
        componentEntities.clear();
        systems.clear();
        entityUpdates.clear();
    }
}