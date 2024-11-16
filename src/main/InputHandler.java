//InputHandler.java
//LEGACY IMPLEMENTATION
//USE THIS FOR REFERENCE
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
    private CommandLine commandLine;
    private Vector2f mouseScreenPos = new Vector2f();
    private Vector2f mouseWorldPos = new Vector2f();
    private GLFWKeyCallback keyCallback;

    public InputHandler(long window, Player player, CameraHandler camera) {
        System.out.println("InputHandler initialized");
        this.window = window;
        this.player = player;
        this.camera = camera;

        // Set up scroll callback for zoom
        glfwSetScrollCallback(window, (win, xoffset, yoffset) -> {
            if (commandLine != null && commandLine.isVisible()) return;
            float zoomFactor = (float) Math.pow(1.1, yoffset);
            camera.adjustZoom(zoomFactor);
        });

        // Set up keyboard input handler
        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            // Only handle key press events, not repeats or releases
            if (action != GLFW_PRESS) {
                return;
            }

            System.out.println("Key pressed: " + key);

            // F3 for debug menu always works
            if (key == GLFW_KEY_F3) {
                if (debugMenu != null) {
                    debugMenu.toggleVisibility();
                }
                return;
            }

            // If command line is visible, send all key input to it
            if (commandLine != null && commandLine.isVisible()) {
                commandLine.handleKeyInput(key, action);
                return;
            }

            // Handle slash key to open command line
            if (key == GLFW_KEY_SLASH) {
                System.out.println("Slash key pressed, opening command line");
                if (commandLine != null && !commandLine.isVisible()) {
                    commandLine.toggleVisibility();
                }
                return;
            }
        });

        // Set up character callback for command line text input
        glfwSetCharCallback(window, (win, codepoint) -> {
            if (commandLine != null && commandLine.isVisible()) {
                System.out.println("Character input: " + (char)codepoint); // Debug print
                commandLine.handleCharInput((char)codepoint);
            }
        });
    }

    public void processMousePosition(CameraHandler camera, int windowWidth, int windowHeight) {
        // If command line is visible, don't process mouse
        if (commandLine != null && commandLine.isVisible()) return;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            DoubleBuffer xpos = stack.mallocDouble(1);
            DoubleBuffer ypos = stack.mallocDouble(1);

            // Get cursor position
            glfwGetCursorPos(window, xpos, ypos);

            // Store the screen coordinates first
            float screenX = (float)xpos.get(0);
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

    public Vector2f processInput() {
        // If command line is visible, return zero movement
        if (commandLine != null && commandLine.isVisible()) {
            return new Vector2f(0, 0);
        }

        Vector2f moveDir = new Vector2f(0, 0);

        // Only process movement keys if command line is not visible
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

    public void setDebugMenu(DebugMenu debugMenu) {
        this.debugMenu = debugMenu;
    }

    public void setCommandLine(CommandLine commandLine) {
        this.commandLine = commandLine;
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