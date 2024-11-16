package engine.core.input;

import org.joml.Vector2f;
import engine.core.logging.Logger;

public class MouseState {
    private static final Logger logger = Logger.getLogger(MouseState.class);
    public static final int MAX_BUTTONS = 8;
    private static final float MOVEMENT_THRESHOLD = 0.001f;

    private final Vector2f position = new Vector2f();
    private final Vector2f previousPosition = new Vector2f();
    private final Vector2f delta = new Vector2f();
    private final KeyState[] buttons = new KeyState[MAX_BUTTONS];
    private float scrollDelta;
    private boolean hasInitialPosition = false;

    public MouseState() {
        for (int i = 0; i < buttons.length; i++) {
            buttons[i] = new KeyState();
        }
        logger.debug("MouseState initialized with {} button slots", MAX_BUTTONS);
    }

    public void updatePosition(float x, float y) {
        previousPosition.set(position);
        position.set(x, y);

        if (!hasInitialPosition) {
            hasInitialPosition = true;
            previousPosition.set(position);
            logger.debug("Initial mouse position set to ({}, {})", x, y);
        }

        delta.set(position).sub(previousPosition);

        // Only log if there's significant movement
        if (delta.lengthSquared() > MOVEMENT_THRESHOLD) {
            logger.trace("Mouse moved: pos=({}, {}), delta=({}, {})",
                    x, y, delta.x, delta.y);
        }
    }

    public void updateButton(int button, boolean pressed) {
        if (button >= 0 && button < buttons.length) {
            KeyState state = buttons[button];
            boolean wasPressed = state.isPressed();
            state.update(pressed);

            if (wasPressed != pressed) {
                logger.debug("Mouse button {} {}",
                        button, pressed ? "pressed" : "released");
            }
        } else {
            logger.warn("Invalid mouse button index: {}", button);
        }
    }

    public void updateScroll(float delta) {
        if (Math.abs(delta) > MOVEMENT_THRESHOLD) {
            logger.trace("Mouse scroll: {}", delta);
        }
        this.scrollDelta = delta;
    }

    public void reset() {
        boolean hadSignificantState = hasSignificantState();

        if (hadSignificantState) {
            logger.trace("Resetting mouse state: {}", getDebugString());
        }

        delta.zero();
        scrollDelta = 0;
        for (KeyState button : buttons) {
            button.reset();
        }
    }

    public boolean hasActiveInput() {
        // Check for significant mouse movement
        if (delta.lengthSquared() > MOVEMENT_THRESHOLD) {
            return true;
        }

        // Check for scroll input
        if (Math.abs(scrollDelta) > MOVEMENT_THRESHOLD) {
            return true;
        }

        // Check for any pressed buttons
        for (KeyState button : buttons) {
            if (button.isPressed() || button.isJustPressed()) {
                return true;
            }
        }

        return false;
    }

    private boolean hasSignificantState() {
        return delta.lengthSquared() > MOVEMENT_THRESHOLD ||
                Math.abs(scrollDelta) > MOVEMENT_THRESHOLD ||
                hasActiveInput();
    }

    public String getDebugString() {
        StringBuilder sb = new StringBuilder();
        sb.append("pos=(").append(String.format("%.1f", position.x))
                .append(", ").append(String.format("%.1f", position.y)).append(")");

        if (delta.lengthSquared() > MOVEMENT_THRESHOLD) {
            sb.append(", delta=(").append(String.format("%.1f", delta.x))
                    .append(", ").append(String.format("%.1f", delta.y)).append(")");
        }

        if (Math.abs(scrollDelta) > MOVEMENT_THRESHOLD) {
            sb.append(", scroll=").append(String.format("%.1f", scrollDelta));
        }

        // Add button states
        boolean first = true;
        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i].isPressed() || buttons[i].isJustPressed()) {
                if (first) {
                    sb.append(", buttons=[");
                    first = false;
                } else {
                    sb.append(", ");
                }
                sb.append(i);
                if (buttons[i].isJustPressed()) {
                    sb.append("(just)");
                }
            }
        }
        if (!first) {
            sb.append("]");
        }

        return sb.toString();
    }

    public MouseStateSnapshot createSnapshot() {
        logger.trace("Creating mouse state snapshot: {}", getDebugString());
        return new MouseStateSnapshot(
                new Vector2f(position),
                new Vector2f(delta),
                buttons.clone(),
                scrollDelta
        );
    }

    // Improved getters with validation
    public Vector2f getPosition() {
        return new Vector2f(position);
    }

    public Vector2f getPreviousPosition() {
        return new Vector2f(previousPosition);
    }

    public Vector2f getDelta() {
        return new Vector2f(delta);
    }

    public float getScrollDelta() {
        return scrollDelta;
    }

    public KeyState getButton(int button) {
        if (button < 0 || button >= buttons.length) {
            logger.warn("Attempted to get invalid button state: {}", button);
            return null;
        }
        return buttons[button];
    }

    public boolean hasInitialPosition() {
        return hasInitialPosition;
    }
}