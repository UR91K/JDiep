//DebugMenu.java
//LEGACY IMPLEMENTATION
//USE THIS FOR REFERENCE
package main;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL30.*;

public class DebugMenu {
    private boolean isVisible = false;
    private final Player player;
    private final CameraHandler camera;
    private final TextRenderer textRenderer;
    private final ShaderHandler shader;
    private Matrix4f orthoMatrix;

    // Menu layout
    private static final float LINE_HEIGHT = 20.0f;
    private static final float MENU_X = 10.0f;
    private static final float MENU_Y = 25.0f;
    private static final float SECTION_SPACING = 20.0f;
    private static final float SLIDER_WIDTH = 150.0f;
    private static final float SLIDER_HEIGHT = 4.0f;
    private static final float HANDLE_WIDTH = 8.0f;
    private static final float HANDLE_HEIGHT = 16.0f;
    private static final float SLIDER_VERTICAL_OFFSET = 5.0f;


    // Menu sections vertical positions
    private float currentY = MENU_Y;

    // Active slider tracking
    private String activeSlider = null;
    private float sliderGrabOffset = 0;

    public DebugMenu(Player player, CameraHandler camera,
                     TextRenderer textRenderer) {
        System.out.println("Debug Menu initialized"); // Debug print
        this.player = player;
        this.camera = camera;
        this.shader = new ShaderHandler();
        this.textRenderer = textRenderer;  // Use the provided textRenderer instead of creating new one
        updateProjection(1280, 720); // Default size, will be updated when window resizes
    }

    public void updateProjection(int windowWidth, int windowHeight) {
        // Create orthographic projection for UI
        this.orthoMatrix = new Matrix4f().ortho2D(0, windowWidth, windowHeight, 0);
    }

