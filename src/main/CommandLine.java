//CommandLine.java
//LEGACY IMPLEMENTATION
//USE THIS FOR REFERENCE
package main;

import org.joml.Vector2f;
import org.joml.Vector4f;
import org.joml.Matrix4f;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;


public class CommandLine {
    private boolean isVisible = false;
    private StringBuilder currentCommand = new StringBuilder();
    private List<String> commandHistory = new ArrayList<>();
    private int historyIndex = -1;
    private final TextRenderer textRenderer;
    private final Player player;
    private final EntityManager entityManager;
    private final Vector4f commandLineColor = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
    private final Vector4f backgroundColor = new Vector4f(0.0f, 0.0f, 0.0f, 0.7f);
    private final float COMMAND_LINE_HEIGHT = 20.0f;
    private final float COMMAND_LINE_PADDING = 5.0f;
    private int cursorPosition = 0;
    private float cursorBlinkTimer = 0;
    private final float CURSOR_BLINK_RATE = 0.5f;
    private boolean showCursor = true;
    private Matrix4f orthoMatrix;


    private Map<String, CommandExecutor> commands = new HashMap<>();

    @FunctionalInterface
    interface CommandExecutor {
        void execute(String[] args);
    }

    public CommandLine(Player player, TextRenderer textRenderer, EntityManager entityManager) {
        this.player = player;
        this.textRenderer = textRenderer;
        this.entityManager = entityManager;

        // Initialize projection matrix with default window size
        updateProjection(Engine.getWindowWidth(), Engine.getWindowHeight());

        // Register commands
        registerCommands();
    }

    private void registerCommands() {
        commands.put("tp", args -> {
            if (args.length != 2) {
                System.out.println("Usage: /tp <x> <y>");
                return;
            }
            try {
                float x = Float.parseFloat(args[0]);
                float y = Float.parseFloat(args[1]);
                player.setPosition(new Vector2f(x, y));
            } catch (NumberFormatException e) {
                System.out.println("Invalid coordinates");
            }
        });

        commands.put("setmass", args -> {
            if (args.length != 1) {
                System.out.println("Usage: /setmass <value>");
                return;
            }
            try {
                float mass = Float.parseFloat(args[0]);
                player.setMass(mass);
            } catch (NumberFormatException e) {
                System.out.println("Invalid mass value");
            }
        });

        commands.put("spawn", args -> {
            if (args.length == 0) {
                System.out.println("Usage: /spawn <type> <x> <y>");
                System.out.println("Available types: dummy");
                return;
            }

            if (args[0].equals("help")) {
                System.out.println("Spawn command usage:");
                System.out.println("/spawn dummy <x> <y> - Spawns a dummy tank at specified coordinates");
                System.out.println("Example: /spawn dummy 100 100");
                return;
            }

            String type = args[0].toLowerCase();
            if (type.equals("dummy")) {
                if (args.length != 3) {
                    System.out.println("Usage: /spawn dummy <x> <y>");
                    return;
                }

                try {
                    float x = Float.parseFloat(args[1]);
                    float y = Float.parseFloat(args[2]);

                    // Check world bounds
                    if (x < GameConstants.BOUNDARY_LEFT || x > GameConstants.BOUNDARY_RIGHT ||
                            y < GameConstants.BOUNDARY_BOTTOM || y > GameConstants.BOUNDARY_TOP) {
                        System.out.println("Position out of world bounds");
                        return;
                    }

                    Vector2f spawnPos = new Vector2f(x, y);
                    DummyTank dummy = entityManager.createDummyTank(spawnPos);
                    System.out.println("Spawned dummy tank " + dummy.getDummyId() + " at (" + x + ", " + y + ")");
                } catch (NumberFormatException e) {
                    System.out.println("Invalid coordinates. Usage: /spawn dummy <x> <y>");
                }
            } else {
                System.out.println("Unknown entity type: " + type);
                System.out.println("Available types: dummy");
            }
        });
    }

    public void toggleVisibility() {
        isVisible = !isVisible;
        System.out.println("Command line visibility: " + isVisible);
        if (isVisible) {
            currentCommand.setLength(0);  // Clear any existing content
            currentCommand.append("/");   // Add single slash
            cursorPosition = 1;
            System.out.println("Command line opened with content: " + currentCommand.toString());
        }
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void update(float deltaTime) {
        if (!isVisible) return;

        // Update cursor blink
        cursorBlinkTimer += deltaTime;
        if (cursorBlinkTimer >= CURSOR_BLINK_RATE) {
            cursorBlinkTimer = 0;
            showCursor = !showCursor;
        }
    }

    public void updateProjection(int windowWidth, int windowHeight) {
        // Create orthographic projection for UI
        this.orthoMatrix = new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1f, 1f);
    }

