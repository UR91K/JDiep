package engine.core;

import engine.core.logging.Logger;
import engine.core.time.Time;

public class GameLoop {
    private static final Logger logger = Logger.getLogger(GameLoop.class);
    private static final float MIN_DELTA_TIME = 0.0001f;
    private static final float MAX_DELTA_TIME = 0.1f;
    private static final int MAX_FIXED_UPDATES = 5;

    private final java.util.function.BooleanSupplier isRunning;
    private final java.util.function.Consumer<Float> updateCallback;
    private final java.util.function.Consumer<Float> fixedUpdateCallback;
    private final java.util.function.Consumer<Float> renderCallback;

    private boolean firstFrame = true;
    private float accumulator = 0.0f;
    private int frameCount = 0;

    public GameLoop(
            java.util.function.BooleanSupplier isRunning,
            java.util.function.Consumer<Float> update,
            java.util.function.Consumer<Float> fixedUpdate,
            java.util.function.Consumer<Float> render) {

        // Verify callbacks aren't null
        if (isRunning == null)
            throw new IllegalArgumentException("isRunning callback cannot be " +
                    "null");
        if (update == null)
            throw new IllegalArgumentException("update callback cannot be " +
                    "null");
        if (fixedUpdate == null)
            throw new IllegalArgumentException("fixedUpdate callback cannot " +
                    "be null");
        if (render == null)
            throw new IllegalArgumentException("render callback cannot be " +
                    "null");

        logger.info("Creating game loop with callbacks:");
        logger.info("- isRunning: {}", isRunning.getClass().getName());
        logger.info("- update: {}", update.getClass().getName());
        logger.info("- fixedUpdate: {}", fixedUpdate.getClass().getName());
        logger.info("- render: {}", render.getClass().getName());

        this.isRunning = isRunning;
        this.updateCallback = update;
        this.fixedUpdateCallback = fixedUpdate;
        this.renderCallback = render;
    }

    public void run() {
        logger.info("Starting game loop");
        Time.reset();
        double lastTime = Time.getRawTime();

        while (isRunning.getAsBoolean()) {
            frameCount++;

            try {
                double currentTime = Time.getRawTime();
                float deltaTime = (float) (currentTime - lastTime);
                lastTime = currentTime;

                if (firstFrame) {
                    logger.debug("Processing first frame");
                    deltaTime = MIN_DELTA_TIME;
                    firstFrame = false;
                }

                // Cap delta time
                if (deltaTime > MAX_DELTA_TIME) {
                    logger.warn("Large delta time detected: {}s, capping to: " +
                                    "{}s",
                            deltaTime, MAX_DELTA_TIME);
                    deltaTime = MAX_DELTA_TIME;
                } else if (deltaTime < MIN_DELTA_TIME) {
                    deltaTime = MIN_DELTA_TIME;
                }

                Time.setDeltaTime(deltaTime);
                accumulator += deltaTime;

                // Regular update
                logger.trace("Frame {} - Executing update callback",
                        frameCount);
                try {
                    updateCallback.accept(deltaTime);
                } catch (Exception e) {
                    logger.error("Error in update callback", e);
                }

                // Fixed timestep updates
                int updateCount = 0;
                while (accumulator >= Time.FIXED_TIME_STEP && updateCount < MAX_FIXED_UPDATES) {
                    logger.trace("Frame {} - Executing fixed update {}",
                            frameCount, updateCount + 1);
                    try {
                        fixedUpdateCallback.accept(Time.FIXED_TIME_STEP);
                    } catch (Exception e) {
                        logger.error("Error in fixed update callback", e);
                    }
                    accumulator -= Time.FIXED_TIME_STEP;
                    updateCount++;
                }

                if (accumulator > Time.FIXED_TIME_STEP) {
                    accumulator = 0;
                }

                // Render - with explicit verification
                logger.trace("Frame {} - About to execute render callback",
                        frameCount);
                if (renderCallback == null) {
                    logger.error("Frame {} - Render callback is null!",
                            frameCount);
                } else {
                    try {
                        renderCallback.accept(deltaTime);
                        logger.trace("Frame {} - Render callback completed",
                                frameCount);
                    } catch (Exception e) {
                        logger.error("Error in render callback", e);
                        e.printStackTrace(); // Print full stack trace
                    }
                }

                // Log status periodically
                if (frameCount % 100 == 0) {
                    logger.debug("Processed {} frames", frameCount);
                }

            } catch (Exception e) {
                logger.error("Critical error in game loop frame {}",
                        frameCount, e);
                e.printStackTrace();
            }
        }

        logger.info("Game loop ended after {} frames", frameCount);
    }
}