    public void toggleVisibility() {
        isVisible = !isVisible;
        System.out.println("Debug Menu visibility toggled: " + isVisible); // Debug print
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void render(int windowWidth, int windowHeight) {
        if (!isVisible) return;

        Matrix4f orthoProjection = new Matrix4f().ortho(
                0, windowWidth,
                windowHeight, 0,
                -1f, 1f
        );

        currentY = MENU_Y;

        // Title
        renderText("Debug Menu", MENU_X, currentY, new Vector4f(1.0f, 1.0f, 0.5f, 1.0f));
        currentY += textRenderer.getLineHeight() + SECTION_SPACING;

        // Tank Properties Section
        renderText("Tank Properties:", MENU_X, currentY, new Vector4f(0.5f, 1.0f, 0.5f, 1.0f));
        currentY += textRenderer.getLineHeight();

        renderSlider("Mass", player.getMass(), 0.5f, 3.0f);
        String posText = String.format("Position: (%.1f, %.1f)", player.getPosition().x, player.getPosition().y);
        renderText(posText, MENU_X, currentY, new Vector4f(1.0f));
        currentY += textRenderer.getLineHeight();

        String velText = String.format("Velocity: (%.1f, %.1f)", player.getVelocity().x, player.getVelocity().y);
        renderText(velText, MENU_X, currentY, new Vector4f(1.0f));
        currentY += textRenderer.getLineHeight() + SECTION_SPACING;

        // Camera Settings Section
        renderText("Camera Settings:", MENU_X, currentY, new Vector4f(0.5f, 1.0f, 0.5f, 1.0f));
        currentY += textRenderer.getLineHeight();

        renderSlider("Zoom", camera.getZoom(), GameConstants.CAMERA_MIN_ZOOM, GameConstants.CAMERA_MAX_ZOOM);
        renderSlider("Spring Stiffness", GameConstants.CAMERA_SPRING_STIFFNESS, 1.0f, 10.0f);
        renderSlider("Spring Damping", GameConstants.CAMERA_SPRING_DAMPING, 1.0f, 10.0f);
    }

    private void renderSlider(String label, float value, float min, float max) {
        float lineHeight = textRenderer.getLineHeight();

        // Render label and value on the same line
        String text = String.format("%s: %.2f", label, value);
        renderText(text, MENU_X, currentY, new Vector4f(1.0f));

        // Calculate slider position
        float sliderY = currentY + lineHeight + SLIDER_VERTICAL_OFFSET;

        // Set shader for geometry rendering
        shader.useShaderProgram();

        // Draw slider track (background)
        float[] trackVertices = {
                MENU_X, sliderY - SLIDER_HEIGHT/2,
                MENU_X + SLIDER_WIDTH, sliderY - SLIDER_HEIGHT/2,
                MENU_X + SLIDER_WIDTH, sliderY + SLIDER_HEIGHT/2,
                MENU_X, sliderY + SLIDER_HEIGHT/2
        };

        int trackVAO = glGenVertexArrays();
        int trackVBO = glGenBuffers();

        glBindVertexArray(trackVAO);
        glBindBuffer(GL_ARRAY_BUFFER, trackVBO);
        glBufferData(GL_ARRAY_BUFFER, trackVertices, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);

        shader.setUniform("color", new Vector4f(0.3f, 0.3f, 0.3f, 1.0f));
        glDrawArrays(GL_TRIANGLE_FAN, 0, 4);

        // Calculate and draw handle
        float handlePos = MENU_X + (value - min) / (max - min) * SLIDER_WIDTH;
        float[] handleVertices = {
                handlePos - HANDLE_WIDTH/2, sliderY - HANDLE_HEIGHT/2,
                handlePos + HANDLE_WIDTH/2, sliderY - HANDLE_HEIGHT/2,
                handlePos + HANDLE_WIDTH/2, sliderY + HANDLE_HEIGHT/2,
                handlePos - HANDLE_WIDTH/2, sliderY + HANDLE_HEIGHT/2
        };

        int handleVAO = glGenVertexArrays();
        int handleVBO = glGenBuffers();

        glBindVertexArray(handleVAO);
        glBindBuffer(GL_ARRAY_BUFFER, handleVBO);
        glBufferData(GL_ARRAY_BUFFER, handleVertices, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);

        shader.setUniform("color", new Vector4f(0.8f, 0.8f, 0.8f, 1.0f));
        glDrawArrays(GL_TRIANGLE_FAN, 0, 4);

        // Cleanup
        glDeleteBuffers(trackVBO);
        glDeleteVertexArrays(trackVAO);
        glDeleteBuffers(handleVBO);
        glDeleteVertexArrays(handleVAO);

        // Update currentY for next element
        currentY = sliderY + HANDLE_HEIGHT + SECTION_SPACING;
    }

    private boolean isMouseOverSlider(Vector2f mousePos, float sliderY) {
        return mousePos.x >= MENU_X && mousePos.x <= MENU_X + SLIDER_WIDTH &&
                mousePos.y >= sliderY - 10 && mousePos.y <= sliderY + 10;
    }

    private float getValueFromSliderPosition(float mouseX, float min, float max) {
        float t = (mouseX - MENU_X) / SLIDER_WIDTH;
        t = Math.max(0, Math.min(1, t));
        return min + t * (max - min);
    }

    private void renderText(String text, float x, float y, Vector4f color) {
        textRenderer.renderText(orthoMatrix, text, x, y, color);
    }

    private void renderRect(float x, float y, float width, float height, Vector4f color, Matrix4f projection) {
        // Implementation for rendering rectangles (for sliders)
        // You'll need to implement this using your shader system
    }

    private boolean isOverSlider(Vector2f mousePos, float sliderY) {
        float actualSliderY = sliderY + SLIDER_HEIGHT/2;
        return mousePos.x >= MENU_X && mousePos.x <= MENU_X + SLIDER_WIDTH &&
                mousePos.y >= actualSliderY - HANDLE_HEIGHT/2 &&
                mousePos.y <= actualSliderY + HANDLE_HEIGHT/2;
    }

    private float getSliderValue(float mouseX, float min, float max) {
        float t = (mouseX - MENU_X) / SLIDER_WIDTH;
        t = Math.max(0, Math.min(1, t));
        return min + t * (max - min);
    }

    // Make sure to add cleanup
    public void cleanup() {
        if (shader != null) shader.cleanup();
    }

    public void updateSliders(Vector2f mousePos) {
        if (!isVisible) return;

        // Reset currentY for checking slider positions
        float checkY = MENU_Y + LINE_HEIGHT + SECTION_SPACING + LINE_HEIGHT;

        // Tank Mass slider
        if (isOverSlider(mousePos, checkY)) {
            float newMass = getSliderValue(mousePos.x, 0.5f, 3.0f);
            player.setMass(newMass);
        }
        checkY += LINE_HEIGHT * 4 + SECTION_SPACING + LINE_HEIGHT;

        // Camera Zoom slider
        if (isOverSlider(mousePos, checkY)) {
            float newZoom = getSliderValue(mousePos.x,
                    GameConstants.CAMERA_MIN_ZOOM, GameConstants.CAMERA_MAX_ZOOM);
            camera.setZoom(newZoom);
        }
        checkY += LINE_HEIGHT * 2;

        // Spring Stiffness slider
        if (isOverSlider(mousePos, checkY)) {
            float newStiffness = getSliderValue(mousePos.x, 1.0f, 10.0f);
            camera.setSpringStiffness(newStiffness);
        }
        checkY += LINE_HEIGHT * 2;

        // Spring Damping slider
        if (isOverSlider(mousePos, checkY)) {
            float newDamping = getSliderValue(mousePos.x, 1.0f, 10.0f);
            camera.setDamping(newDamping);
        }
    }
}