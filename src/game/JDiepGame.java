package game;

import engine.core.GameLoop;
import engine.core.graphics.Camera;
import engine.core.graphics.Renderer;
import engine.core.graphics.Shader;
import engine.core.graphics.systems.GridRenderSystem;
import engine.core.graphics.systems.RenderSystem;
import engine.core.input.InputManager;
import engine.core.logging.Logger;
import engine.core.window.Window;
import engine.core.window.WindowConfig;
import engine.ecs.World;
import engine.event.EventBus;
import engine.physics.systems.*;
import game.components.LocalPlayerControlComponent;
import game.components.TankRendererComponent;
import game.systems.*;
import game.entities.tanks.*;
import org.joml.Vector2f;
import static org.lwjgl.glfw.GLFW.*;

public class JDiepGame {
    private static final Logger logger = Logger.getLogger(JDiepGame.class);
    private int frameCount = 0;

    // Core components
    private final Window window;
    private final World world;
    private final Camera camera;
    private final Renderer renderer;
    private final InputManager inputManager;
    private final EventBus eventBus;
    private final GameLoop gameLoop;

    // Game-specific components
    private LocalPlayerTank playerTank;
    private boolean isRunning = true;

    // Test mode configuration
    private final boolean testMode;
    private final int maxTestFrames;

    public JDiepGame() {
        this(false, 0);  // Normal game mode
    }

    public JDiepGame(boolean testMode, int maxTestFrames) {
        this.testMode = testMode;
        this.maxTestFrames = maxTestFrames;

        logger.info("===== Starting JDiep initialization =====");
        logger.info("Mode: {}", testMode ? "TEST" : "NORMAL");
        if (testMode) {
            logger.info("Test frames: {}", maxTestFrames);
        }

        try {
            // Create and initialize window
            logger.info("Creating window...");
            WindowConfig config = new WindowConfig.Builder()
                    .width(1280)
                    .height(720)
                    .title(testMode ? "JDiep [TEST MODE]" : "JDiep")
                    .vsync(true)
                    .build();

            window = new Window(config);
            logger.debug("Window instance created, initializing...");
            window.init();
            logger.info("Window created and initialized successfully");

            // Initialize core systems
            logger.debug("Getting event bus from window...");
            eventBus = window.getEventBus();
            logger.debug("Getting input manager from window...");
            inputManager = window.getInputManager();

            logger.debug("Creating world...");
            world = new World();

            logger.debug("Creating camera...");
            camera = new Camera(new Vector2f(0, 0));

            logger.debug("Creating renderer...");
            renderer = new Renderer(camera);
            renderer.setViewport(config.getWidth(), config.getHeight());

            logger.debug("Creating game loop...");
            gameLoop = new GameLoop(
                    this::isRunning,
                    this::update,
                    this::fixedUpdate,
                    this::render
            );

            logger.info("Core systems initialized, setting up game systems...");
            initializeSystems();

            logger.info("Setting up initial game state...");
            setupGame();

            logger.info("===== JDiep initialization complete =====");

        } catch (Exception e) {
            logger.error("Fatal error during JDiep initialization", e);
            throw new RuntimeException("Failed to initialize JDiep", e);
        }
    }

    private void initializeSystems() {
        var systems = new engine.ecs.System[] {
                new PhysicsSystem(eventBus),
                new CollisionSystem(eventBus),
                new MovementSystem(),
                new TankControlSystem(),
                new GridRenderSystem(renderer, camera),  // Add before RenderSystem
                new RenderSystem(renderer, camera)
        };

        for (var system : systems) {
            logger.info("Adding system: {}", system.getClass().getSimpleName());
            world.addSystem(system);
        }
    }

    private void setupGame() {
        // Add to setupGame() method
        logger.info("Setting up appropriate logging levels...");
        Logger.setClassLevel(Window.class, Logger.Level.ERROR);
        Logger.setClassLevel(World.class, Logger.Level.ERROR);
        Logger.setClassLevel(Shader.class, Logger.Level.ERROR);
        Logger.setClassLevel(TankRendererComponent.class, Logger.Level.ERROR);
        Logger.setClassLevel(RenderSystem.class, Logger.Level.ERROR);

        Logger.setClassLevel(InputManager.class, Logger.Level.TRACE);
        Logger.setClassLevel(LocalPlayerControlComponent.class, Logger.Level.TRACE);
        Logger.setClassLevel(TankControlSystem.class, Logger.Level.TRACE);
        Logger.setClassLevel(MovementSystem.class, Logger.Level.TRACE);
        Logger.setClassLevel(PhysicsSystem.class, Logger.Level.TRACE);
        Logger.setClassLevel(BaseTank.class, Logger.Level.TRACE);



        try {
            logger.info("Creating player tank...");
            TankStats playerStats = new TankStats();
            playerTank = new LocalPlayerTank(
                    new Vector2f(0, 0),
                    playerStats,
                    "Player1",
                    inputManager,
                    camera
            );
            world.addEntity(playerTank);
            logger.info("Player tank created");

            logger.info("Creating dummy tanks...");
            createDummyTank(new Vector2f(200, 200));
            createDummyTank(new Vector2f(-200, -200));
            logger.info("Dummy tanks created");
        } catch (Exception e) {
            logger.error("Error during game setup:", e);
            throw new RuntimeException("Failed to setup game", e);
        }
    }

