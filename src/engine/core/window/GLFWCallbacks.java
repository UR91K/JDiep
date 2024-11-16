package engine.core.window;

import org.lwjgl.glfw.*;
import engine.core.input.InputManager;

import static org.lwjgl.glfw.GLFW.*;

public class GLFWCallbacks {
    private final Window window;
    private final InputManager inputManager;

    private GLFWWindowSizeCallback windowSizeCallback;
    private GLFWKeyCallback keyCallback;
    private GLFWCursorPosCallback cursorPosCallback;
    private GLFWMouseButtonCallback mouseButtonCallback;
    private GLFWScrollCallback scrollCallback;

    public GLFWCallbacks(Window window, InputManager inputManager) {
        this.window = window;
        this.inputManager = inputManager;
    }

    public void setupCallbacks() {
        // Window resize callback
        windowSizeCallback = new GLFWWindowSizeCallback() {
            @Override
            public void invoke(long windowHandle, int width, int height) {
                // Will be handled by graphics system
            }
        };

        // Keyboard callback
        keyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long windowHandle, int key, int scancode, int action, int mods) {
                inputManager.handleKeyInput(key, action, mods);
            }
        };

        // Mouse position callback
        cursorPosCallback = new GLFWCursorPosCallback() {
            @Override
            public void invoke(long windowHandle, double x, double y) {
                inputManager.handleMousePosition(x, y);
            }
        };

        // Mouse button callback
        mouseButtonCallback = new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long windowHandle, int button, int action, int mods) {
                inputManager.handleMouseButton(button, action, mods);
            }
        };

        // Scroll callback
        scrollCallback = new GLFWScrollCallback() {
            @Override
            public void invoke(long windowHandle, double xOffset, double yOffset) {
                inputManager.handleMouseScroll(xOffset, yOffset);
            }
        };

        // Set all callbacks
        glfwSetWindowSizeCallback(window.getHandle(), windowSizeCallback);
        glfwSetKeyCallback(window.getHandle(), keyCallback);
        glfwSetCursorPosCallback(window.getHandle(), cursorPosCallback);
        glfwSetMouseButtonCallback(window.getHandle(), mouseButtonCallback);
        glfwSetScrollCallback(window.getHandle(), scrollCallback);
    }

    public void cleanup() {
        windowSizeCallback.free();
        keyCallback.free();
        cursorPosCallback.free();
        mouseButtonCallback.free();
        scrollCallback.free();
    }
}