package main;

import org.joml.Vector2f;
import org.joml.Vector4f;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

public class DummyTank extends Tank {
    // Static color overrides for dummy tanks
    private static final Vector4f DUMMY_FILL_COLOR = GameConstants.rgb(100, 100, 100);  // Gray
    private static final Vector4f DUMMY_STROKE_COLOR = GameConstants.rgb(70, 70, 70);   // Dark gray
    private static final Vector4f DUMMY_TURRET_FILL_COLOR = GameConstants.rgb(80, 80, 80);
    private static final Vector4f DUMMY_TURRET_STROKE_COLOR = GameConstants.rgb(60, 60, 60);

    private static int dummyCount = 0;  // Keep track of dummy tanks for identification
    private final int dummyId;          // Unique identifier for this dummy tank

    public DummyTank(Vector2f startPos) {
        super(startPos,
                GameConstants.PLAYER_DEFAULT_RADIUS,
                GameConstants.PLAYER_TURRET_WIDTH,
                GameConstants.PLAYER_TURRET_LENGTH);

        // Assign unique ID
        this.dummyId = ++dummyCount;

        // Set name tag using shortened UUID
        setNameTag("Dummy-" + dummyId);

        // Set type
        setType(EntityType.TANK);

        // Disable movement
        this.moveSpeed = 0;
        this.friction = 0;
        this.velocity.zero();

        // Set fixed rotation (can be changed later if needed)
        this.rotation = 0;
    }

    @Override
    protected void renderBody(ShaderHandler shader) {
        glBindVertexArray(circleVAO);

        shader.setUniform("color", DUMMY_FILL_COLOR);
        glDrawArrays(GL_TRIANGLE_FAN, 0, 33);

        shader.setUniform("color", DUMMY_STROKE_COLOR);
        glDrawArrays(GL_LINE_LOOP, 0, 33);
    }

    @Override
    protected void renderTurret(ShaderHandler shader) {
        glBindVertexArray(turretVAO);
        shader.setUniform("color", DUMMY_TURRET_FILL_COLOR);
        glDrawArrays(GL_TRIANGLE_FAN, 0, 5);

        shader.setUniform("color", DUMMY_TURRET_STROKE_COLOR);
        glDrawArrays(GL_LINE_LOOP, 1, 4);
    }

    @Override
    public void update(float deltaTime) {
        // Simplified update - only update collider positions
        Vector2f pos = getPositionDirect();
        bodyCollider.setCenter(pos);
        updateTurretTransform();
    }

    @Override
    protected void handleCollisions(float deltaTime) {
        // Only handle basic boundary collisions
        handleBodyCollisions();
        // Skip turret collisions for simplicity
    }

    public int getDummyId() {
        return dummyId;
    }

    public void setRotation(float newRotation) {
        this.rotation = newRotation;
        updateTurretTransform();
    }

    // Static method to reset dummy count (useful for cleanup/testing)
    public static void resetDummyCount() {
        dummyCount = 0;
    }
}