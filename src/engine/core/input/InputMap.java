package engine.core.input;

import engine.core.logging.Logger;

import static org.lwjgl.glfw.GLFW.*;

public class InputMap {
    private static final Logger logger = Logger.getLogger(InputMap.class);

    // Movement
    public static final String ACTION_MOVE_LEFT = "move_left";
    public static final String ACTION_MOVE_RIGHT = "move_right";
    public static final String ACTION_MOVE_UP = "move_up";
    public static final String ACTION_MOVE_DOWN = "move_down";
    public static final String ACTION_JUMP = "jump";
    public static final String ACTION_SPRINT = "sprint";

    // Camera
    public static final String ACTION_CAMERA_PAN = "camera_pan";
    public static final String ACTION_CAMERA_ZOOM_IN = "camera_zoom_in";
    public static final String ACTION_CAMERA_ZOOM_OUT = "camera_zoom_out";

    // Interaction
    public static final String ACTION_INTERACT = "interact";
    public static final String ACTION_USE = "use";
    public static final String ACTION_PAUSE = "pause";

    public static void setupDefaultBindings(InputBindings bindings) {
        logger.info("Setting up default input bindings");

        // Movement
        bindings.bindKey(ACTION_MOVE_LEFT, GLFW_KEY_A);
        bindings.bindKey(ACTION_MOVE_RIGHT, GLFW_KEY_D);
        bindings.bindKey(ACTION_MOVE_UP, GLFW_KEY_W);
        bindings.bindKey(ACTION_MOVE_DOWN, GLFW_KEY_S);
        bindings.bindKey(ACTION_JUMP, GLFW_KEY_SPACE);
        bindings.bindKey(ACTION_SPRINT, GLFW_KEY_LEFT_SHIFT);

        // Camera
        bindings.bindMouseButton(ACTION_CAMERA_PAN, GLFW_MOUSE_BUTTON_RIGHT);
        bindings.bindKey(ACTION_CAMERA_ZOOM_IN, GLFW_KEY_E);
        bindings.bindKey(ACTION_CAMERA_ZOOM_OUT, GLFW_KEY_Q);

        // Interaction
        bindings.bindKey(ACTION_INTERACT, GLFW_KEY_E);
        bindings.bindMouseButton(ACTION_USE, GLFW_MOUSE_BUTTON_LEFT);
        bindings.bindKey(ACTION_PAUSE, GLFW_KEY_ESCAPE);

        bindings.logCurrentBindings();
    }
}