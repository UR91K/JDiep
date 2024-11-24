package main;

public abstract class Component {
    protected Entity owner;
    protected boolean isEnabled;

    public Component() {
        this.isEnabled = true;
    }

    public void setOwner(Entity owner) {
        this.owner = owner;
    }

    public abstract void update(float deltaTime);

    public void onCollision(Entity other, CollisionInfo info) {}

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

    public boolean isEnabled() {
        return isEnabled;
    }
}