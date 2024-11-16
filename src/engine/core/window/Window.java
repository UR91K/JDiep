package engine.core.window;

import engine.core.input.InputManager;
import engine.core.logging.Logger;
import engine.event.EventBus;
import engine.event.WindowEvent;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Window {
    private static final Logger logger = Logger.getLogger(Window.class);

    private long handle;
    private WindowConfig config;
    private GLFWCallbacks callbacks;
    private boolean isFullscreen = false;
    private int[] savedPos = new int[2];
    private int[] savedSize = new int[2];
    private EventBus eventBus;
    private InputManager inputManager;

    // Track current window dimensions
    private static int currentWidth;
    private static int currentHeight;

    private boolean contextReady = false;

    public Window(WindowConfig config) {
        this.config = config;
        this.eventBus = new EventBus();
        currentWidth = config.getWidth();
        currentHeight = config.getHeight();
    }

    public void init() {
        logger.info("Initializing GLFW window");

        // Setup error callback with logging
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW with error checking
        logger.debug("Initializing GLFW");
        if (!glfwInit()) {
            logger.error("Failed to initialize GLFW");
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure GLFW
        logger.debug("Configuring GLFW window hints");
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, config.isResizable() ? GLFW_TRUE : GLFW_FALSE);
        glfwWindowHint(GLFW_SAMPLES, config.getMultisamples());

        // Create window
        logger.debug("Creating GLFW window");
        handle = glfwCreateWindow(
                config.getWidth(),
                config.getHeight(),
                config.getTitle(),
                NULL,
                NULL
        );

        if (handle == NULL) {
            logger.error("Failed to create GLFW window");
            throw new RuntimeException("Failed to create GLFW window");
        }

        // Set up size callback
        logger.debug("Setting up window callbacks");
        glfwSetWindowSizeCallback(handle, (window, width, height) -> {
            logger.debug("Window resized: {}x{}", width, height);
            currentWidth = width;
            currentHeight = height;
            eventBus.emit(new WindowEvent.Resize(width, height));
        });

        // Initialize input manager
        logger.debug("Initializing input manager");
        this.inputManager = new InputManager(eventBus, handle);

        // Set up remaining callbacks
        callbacks = new GLFWCallbacks(this, inputManager);
        callbacks.setupCallbacks();

        // Center window
        logger.debug("Centering window on screen");
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            glfwGetWindowSize(handle, pWidth, pHeight);
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            glfwSetWindowPos(
                    handle,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        }

        // Make OpenGL context current
        logger.debug("Making OpenGL context current");
        glfwMakeContextCurrent(handle);

        // Enable v-sync
        logger.debug("Setting VSync: {}", config.isVsync());
        glfwSwapInterval(config.isVsync() ? 1 : 0);

        // Create OpenGL capabilities
        logger.debug("Creating OpenGL capabilities");
        GL.createCapabilities();

        // Check OpenGL version
        String version = GL11.glGetString(GL11.GL_VERSION);
        logger.info("OpenGL Version: {}", version);

        contextReady = true;

        // Make window visible
        logger.debug("Making window visible");
        glfwShowWindow(handle);

        // Log initial GL error state
        int error = GL11.glGetError();
        if (error != GL11.GL_NO_ERROR) {
            logger.error("OpenGL error after initialization: 0x{}", Integer.toHexString(error));
        } else {
            logger.debug("No OpenGL errors after initialization");
        }
    }


    public void toggleFullscreen() {
        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

        if (!isFullscreen) {
            // Save current window position and size
            int[] xpos = new int[1], ypos = new int[1];
            glfwGetWindowPos(handle, xpos, ypos);
            savedPos[0] = xpos[0];
            savedPos[1] = ypos[0];
            savedSize[0] = config.getWidth();
            savedSize[1] = config.getHeight();

            // Switch to fullscreen
            glfwSetWindowMonitor(handle, glfwGetPrimaryMonitor(), 0, 0,
                    vidmode.width(), vidmode.height(), vidmode.refreshRate());
        } else {
            // Restore windowed mode
            glfwSetWindowMonitor(handle, NULL,
                    savedPos[0], savedPos[1],
                    savedSize[0], savedSize[1],
                    GLFW_DONT_CARE);
        }

        isFullscreen = !isFullscreen;
    }

    public void update() {
        if (!contextReady) {
            logger.error("Attempting to update window before context is ready");
            return;
        }

        try {
            glfwSwapBuffers(handle);

            int error = GL11.glGetError();
            if (error != GL11.GL_NO_ERROR) {
                logger.error("OpenGL error during buffer swap: 0x{}", Integer.toHexString(error));
            }

            glfwPollEvents();
            eventBus.update();
            inputManager.update();
        } catch (Exception e) {
            logger.error("Error during window update", e);
        }
    }

    public void cleanup() {
        callbacks.cleanup();
        glfwFreeCallbacks(handle);
        glfwDestroyWindow(handle);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    public boolean shouldClose() {
        return handle != NULL && glfwWindowShouldClose(handle);
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public InputManager getInputManager() {
        return inputManager;
    }

    public long getHandle() {
        return handle;
    }

    /**
     * Gets the current window width. This will reflect any resize operations.
     */
    public static int getWidth() {
        return currentWidth;
    }

    /**
     * Gets the current window height. This will reflect any resize operations.
     */
    public static int getHeight() {
        return currentHeight;
    }
}
