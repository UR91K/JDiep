package engine.core.input;

import engine.core.logging.Logger;

import java.util.HashMap;
import java.util.Map;

public class InputContext {
    private static final Logger logger = Logger.getLogger(InputContext.class);

    private final String name;
    private final InputBindings bindings;
    private final Map<String, Runnable> pressedActions = new HashMap<>();
    private final Map<String, Runnable> justPressedActions = new HashMap<>();
    private boolean active;

    public InputContext(String name) {
        this.name = name;
        this.bindings = new InputBindings();
        logger.info("Created new input context: {}", name);
    }

    public void bind(String action, int key) {
        bindings.bindKey(action, key);
    }

    public void bindMouseButton(String action, int button) {
        bindings.bindMouseButton(action, button);
    }

    public void onPressed(String action, Runnable handler) {
        pressedActions.put(action, handler);
        logger.debug("Registered pressed handler for action '{}' in context '{}'", action, name);
    }

    public void onJustPressed(String action, Runnable handler) {
        justPressedActions.put(action, handler);
        logger.debug("Registered just pressed handler for action '{}' in context '{}'", action, name);
    }

    public void update(InputManager input) {
        if (!active) return;

        // Handle continuous press actions
        pressedActions.forEach((action, handler) -> {
            if (bindings.isActionPressed(action, input)) {
                logger.trace("Executing pressed handler for action '{}' in context '{}'", action, name);
                handler.run();
            }
        });

        // Handle just pressed actions
        justPressedActions.forEach((action, handler) -> {
            if (bindings.isActionJustPressed(action, input)) {
                logger.debug("Executing just pressed handler for action '{}' in context '{}'", action, name);
                handler.run();
            }
        });
    }

    public void setActive(boolean active) {
        if (this.active != active) {
            logger.info("Input context '{}' {}activated", name, active ? "" : "de");
            this.active = active;
        }
    }

    public String getName() {
        return name;
    }
}