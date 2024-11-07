package main;

import org.joml.Vector2f;
import java.util.*;

public class EntityManager {
    private Map<UUID, Entity> entities;
    private List<Entity> entitiesToAdd;
    private List<UUID> entitiesToRemove;
    private Map<EntityType, List<Entity>> entityTypeMap;
    private TextRenderer textRenderer;

    // Add specific collections for tank entities
    private List<Tank> tanks;
    private List<DummyTank> dummyTanks;


    public EntityManager() {
        this.entities = new HashMap<>();
        this.entitiesToAdd = new ArrayList<>();
        this.entitiesToRemove = new ArrayList<>();
        this.entityTypeMap = new HashMap<>();
        this.tanks = new ArrayList<>();
        this.dummyTanks = new ArrayList<>();

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

            // Add to specific collections
            if (entity instanceof Tank) {
                tanks.add((Tank) entity);
                if (entity instanceof DummyTank) {
                    dummyTanks.add((DummyTank) entity);
                }
            }
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

                // Remove from specific collections
                if (entity instanceof Tank) {
                    tanks.remove(entity);
                    if (entity instanceof DummyTank) {
                        dummyTanks.remove(entity);
                    }
                }
            }
        }
        entitiesToRemove.clear();
    }
    // Dummy tank specific methods



    public DummyTank createDummyTank(Vector2f position) {
        DummyTank dummy = new DummyTank(position);
        dummy.setTextRenderer(textRenderer);
        addEntity(dummy);
        return dummy;
    }

    public void removeAllDummyTanks() {
        List<DummyTank> tanksToRemove = new ArrayList<>(dummyTanks);
        for (DummyTank tank : tanksToRemove) {
            removeEntity(tank.getId());
        }
        DummyTank.resetDummyCount();
    }

    public List<DummyTank> getDummyTanks() {
        return Collections.unmodifiableList(dummyTanks);
    }

    public List<Tank> getAllTanks() {
        return Collections.unmodifiableList(tanks);
    }

    public void setTextRenderer(TextRenderer textRenderer) {
        this.textRenderer = textRenderer;
    }

    public void addEntity(Entity entity) {
        if (entity instanceof Tank) {
            ((Tank) entity).setTextRenderer(textRenderer);
        }
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