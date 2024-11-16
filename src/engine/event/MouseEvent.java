package engine.event;

import org.joml.Vector2f;

public abstract class MouseEvent extends Event {

    public static class Move extends MouseEvent {
        private final Vector2f position;
        private final Vector2f delta;

        public Move(Vector2f position, Vector2f delta) {
            this.position = new Vector2f(position);
            this.delta = new Vector2f(delta);
        }

        public Vector2f getPosition() { return new Vector2f(position); }
        public Vector2f getDelta() { return new Vector2f(delta); }
    }

    public static class Button extends MouseEvent {
        private final int button;
        private final int action;
        private final int mods;

        public Button(int button, int action, int mods) {
            this.button = button;
            this.action = action;
            this.mods = mods;
        }

        public int getButton() { return button; }
        public int getAction() { return action; }
        public int getMods() { return mods; }
    }

    public static class Scroll extends MouseEvent {
        private final float xOffset;
        private final float yOffset;

        public Scroll(float xOffset, float yOffset) {
            this.xOffset = xOffset;
            this.yOffset = yOffset;
        }

        public float getXOffset() { return xOffset; }
        public float getYOffset() { return yOffset; }
    }
}
