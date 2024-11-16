package engine.core.input;

import org.joml.Vector2f;

class MouseStateSnapshot {
    private final Vector2f position;
    private final Vector2f delta;
    private final KeyState[] buttons;
    private final float scrollDelta;

    public MouseStateSnapshot(Vector2f position, Vector2f delta,
                              KeyState[] buttons, float scrollDelta) {
        this.position = new Vector2f(position);
        this.delta = new Vector2f(delta);
        this.buttons = buttons.clone();
        this.scrollDelta = scrollDelta;
    }

    public Vector2f getPosition() {
        return position;
    }

    public Vector2f getDelta() {
        return delta;
    }

    public KeyState[] getButtons() {
        return buttons;
    }

    public float getScrollDelta() {
        return scrollDelta;
    }
}
