package engine.core.time;

import static org.lwjgl.glfw.GLFW.glfwGetTime;

public class Time {
    private static double lastFrame = 0.0;
    private static float deltaTime = 0.0f;
    private static float timeScale = 1.0f;
    public static final float FIXED_TIME_STEP = 1.0f / 60.0f;
    private static float accumulator = 0.0f;

    public static void update() {
        double currentFrame = glfwGetTime();

        // Handle first update
        if (lastFrame == 0.0) {
            deltaTime = 0.0f;
        } else {
            deltaTime = (float)(currentFrame - lastFrame) * timeScale;
        }

        lastFrame = currentFrame;
        accumulator += deltaTime;
    }

    public static void setTimeScale(float scale) {
        timeScale = scale;
    }

    public static float getDeltaTime() {
        return deltaTime;
    }

    public static float getTimeScale() {
        return timeScale;
    }

    public static float getFixedTimeStep() {
        return FIXED_TIME_STEP;
    }

    public static float getAccumulator() {
        return accumulator;
    }

    public static void subtractAccumulator(float step) {
        accumulator = Math.max(0.0f, accumulator - step);
    }

    public static double getRawTime() {
        return glfwGetTime();
    }

    public static void reset() {
        lastFrame = 0.0;
        deltaTime = 0.0f;
        accumulator = 0.0f;
    }

    public static void setDeltaTime(float deltaTime) {
        Time.deltaTime = deltaTime;
    }
}
