package engine.core.input;

import org.joml.Vector2f;
import engine.event.EventBus;
import engine.event.KeyEvent;
import engine.event.MouseEvent;
import engine.event.WindowEvent;
import engine.core.logging.Logger;

import java.util.HashMap;
import java.util.Map;
import static org.lwjgl.glfw.GLFW.*;

public class InputManager {
    private static final Logger logger = Logger.getLogger(InputManager.class);

    private final EventBus eventBus;
    private final Map<Integer, KeyState> keyStates;
    private final MouseState mouseState;
    private final long windowHandle;
    private boolean cursorLocked;
    private boolean windowFocused;
    private boolean initialized;

    public InputManager(EventBus eventBus, long windowHandle) {
        this.eventBus = eventBus;
        this.windowHandle = windowHandle;
        this.keyStates = new HashMap<>();
        this.mouseState = new MouseState();
        this.windowFocused = true;
        this.initialized = true;

        // Subscribe to window focus events
        eventBus.subscribe(WindowEvent.Focus.class, this::handleFocusEvent);

        logger.info("InputManager initialized for window {}", windowHandle);
        logger.debug("Initial state - focused: {}, cursorLocked: {}", windowFocused, cursorLocked);
    }

    public void update() {
        if (!initialized) {
            logger.error("Update called before initialization");
            return;
        }

        if (!windowFocused) {
            if (!keyStates.isEmpty() || mouseState.hasActiveInput()) {
                logger.debug("Clearing input states due to window focus loss");
                keyStates.values().forEach(KeyState::reset);
                mouseState.reset();
            }
            return;
        }

        // Log active inputs before reset
        if (logger.isTraceEnabled()) {
            logActiveInputs();
        }

        // Reset one-frame states
        keyStates.values().forEach(KeyState::reset);
        mouseState.reset();
    }

    private void logActiveInputs() {
        StringBuilder activeKeys = new StringBuilder();
        keyStates.forEach((key, state) -> {
            if (state.isPressed() || state.isJustPressed()) {
                activeKeys.append(getKeyName(key)).append(" ");
            }
        });

        if (activeKeys.length() > 0 || mouseState.hasActiveInput()) {
            logger.trace("Active inputs - Keys: {}, Mouse: {}",
                    activeKeys.toString().trim(),
                    mouseState.getDebugString());
        }
    }

    private void handleFocusEvent(WindowEvent.Focus event) {
        boolean wasFocused = windowFocused;
        windowFocused = event.isFocused();

        logger.debug("Window focus changed: {} -> {}", wasFocused, windowFocused);

        if (!windowFocused) {
            logger.debug("Clearing input states due to focus loss");
            keyStates.values().forEach(state -> state.update(false));
            for (int i = 0; i < MouseState.MAX_BUTTONS; i++) {
                mouseState.updateButton(i, false);
            }
        }
    }

    public void handleKeyInput(int key, int action, int mods) {
        if (!windowFocused) {
            logger.trace("Ignoring key input (window not focused) - key: {}, action: {}",
                    getKeyName(key), getActionName(action));
            return;
        }

        KeyState state = keyStates.computeIfAbsent(key, k -> {
            logger.trace("Creating new key state for {}", getKeyName(k));
            return new KeyState();
        });

        boolean pressed = action != GLFW_RELEASE;
        logger.debug("Key input - key: {}, action: {}, mods: {}",
                getKeyName(key), getActionName(action), getModifierString(mods));

        state.update(pressed);
        eventBus.emit(new KeyEvent(key, action, mods));
    }

    public void handleMouseButton(int button, int action, int mods) {
        if (!windowFocused) {
            logger.trace("Ignoring mouse button (window not focused) - button: {}, action: {}",
                    button, getActionName(action));
            return;
        }

        boolean pressed = action != GLFW_RELEASE;
        logger.debug("Mouse button - button: {}, action: {}, mods: {}",
                button, getActionName(action), getModifierString(mods));

        mouseState.updateButton(button, pressed);
        eventBus.emit(new MouseEvent.Button(button, action, mods));
    }

