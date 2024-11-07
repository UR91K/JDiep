package main;

import java.util.*;

public class EntityManager {
    private Map<UUID, Entity> entities;
    private List<Entity> entitiesToAdd;
    private List<UUID> entitiesToRemove;
    private Map<EntityType, List<Entity>> entityTypeMap;

    public EntityManager() {
        this.entities = new HashMap<>();
        this.entitiesToAdd = new ArrayList<>();
        this.entitiesToRemove = new ArrayList<>();
        this.entityTypeMap = new HashMap<>();

        // Initialize lists for each entity type
        for (EntityType type : EntityType.values()) {
            entityTypeMap.put(type, new ArrayList<>());
        }
    }

    public void update(float deltaTime) {
        // Process additions
        for (Entity entity : entitiesToAdd) {
            entities.put(entity.getId(), entity);
            entityTypeMap.get(entity.getType()).add(entity);
        }
        entitiesToAdd.clear();

        // Update all entities
        for (Entity entity : entities.values()) {
            if (entity.isActive()) {
                entity.update(deltaTime);
            }
        }

        // Process removals
        for (UUID id : entitiesToRemove) {
            Entity entity = entities.get(id);
            if (entity != null) {
                entityTypeMap.get(entity.getType()).remove(entity);
                entities.remove(id);
            }
        }
        entitiesToRemove.clear();
    }

    public void addEntity(Entity entity) {
        entitiesToAdd.add(entity);
    }

    public void removeEntity(UUID id) {
        entitiesToRemove.add(id);
    }

    public Entity getEntity(UUID id) {
        return entities.get(id);
    }

    public List<Entity> getEntitiesByType(EntityType type) {
        return new ArrayList<>(entityTypeMap.get(type));
    }

    public Collection<Entity> getAllEntities() {
        return new ArrayList<>(entities.values());
    }
}