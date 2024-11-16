package engine.core.input;

import java.util.HashMap;
import java.util.Map;
import engine.core.logging.Logger;

public class InputBindings {
    private static final Logger logger = Logger.getLogger(InputBindings.class);

    private final Map<String, Integer> keyBindings = new HashMap<>();
    private final Map<String, Integer> mouseBindings = new HashMap<>();
    private final Map<String, Long> lastActionTime = new HashMap<>();
    private static final long DOUBLE_PRESS_THRESHOLD = 300; // milliseconds

    public void bindKey(String action, int key) {
        Integer oldKey = keyBindings.put(action, key);
        if (oldKey != null) {
            logger.info("Rebound action '{}' from key {} to {}", action, oldKey, key);
        } else {
            logger.info("Bound action '{}' to key {}", action, key);
        }
    }

    public void bindMouseButton(String action, int button) {
        Integer oldButton = mouseBindings.put(action, button);
        if (oldButton != null) {
            logger.info("Rebound action '{}' from mouse button {} to {}",
                    action, oldButton, button);
        } else {
            logger.info("Bound action '{}' to mouse button {}", action, button);
        }
    }

    public void unbindAction(String action) {
        Integer key = keyBindings.remove(action);
        Integer button = mouseBindings.remove(action);
        if (key != null || button != null) {
            logger.info("Unbound action '{}' (was bound to key: {}, mouse: {})",
                    action, key, button);
        }
    }

    public boolean isActionPressed(String action, InputManager input) {
        if (action == null || input == null) {
            logger.warn("Null action or input manager");
            return false;
        }

        Integer key = keyBindings.get(action);
        if (key != null) {
            boolean pressed = input.isKeyPressed(key);
            logger.trace("Action '{}' (key {}) pressed: {}", action, key, pressed);
            return pressed;
        }

        Integer button = mouseBindings.get(action);
        if (button != null) {
            boolean pressed = input.isMouseButtonPressed(button);
            logger.trace("Action '{}' (mouse {}) pressed: {}", action, button, pressed);
            return pressed;
        }

        logger.trace("Attempted to check unbound action: '{}'", action);
        return false;
    }

    public boolean isActionJustPressed(String action, InputManager input) {
        if (action == null || input == null) {
            logger.warn("Null action or input manager");
            return false;
        }

        Integer key = keyBindings.get(action);
        if (key != null && input.isKeyJustPressed(key)) {
            logger.debug("Action '{}' just triggered by key {}", action, key);
            updateActionTime(action);
            return true;
        }

        Integer button = mouseBindings.get(action);
        if (button != null && input.isMouseButtonJustPressed(button)) {
            logger.debug("Action '{}' just triggered by mouse button {}", action, button);
            updateActionTime(action);
            return true;
        }

        return false;
    }

    private void updateActionTime(String action) {
        long now = System.currentTimeMillis();
        lastActionTime.put(action, now);
    }

    public boolean isActionDoublePressed(String action, InputManager input) {
        if (!isActionJustPressed(action, input)) {
            return false;
        }

        Long lastTime = lastActionTime.get(action);
        long now = System.currentTimeMillis();
        boolean isDouble = lastTime != null && (now - lastTime) <= DOUBLE_PRESS_THRESHOLD;

        if (isDouble) {
            logger.debug("Double press detected for action '{}'", action);
        }

        return isDouble;
    }

    public void clearBindings() {
        logger.info("Clearing all input bindings");
        keyBindings.clear();
        mouseBindings.clear();
        lastActionTime.clear();
    }

    public void logCurrentBindings() {
        logger.debug("Current key bindings:");
        keyBindings.forEach((action, key) ->
                logger.debug("  {} -> key {}", action, key));

        logger.debug("Current mouse bindings:");
        mouseBindings.forEach((action, button) ->
                logger.debug("  {} -> mouse button {}", action, button));
    }
}