    public void render(float deltaTime) {
        frameCount++;
        logger.debug("===== RENDER FRAME {} START (deltaTime={}) =====",
                frameCount, deltaTime);

        try {
            logger.debug("Calling renderer.beginFrame()");
            renderer.beginFrame();

            var systems = world.getSystems();
            logger.debug("Found {} total systems for rendering", systems.size());

            // Update render systems
            updateSystems(deltaTime, sys -> sys.isRenderSystem() || sys instanceof RenderSystem);

            logger.debug("Calling renderer.endFrame()");
            renderer.endFrame();

            logger.debug("Calling window.update()");
            window.update();

            logger.debug("===== RENDER FRAME {} COMPLETE =====", frameCount);
        } catch (Exception e) {
            logger.error("Error during render frame {}", frameCount, e);
            throw new RuntimeException("Render failed", e);
        }
    }

    private void update(float deltaTime) {
        try {
            if (testMode) {
                handleTestInput();
            }

            // Update camera to follow player
            camera.setTarget(playerTank.getPosition());
            camera.update();

            // Update non-physics, non-render systems
            updateSystems(deltaTime, sys ->
                    !(sys.isPhysicsSystem() || sys instanceof RenderSystem));
        } catch (Exception e) {
            logger.error("Error during update", e);
        }
    }

    private void fixedUpdate(float fixedDeltaTime) {
        try {
            updateSystems(fixedDeltaTime, sys -> sys.isPhysicsSystem());
        } catch (Exception e) {
            logger.error("Error during fixed update", e);
        }
    }

    private void updateSystems(float deltaTime, java.util.function.Predicate<engine.ecs.System> systemPredicate) {
        try {
            logger.debug("updateSystems - deltaTime={}", deltaTime);
            int count = 0;
            for (var system : world.getSystems()) {
                if (system != null && system.isEnabled() && systemPredicate.test(system)) {
                    logger.debug("Updating system: {}", system.getClass().getSimpleName());
                    try {
                        system.update(deltaTime);
                        count++;
                    } catch (Exception e) {
                        logger.error("Error updating system: {}",
                                system.getClass().getSimpleName(), e);
                        throw e;
                    }
                }
            }
            logger.debug("Updated {} systems", count);
        } catch (Exception e) {
            logger.error("Error in updateSystems", e);
            throw new RuntimeException("Failed to update systems", e);
        }
    }

    public void run() {
        try {
            logger.info("Starting game loop");
            gameLoop.run();
        } catch (Exception e) {
            logger.error("Fatal error during game loop:", e);
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        logger.info("Cleaning up resources");
        if (world != null) world.cleanup();
        if (renderer != null) renderer.cleanup();
        if (window != null) window.cleanup();
    }

    private boolean isRunning() {
        // In test mode, stop after max frames
        if (testMode && frameCount >= maxTestFrames) {
            logger.info("Test mode: Reached {} frames, stopping game", maxTestFrames);
            isRunning = false;
        }
        return isRunning && !window.shouldClose();
    }

    private void createDummyTank(Vector2f position) {
        DummyTank dummy = new DummyTank(position, new TankStats());
        world.addEntity(dummy);
    }

    public void enableDebugLogging(Class<?>... classes) {
        for (Class<?> clazz : classes) {
            Logger.setClassLevel(clazz, Logger.Level.DEBUG);
        }
    }

    public void enableTraceLogging(Class<?>... classes) {
        for (Class<?> clazz : classes) {
            Logger.setClassLevel(clazz, Logger.Level.TRACE);
        }
    }

    public void resetLogging(Class<?>... classes) {
        if (classes.length == 0) {
            Logger.resetAllClassLevels();
        } else {
            for (Class<?> clazz : classes) {
                Logger.resetClassLevel(clazz);
            }
        }
    }

    private void handleTestInput() {
        if (!testMode) return;

        logger.info("=== Running Input Test Sequence ===");
        switch (frameCount) {
            case 1:
                logger.info("Test frame 1: Simulating 'A' key press (move left)");
                inputManager.handleKeyInput(GLFW_KEY_A, GLFW_PRESS, 0);
                break;
            case 2:
                logger.info("Test frame 2: Simulating 'A' key release");
                inputManager.handleKeyInput(GLFW_KEY_A, GLFW_RELEASE, 0);
                logger.info("Test frame 2: Simulating 'W' key press (move up)");
                inputManager.handleKeyInput(GLFW_KEY_W, GLFW_PRESS, 0);
                break;
            case 3:
                logger.info("Test frame 3: Simulating 'W' key release");
                inputManager.handleKeyInput(GLFW_KEY_W, GLFW_RELEASE, 0);
                logger.info("Test frame 3: Simulating 'D' key press (move right)");
                inputManager.handleKeyInput(GLFW_KEY_D, GLFW_PRESS, 0);
                break;
            case 4:
                logger.info("Test frame 4: Simulating 'D' key release");
                inputManager.handleKeyInput(GLFW_KEY_D, GLFW_RELEASE, 0);
                break;
        }
    }
}