    public void handleMousePosition(double x, double y) {
        if (!windowFocused) {
            logger.trace("Ignoring mouse position (window not focused) - pos: ({}, {})", x, y);
            return;
        }

        mouseState.updatePosition((float)x, (float)y);
        Vector2f delta = mouseState.getDelta();

        if (delta.lengthSquared() > 0) {
            logger.trace("Mouse moved - pos: ({}, {}), delta: ({}, {})", x, y, delta.x, delta.y);
        }

        eventBus.emit(new MouseEvent.Move(mouseState.getPosition(), delta));
    }

    public void handleMouseScroll(double xOffset, double yOffset) {
        if (!windowFocused) {
            logger.trace("Ignoring mouse scroll (window not focused) - offset: ({}, {})",
                    xOffset, yOffset);
            return;
        }

        logger.trace("Mouse scroll - offset: ({}, {})", xOffset, yOffset);
        mouseState.updateScroll((float)yOffset);
        eventBus.emit(new MouseEvent.Scroll((float)xOffset, (float)yOffset));
    }

    public void setCursorLocked(boolean locked) {
        if (this.cursorLocked != locked) {
            logger.info("Cursor lock changed: {} -> {}", this.cursorLocked, locked);
            this.cursorLocked = locked;
            glfwSetInputMode(windowHandle, GLFW_CURSOR,
                    locked ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
        }
    }

    // Getters with input validation
    public boolean isKeyPressed(int key) {
        if (!windowFocused) return false;
        if (key < 0) {
            logger.warn("Invalid key code: {}", key);
            return false;
        }
        KeyState state = keyStates.get(key);
        return state != null && state.isPressed();
    }

    public boolean isKeyJustPressed(int key) {
        if (!windowFocused) return false;
        if (key < 0) {
            logger.warn("Invalid key code: {}", key);
            return false;
        }
        KeyState state = keyStates.get(key);
        boolean result = state != null && state.isJustPressed();
        if (result) {
            logger.debug("Key just pressed: {}", getKeyName(key));
        }
        return result;
    }

    public boolean isMouseButtonPressed(int button) {
        if (!windowFocused) return false;
        if (button < 0 || button >= MouseState.MAX_BUTTONS) {
            logger.warn("Invalid mouse button: {}", button);
            return false;
        }
        return mouseState.getButton(button).isPressed();
    }

    public boolean isMouseButtonJustPressed(int button) {
        if (!windowFocused) return false;
        if (button < 0 || button >= MouseState.MAX_BUTTONS) {
            logger.warn("Invalid mouse button: {}", button);
            return false;
        }
        boolean result = mouseState.getButton(button).isJustPressed();
        if (result) {
            logger.debug("Mouse button just pressed: {}", button);
        }
        return result;
    }

    // Helper methods for logging
    private String getKeyName(int key) {
        // Implement GLFW key name lookup
        return "KEY_" + key;
    }

    private String getActionName(int action) {
        return switch (action) {
            case GLFW_PRESS -> "PRESS";
            case GLFW_RELEASE -> "RELEASE";
            case GLFW_REPEAT -> "REPEAT";
            default -> "UNKNOWN";
        };
    }

    private String getModifierString(int mods) {
        StringBuilder sb = new StringBuilder();
        if ((mods & GLFW_MOD_SHIFT) != 0) sb.append("SHIFT ");
        if ((mods & GLFW_MOD_CONTROL) != 0) sb.append("CTRL ");
        if ((mods & GLFW_MOD_ALT) != 0) sb.append("ALT ");
        if ((mods & GLFW_MOD_SUPER) != 0) sb.append("SUPER ");
        return sb.length() > 0 ? sb.toString().trim() : "NONE";
    }

    // Remaining getters
    public Vector2f getMousePosition() { return mouseState.getPosition(); }
    public Vector2f getMouseDelta() { return mouseState.getDelta(); }
    public float getScrollDelta() { return windowFocused ? mouseState.getScrollDelta() : 0f; }
    public boolean isCursorLocked() { return cursorLocked; }
    public boolean isWindowFocused() { return windowFocused; }

    public InputSnapshot createSnapshot() {
        logger.trace("Creating input snapshot");
        return new InputSnapshot(
                new HashMap<>(keyStates),
                mouseState.createSnapshot(),
                cursorLocked
        );
    }
}