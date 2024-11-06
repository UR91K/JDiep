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

    private static final float WORLD_SIZE = 500.0f; // Half the total size (1000x1000 total)
    private static final float GRID_SPACING = 2.0f; // Space between grid lines

    private Player player;
    private InputHandler inputHandler;
    private ShaderHandler shaderHandler;
    private CameraHandler camera;
    private float deltaTime;
    private float lastFrame;
    private DebugMenu debugMenu;
    private int gridVertexCount;
    private int gridVAO;
    private Matrix4f projectionMatrix;

    private GLFWErrorCallback errorCallback;

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
        glfwWindowHint(GLFW_RESIZABLE, 1);

        // Create window
        window = glfwCreateWindow(width, height, "2D Game", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create window");
        }

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
        debugMenu = new DebugMenu(player, camera);

        // Add debug menu to input handler after creation
        inputHandler.setDebugMenu(debugMenu);

        // Update projection and create grid with new scale
        updateProjectionMatrix();
        createGrid();
    }

    private void updateProjectionMatrix() {
        float aspectRatio = (float) width / height;

        projectionMatrix = new Matrix4f().ortho(
                -GameConstants.DEFAULT_VIEW_SIZE * aspectRatio,
                GameConstants.DEFAULT_VIEW_SIZE * aspectRatio,
                -GameConstants.DEFAULT_VIEW_SIZE,
                GameConstants.DEFAULT_VIEW_SIZE,
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
            player.update(moveDir, deltaTime);
            camera.update(player.getPosition(), deltaTime);

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

        // 2. Render UI (Debug Menu)
        if (debugMenu != null && debugMenu.isVisible()) {
            // Enable blending for UI
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            debugMenu.render(width, height);

            // Restore blend state
            glDisable(GL_BLEND);
        }
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

    public static void main(String[] args) {
        new Engine().run();
    }
}