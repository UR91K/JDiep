package main;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Engine {
    private long window;
    private static int width = 1280;
    private static int height = 720;
    private float aspectRatio = (float) width / height;
    private static final float BASE_VIEW_HEIGHT = GameConstants.DEFAULT_VIEW_SIZE * 2;

    private Player player;
    private InputHandler inputHandler;
    private ShaderHandler shaderHandler;
    private CameraHandler camera;
    private float deltaTime;
    private float lastFrame;
    private DebugMenu debugMenu;
    private CommandLine commandLine;
    private TextRenderer textRenderer;
    private int gridVertexCount;
    private int gridVAO;
    private Matrix4f projectionMatrix;

    private GLFWErrorCallback errorCallback;
    private GLFWWindowSizeCallback windowSizeCallback;

    public void init() {
        // Set up error callback first
        errorCallback = GLFWErrorCallback.createPrint(System.err);
        glfwSetErrorCallback(errorCallback);

        // Initialize GLFW
        if (!glfwInit()) {
            throw new RuntimeException("Failed to initialize GLFW");
        }

        // Configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        // Create window
        window = glfwCreateWindow(width, height, "2D Game", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create window");
        }

        // Set up resize callback
        windowSizeCallback = new GLFWWindowSizeCallback() {
            @Override
            public void invoke(long window, int newWidth, int newHeight) {
                width = newWidth;
                height = newHeight;
                aspectRatio = (float) width / height;

                // Update viewport
                glViewport(0, 0, width, height);

                // Update projection matrix
                updateProjectionMatrix();

                // Update UI components
                if (debugMenu != null) {
                    debugMenu.updateProjection(width, height);
                }
                if (commandLine != null) {
                    commandLine.updateProjection(width, height);
                }
            }
        };
        glfwSetWindowSizeCallback(window, windowSizeCallback);

        // Center window
        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(window,
                (vidmode.width() - width) / 2,
                (vidmode.height() - height) / 2
        );

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1); // Enable v-sync
        glfwShowWindow(window);

        // Initialize OpenGL
        GL.createCapabilities();
        glClearColor(
                GameConstants.CLEAR_COLOR_R,
                GameConstants.CLEAR_COLOR_G,
                GameConstants.CLEAR_COLOR_B,
                GameConstants.CLEAR_COLOR_A
        );

        // Initialize components
        shaderHandler = new ShaderHandler();
        player = new Player(new Vector2f(0, 0));
        camera = new CameraHandler(player.getPosition());
        inputHandler = new InputHandler(window, player, camera);

        // Create shared text renderer
        textRenderer = new TextRenderer("fonts/vcr_osd_mono.ttf");

        // Create debug components
        debugMenu = new DebugMenu(player, camera, textRenderer);
        commandLine = new CommandLine(player, textRenderer);

        // Add debug components to input handler
        inputHandler.setDebugMenu(debugMenu);
        inputHandler.setCommandLine(commandLine);

        // Update projection and create grid with new scale
        updateProjectionMatrix();
        createGrid();
    }

    private void updateProjectionMatrix() {
        // Calculate view width based on the fixed view height and current aspect ratio
        float viewWidth = BASE_VIEW_HEIGHT * aspectRatio;

        // Create orthographic projection that maintains consistent height
        projectionMatrix = new Matrix4f().ortho(
                -viewWidth / 2,
                viewWidth / 2,
                -BASE_VIEW_HEIGHT / 2,
                BASE_VIEW_HEIGHT / 2,
                -1.0f, 1.0f
        );
    }

    private float[] generateGridVertices(float spacing) {
        int numLines = (int)((GameConstants.WORLD_SIZE * 2) / spacing) + 1;
        float[] vertices = new float[numLines * 4 * 2 * 2];
        int idx = 0;

        for (float x = -GameConstants.WORLD_SIZE; x <= GameConstants.WORLD_SIZE; x += spacing) {
            vertices[idx++] = x;
            vertices[idx++] = -GameConstants.WORLD_SIZE;
            vertices[idx++] = x;
            vertices[idx++] = GameConstants.WORLD_SIZE;
        }

        for (float y = -GameConstants.WORLD_SIZE; y <= GameConstants.WORLD_SIZE; y += spacing) {
            vertices[idx++] = -GameConstants.WORLD_SIZE;  // x1
            vertices[idx++] = y;  // y1
            vertices[idx++] = GameConstants.WORLD_SIZE;   // x2
            vertices[idx++] = y;  // y2
        }

        return vertices;
    }

    private void createGrid() {
        float[] gridVertices = generateGridVertices(GameConstants.GRID_SPACING);

        gridVAO = glGenVertexArrays();
        int VBO = glGenBuffers();

        glBindVertexArray(gridVAO);
        glBindBuffer(GL_ARRAY_BUFFER, VBO);
        glBufferData(GL_ARRAY_BUFFER, gridVertices, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        // Store the number of vertices for rendering
        gridVertexCount = gridVertices.length / 2;  // Add this as a class field
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            float currentFrame = (float) glfwGetTime();
            deltaTime = currentFrame - lastFrame;
            lastFrame = currentFrame;

            // Input
            Vector2f moveDir = inputHandler.processInput();
            inputHandler.processMousePosition(camera, width, height);

            // Update
            player.updateRotation(inputHandler.getMouseWorldPos());
            player.updateMovement(moveDir, deltaTime);
            player.update(deltaTime);  // This deltaTime propagates through the update chain
            camera.update(player.position, deltaTime);

            // Render
            render();

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void render() {
        glClear(GL_COLOR_BUFFER_BIT);

        // 1. Render game world
        Matrix4f viewMatrix = camera.getViewMatrix();
        Matrix4f viewProjectionMatrix = new Matrix4f(projectionMatrix).mul(viewMatrix);

        // Render grid
        shaderHandler.useShaderProgram();
        shaderHandler.setUniform("projection", viewProjectionMatrix);
        shaderHandler.setUniform("model", new Matrix4f());
        shaderHandler.setUniform("color", GameConstants.GRID_COLOR);

        glBindVertexArray(gridVAO);
        glDrawArrays(GL_LINES, 0, gridVertexCount);

        // Render player
        player.render(shaderHandler, viewProjectionMatrix);

        // Enable blending for UI elements
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // 2. Render UI (Debug Menu)
        if (debugMenu != null && debugMenu.isVisible()) {
            debugMenu.render(width, height);
        }

        // 3. Render Command Line
        if (commandLine != null && commandLine.isVisible()) {
            commandLine.render(width, height);
        }

        // Disable blending
        glDisable(GL_BLEND);
    }

    public void run() {
        try {
            init();
            loop();
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        if (debugMenu != null) debugMenu.cleanup();
        if (player != null) player.cleanup();
        if (shaderHandler != null) shaderHandler.cleanup();
        if (inputHandler != null) inputHandler.cleanup();
        if (textRenderer != null) textRenderer.cleanup();
        if (windowSizeCallback != null) windowSizeCallback.free();

        // Clean up GLFW
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        if (errorCallback != null) {
            errorCallback.free();
        }
    }

    public static int getWindowWidth() {
        return width;
    }

    public static int getWindowHeight() {
        return height;
    }

    public static float getAspectRatio() {
        return (float) width / height;
    }

    public static void main(String[] args) {
        new Engine().run();
    }
}