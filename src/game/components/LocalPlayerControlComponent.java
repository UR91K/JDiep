package game.components;

import engine.core.logging.Logger;
import engine.ecs.Component;
import engine.core.input.InputManager;
import engine.core.input.InputBindings;
import engine.physics.components.Transform2DComponent;
import engine.core.graphics.Camera;
import org.joml.Vector2f;

import static org.lwjgl.glfw.GLFW.*;

public class LocalPlayerControlComponent extends Component {
    private static final Logger logger = Logger.getLogger(LocalPlayerControlComponent.class);

    private static final String ACTION_MOVE_UP = "move_up";
    private static final String ACTION_MOVE_DOWN = "move_down";
    private static final String ACTION_MOVE_LEFT = "move_left";
    private static final String ACTION_MOVE_RIGHT = "move_right";
    private static final String ACTION_FIRE = "fire";

    private final InputManager inputManager;
    private final InputBindings bindings;
    private final Camera camera;
    private final Vector2f moveDirection = new Vector2f();
    private float targetRotation = 0.0f;
    private Transform2DComponent transform;

    public LocalPlayerControlComponent(InputManager inputManager, Camera camera) {
        this.inputManager = inputManager;
        this.camera = camera;
        this.bindings = new InputBindings();

        setupInputBindings();
        logger.info("Created LocalPlayerControlComponent");
    }

    private void setupInputBindings() {
        // Movement bindings
        bindings.bindKey(ACTION_MOVE_UP, GLFW_KEY_W);
        bindings.bindKey(ACTION_MOVE_DOWN, GLFW_KEY_S);
        bindings.bindKey(ACTION_MOVE_LEFT, GLFW_KEY_A);
        bindings.bindKey(ACTION_MOVE_RIGHT, GLFW_KEY_D);

        // Combat bindings
        bindings.bindMouseButton(ACTION_FIRE, GLFW_MOUSE_BUTTON_LEFT);

        logger.debug("Input bindings configured for player control");
    }

    public void processInput() {
        // Lazy initialization of transform component
        if (transform == null) {
            transform = getOwner().getComponent(Transform2DComponent.class);
            if (transform == null) {
                logger.error("LocalPlayerControlComponent requires Transform2DComponent!");
                setEnabled(false);
                return;
            }
        }

        if (!isEnabled()) {
            return;
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Processing input for frame");
        }

        processMovement();
        processAiming();
    }

    private void processMovement() {
        // Reset movement direction
        moveDirection.zero();

        // Build movement vector from input
        if (bindings.isActionPressed(ACTION_MOVE_UP, inputManager)) moveDirection.y += 1;
        if (bindings.isActionPressed(ACTION_MOVE_DOWN, inputManager)) moveDirection.y -= 1;
        if (bindings.isActionPressed(ACTION_MOVE_LEFT, inputManager)) moveDirection.x -= 1;
        if (bindings.isActionPressed(ACTION_MOVE_RIGHT, inputManager)) moveDirection.x += 1;

        // Normalize if moving diagonally
        if (moveDirection.lengthSquared() > 0) {
            moveDirection.normalize();
            if (logger.isDebugEnabled()) {
                logger.debug("Player moving: direction={}", moveDirection);
            }
        }
    }

    private void processAiming() {
        Vector2f mousePos = inputManager.getMousePosition();
        Vector2f worldPos = camera.screenToWorld(mousePos.x, mousePos.y);
        Vector2f tankPos = transform.getPosition();
        Vector2f toMouse = new Vector2f(worldPos).sub(tankPos);

        float newRotation = (float)Math.atan2(toMouse.y, toMouse.x);

        // Only log if rotation changed significantly
        if (Math.abs(newRotation - targetRotation) > 0.01f) {
            if (logger.isTraceEnabled()) {
                logger.trace("Aiming - mouseWorld:{}, tankPos:{}, rotation:{}",
                        worldPos, tankPos, newRotation);
            }
        }

        targetRotation = newRotation;
    }

    public Vector2f getMoveDirection() {
        return new Vector2f(moveDirection);
    }

    public float getTargetRotation() {
        return targetRotation;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (enabled != isEnabled()) {
            if (enabled) {
                logger.debug("Enabling LocalPlayerControlComponent");
            } else {
                logger.debug("Disabling LocalPlayerControlComponent - state: pos={}, move={}, rot={}",
                        transform != null ? transform.getPosition() : "null",
                        moveDirection,
                        targetRotation);
            }
            super.setEnabled(enabled);
        }
    }
}