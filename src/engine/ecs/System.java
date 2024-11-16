package engine.ecs;

import java.util.Set;

public abstract class System {
    protected World world;
    private boolean enabled = true;

    public void initialize(World world) {
        this.world = world;
    }

    public abstract void update(float deltaTime);

    protected <T extends Component> Set<Entity> getEntitiesWithComponent(Class<T> componentClass) {
        return world.getEntitiesWithComponent(componentClass);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isPhysicsSystem() {
        return false;  // Override in physics systems
    }

    public boolean isRenderSystem() {
        return false;
    }
}