    public void render(int windowWidth, int windowHeight) {
        if (!isVisible) return;

        // Use the cached ortho projection instead of creating a new one each frame
        ShaderHandler shader = new ShaderHandler();
        shader.useShaderProgram();

        // Render current command
        String displayText = currentCommand.toString();
        if (showCursor && cursorPosition <= displayText.length()) {
            displayText = displayText.substring(0, cursorPosition) + "|" +
                    displayText.substring(cursorPosition);
        }

        // Calculate y-position from bottom of window
        float yPosition = windowHeight - COMMAND_LINE_HEIGHT - COMMAND_LINE_PADDING;

        System.out.println("Rendering command line: " + displayText); // Debug print

        // Use cached orthoMatrix instead of creating new one
        textRenderer.renderText(
                orthoMatrix,
                displayText,
                COMMAND_LINE_PADDING,
                yPosition,
                commandLineColor
        );
    }

    public void handleCharInput(char c) {
        if (!isVisible) return;

        // Prevent adding another slash at the start
        if (c == '/' && currentCommand.length() == 1) {
            return;
        }

        // Only accept printable characters
        if (c >= 32 && c < 127) {
            currentCommand.insert(cursorPosition, c);
            cursorPosition++;
            System.out.println("Current command after char input: " + currentCommand.toString());
        }
    }

    public void handleKeyInput(int key, int action) {
        if (!isVisible) return;

        // Only handle key press events, not repeats or releases
        if (action != org.lwjgl.glfw.GLFW.GLFW_PRESS) {
            return;
        }

        System.out.println("Command line handling key: " + key);

        switch (key) {
            case org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE:
                if (cursorPosition > 1) { // Don't delete the initial '/'
                    currentCommand.deleteCharAt(cursorPosition - 1);
                    cursorPosition--;
                    System.out.println("Current command after backspace: " + currentCommand.toString());
                }
                break;

            case org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE:
                if (cursorPosition < currentCommand.length()) {
                    currentCommand.deleteCharAt(cursorPosition);
                }
                break;

            case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT:
                if (cursorPosition > 1) { // Don't move before the '/'
                    cursorPosition--;
                }
                break;

            case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT:
                if (cursorPosition < currentCommand.length()) {
                    cursorPosition++;
                }
                break;

            case org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER:
                executeCommand();
                break;

            case org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE:
                isVisible = false;
                break;

            case org.lwjgl.glfw.GLFW.GLFW_KEY_UP:
                navigateHistory(true);
                break;

            case org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN:
                navigateHistory(false);
                break;
        }
    }

    private void executeCommand() {
        String cmdStr = currentCommand.toString().trim();
        if (cmdStr.length() <= 1) return; // Just the '/' character

        // Add to history
        commandHistory.add(cmdStr);
        historyIndex = commandHistory.size();

        // Parse command
        String[] parts = cmdStr.substring(1).split("\\s+");
        String commandName = parts[0];
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);

        // Execute command
        CommandExecutor executor = commands.get(commandName);
        if (executor != null) {
            executor.execute(args);
        } else {
            System.out.println("Unknown command: " + commandName);
        }

        // Reset command line and hide it
        currentCommand.setLength(0);
        currentCommand.append("/");
        cursorPosition = 1;
        isVisible = false;  // Add this line to hide the command line
    }

    private void navigateHistory(boolean up) {
        if (commandHistory.isEmpty()) return;

        if (up) {
            if (historyIndex > 0) {
                historyIndex--;
                currentCommand.setLength(0);
                currentCommand.append(commandHistory.get(historyIndex));
                cursorPosition = currentCommand.length();
            }
        } else {
            if (historyIndex < commandHistory.size() - 1) {
                historyIndex++;
                currentCommand.setLength(0);
                currentCommand.append(commandHistory.get(historyIndex));
                cursorPosition = currentCommand.length();
            } else {
                currentCommand.setLength(0);
                currentCommand.append("/");
                cursorPosition = 1;
                historyIndex = commandHistory.size();
            }
        }
    }
}