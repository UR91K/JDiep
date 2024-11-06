package main;

import org.joml.Vector2f;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.system.MemoryStack;

import java.nio.DoubleBuffer;

import static javax.swing.text.html.CSS.Attribute.LINE_HEIGHT;
import static org.lwjgl.glfw.GLFW.*;

public class InputHandler {
    private long window;
    private Player player;
    private CameraHandler camera;
    private DebugMenu debugMenu;
    private Vector2f mouseScreenPos = new Vector2f();
    private Vector2f mouseWorldPos = new Vector2f();
    private GLFWKeyCallback keyCallback;

    public InputHandler(long window, Player player, CameraHandler camera) {
        System.out.println("InputHandler initialized"); // Debug print
        this.window = window;
        this.player = player;
        this.camera = camera;

        // Set up scroll callback for zoom
        glfwSetScrollCallback(window, (win, xoffset, yoffset) -> {
            float zoomFactor = (float) Math.pow(1.1, yoffset);
            camera.adjustZoom(zoomFactor);
        });

        // Set up keyboard input handler
        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            System.out.println("Key event received - Key: " + key + ", Action: " + action); // Debug print

            if (key == GLFW_KEY_F3 && action == GLFW_PRESS) {
                System.out.println("F3 pressed"); // Debug print
                if (debugMenu != null) {
                    System.out.println("Debug menu exists, toggling..."); // Debug print
                    debugMenu.toggleVisibility();
                } else {
                    System.out.println("Debug menu is null!"); // Debug print
                }
            }
        });
    }

    public void processMousePosition(CameraHandler camera, int windowWidth, int windowHeight) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            DoubleBuffer xpos = stack.mallocDouble(1);
            DoubleBuffer ypos = stack.mallocDouble(1);

            // Get cursor position
            glfwGetCursorPos(window, xpos, ypos);

            // Store the screen coordinates first
            float screenX = (float)xpos.get(0);  // Use get(0) to read from the start
            float screenY = (float)ypos.get(0);
            mouseScreenPos.set(screenX, screenY);

            // Convert to world coordinates
            mouseWorldPos = camera.screenToWorld(screenX, screenY, windowWidth, windowHeight);

            // Process debug menu input if visible
            if (debugMenu != null && debugMenu.isVisible()) {
                boolean mousePressed = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;
                boolean mouseDown = mousePressed ||
                        glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_REPEAT;
                if (mousePressed || mouseDown) {
                    debugMenu.updateSliders(mouseScreenPos);
                }
            }
        }
    }

    // Method to add debug menu after it's created

    public void setDebugMenu(DebugMenu debugMenu) {
        System.out.println("Setting debug menu"); // Debug print
        this.debugMenu = debugMenu;
    }

    private Vector2f processMovementInput() {
        Vector2f moveDir = new Vector2f(0, 0);

        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) moveDir.y += 1;
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) moveDir.y -= 1;
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) moveDir.x -= 1;
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) moveDir.x += 1;

        // Sprint modifier
        if (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) {
            moveDir.mul(2.0f);
        }

        if (moveDir.lengthSquared() > 0) {
            moveDir.normalize();
        }

        return moveDir;
    }

    public Vector2f processInput() {
        Vector2f moveDir = new Vector2f(0, 0);

        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) moveDir.y += 1;
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) moveDir.y -= 1;
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) moveDir.x -= 1;
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) moveDir.x += 1;

        // Sprint modifier
        if (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) {
            moveDir.mul(2.0f);
        }

        if (moveDir.lengthSquared() > 0) {
            moveDir.normalize();
        }

        if (glfwGetKey(window, GLFW_KEY_F3) == GLFW_PRESS) {
            System.out.println("F3 detected in processInput()"); // Debug print
        }

        return moveDir;
    }

    private void processDebugMenuInput(boolean mousePressed, boolean mouseDown) {
        if (mousePressed || mouseDown) {
            debugMenu.updateSliders(mouseScreenPos);
        }
    }

    public Vector2f getMouseWorldPos() {
        return new Vector2f(mouseWorldPos);
    }

    public Vector2f getMouseScreenPos() {
        return new Vector2f(mouseScreenPos);
    }

    public void cleanup() {
        if (keyCallback != null) {
            keyCallback.free();
        }
    }
}