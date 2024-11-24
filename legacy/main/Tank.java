package main;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

public abstract class Tank extends Entity {
    // Rendering meshes
    protected int circleVAO;
    protected int turretVAO;

    // Tank body properties
    protected float bodyRadius;  // Current scaled radius for body collision and rendering

    // Turret properties
    protected float rotation = 0.0f;
    protected float turretWidth;  // Current scaled width
    protected float turretLength; // Current scaled length
    protected float lastRotation = 0f;  // Add this as a class field
    protected static final float TURRET_PUSH_FORCE = 1000.0f;  // Tune this constant

    // Collision shapes
    protected CircleShape bodyCollider;
    protected PolygonShape turretCollider;

    // Physics properties
    protected float moveSpeed;
    protected float friction;
    protected float bounceFactor;

    protected String nameTag;
    protected static final Vector4f NAME_TAG_COLOR = new Vector4f(1.0f, 1.0f, 1.0f, 0.9f);
    protected static final float NAME_TAG_OFFSET_Y = -8.0f;
    protected static final float NAME_TAG_SCALE = 0.08f;
    protected TextRenderer textRenderer;


    public Tank(Vector2f position, float radius, float turretWidth, float turretLength) {
        super(position, radius);
        setType(EntityType.TANK);

        // Initialize dimensions
        this.bodyRadius = radius;
        this.turretWidth = turretWidth;
        this.turretLength = turretLength;

        // Create collision shapes
        bodyCollider = new CircleShape(bodyRadius);
        turretCollider = createTurretCollider();

        // Create rendering meshes
        createMeshes();

        // Default name tag to empty
        this.nameTag = "";
    }

    protected void updateMassScaling() {
        float scaleFactor = getMass();
        this.bodyRadius = GameConstants.PLAYER_DEFAULT_RADIUS * scaleFactor;
        this.turretWidth = GameConstants.PLAYER_TURRET_WIDTH * scaleFactor;
        this.turretLength = GameConstants.PLAYER_TURRET_LENGTH * scaleFactor;

        // Update colliders
        if (bodyCollider != null) {
            bodyCollider.setRadius(bodyRadius);
        }
        if (turretCollider != null) {
            turretCollider = createTurretCollider(); // Recreate with new dimensions
        }
    }

    protected PolygonShape createTurretCollider() {
        // Create a rectangle shape for the turret
        float offset = calculateTurretOffset();
        float startX = bodyRadius - offset;
        float endX = startX + turretLength;

        Vector2f[] vertices = new Vector2f[4];
        vertices[0] = new Vector2f(startX, -turretWidth/2);
        vertices[1] = new Vector2f(endX, -turretWidth/2);
        vertices[2] = new Vector2f(endX, turretWidth/2);
        vertices[3] = new Vector2f(startX, turretWidth/2);

        return new PolygonShape(vertices);
    }

    @Override
    public void setMass(float newMass) {
        super.setMass(newMass);
        updateMassScaling();
        // Recreate meshes with new dimensions
        createMeshes();
    }

    protected void createMeshes() {
        createCircleMesh();
        createTurretMesh();
    }

    protected float calculateTurretOffset() {
        float r = bodyRadius;
        float A = turretWidth;

        // Prevent invalid math if turret width is greater than diameter
        if (A > 2 * r) {
            System.out.println("Warning: Turret width (" + A + ") is greater than tank diameter (" + (2*r) + ")");
            A = 2 * r;
        }

        float offset = r - (float)Math.sqrt(r*r - (A/2)*(A/2));

        // Debug output
        System.out.println("Tank radius: " + r);
        System.out.println("Turret width: " + A);
        System.out.println("Calculated offset: " + offset);

        return offset;
    }

    protected void createCircleMesh() {
        int segments = 32;
        float[] vertices = new float[(segments + 1) * 2];

        for (int i = 0; i <= segments; i++) {
            float angle = (float) (2 * Math.PI * i / segments);
            vertices[i * 2] = (float) Math.cos(angle) * bodyRadius;
            vertices[i * 2 + 1] = (float) Math.sin(angle) * bodyRadius;
        }

        circleVAO = glGenVertexArrays();
        int VBO = glGenBuffers();

        glBindVertexArray(circleVAO);
        glBindBuffer(GL_ARRAY_BUFFER, VBO);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
    }

    protected void createTurretMesh() {
        float offset = calculateTurretOffset();
        System.out.println("Applied offset: " + offset);

        // The turret should start at radius - offset (the intersection point)
        float startX = bodyRadius - offset;
        // And extend outward from there
        float endX = startX + turretLength;

        float[] vertices = {
                startX, 0,                      // Center
                startX, -turretWidth/2,         // Bottom base (starts at circle intersection)
                endX, -turretWidth/2,           // Right bottom (extends outward)
                endX, turretWidth/2,            // Right top (extends outward)
                startX, turretWidth/2           // Top base (starts at circle intersection)
        };

        // Debug vertices
        System.out.println("Turret vertices:");
        for (int i = 0; i < vertices.length; i += 2) {
            System.out.println("Vertex " + (i/2) + ": (" + vertices[i] + ", " + vertices[i+1] + ")");
        }

        turretVAO = glGenVertexArrays();
        int VBO = glGenBuffers();

        glBindVertexArray(turretVAO);
        glBindBuffer(GL_ARRAY_BUFFER, VBO);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
    }

    public void render(ShaderHandler shader, Matrix4f viewProjectionMatrix) {
        shader.useShaderProgram();
        glLineWidth(GameConstants.STROKE_WIDTH);

        // Set projection matrix once
        shader.setUniform("projection", viewProjectionMatrix);

        // First render the turret (behind body)
        Matrix4f turretModel = new Matrix4f()
                .translate(position.x, position.y, 0)
                .rotate(rotation, 0, 0, 1);
        shader.setUniform("model", turretModel);
        renderTurret(shader);

        // Then render the body on top
        Matrix4f bodyModel = new Matrix4f()
                .translate(position.x, position.y, 0);
        shader.setUniform("model", bodyModel);
        renderBody(shader);

        // Render name tag if set
        renderNameTag(viewProjectionMatrix);

        glLineWidth(1.0f);
    }

    protected void renderNameTag(Matrix4f viewProjectionMatrix) {
        if (nameTag != null && !nameTag.isEmpty() && textRenderer != null) {
            // Enable blending for text
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            float textWidth = textRenderer.getTextWidth(nameTag) * NAME_TAG_SCALE;
            float textHeight = textRenderer.getLineHeight() * NAME_TAG_SCALE;

            // Position above tank
            float yPos = position.y + (bodyRadius + NAME_TAG_OFFSET_Y);
            float xPos = position.x;

            // Create projection matrix for text
            Matrix4f textProjection = new Matrix4f(viewProjectionMatrix)
                    .translate(xPos, yPos, 0)      // Move to position
                    .scale(NAME_TAG_SCALE, -NAME_TAG_SCALE, 1.0f);  // Scale and flip Y

            // Center the text by offsetting by half its width
            float xOffset = -textWidth / (2 * NAME_TAG_SCALE);

            // Render text
            textRenderer.renderText(
                    textProjection,
                    nameTag,
                    xOffset,
                    0,
                    NAME_TAG_COLOR
            );

            // Restore GL state
            glDisable(GL_BLEND);
        }
    }

    protected void renderTurret(ShaderHandler shader) {
        glBindVertexArray(turretVAO);
        shader.setUniform("color", GameConstants.TURRET_FILL_COLOR);
        glDrawArrays(GL_TRIANGLE_FAN, 0, 5);

        shader.setUniform("color", GameConstants.TURRET_STROKE_COLOR);
        glDrawArrays(GL_LINE_LOOP, 1, 4);
    }

    protected void renderBody(ShaderHandler shader) {
        glBindVertexArray(circleVAO);

        shader.setUniform("color", GameConstants.PLAYER_FILL_COLOR);
        glDrawArrays(GL_TRIANGLE_FAN, 0, 33);

        shader.setUniform("color", GameConstants.PLAYER_STROKE_COLOR);
        glDrawArrays(GL_LINE_LOOP, 0, 33);
    }

    protected void updateTurretTransform() {
        Vector2f tankPos = getPositionDirect();
        turretCollider.setCenter(tankPos);
        turretCollider.rotate(rotation);
    }

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);

        // Update collider positions
        Vector2f pos = getPositionDirect();
        bodyCollider.setCenter(pos);

        // Update turret position and rotation
        updateTurretTransform();

        // Handle collisions
        handleCollisions(deltaTime);
    }

    protected void updateMovement(Vector2f moveDir, float deltaTime) {
        // Basic movement physics
        Vector2f acceleration = new Vector2f(moveDir).mul(moveSpeed);
        velocity.add(new Vector2f(acceleration).mul(deltaTime));

        if (velocity.lengthSquared() > 0) {
            float speed = velocity.length();
            float drop = speed * friction * deltaTime;
            float newSpeed = Math.max(0, speed - drop);
            if (newSpeed != speed) {
                velocity.mul(newSpeed / speed);
            }
        }

        position.add(new Vector2f(velocity).mul(deltaTime));
    }

    protected void handleCollisions(float deltaTime) {
        handleBodyCollisions();
        handleTurretCollisions(deltaTime);
    }

    protected void handleBodyCollisions() {
        Vector2f pos = getPositionDirect();
        Vector2f vel = getVelocityDirect();
        boolean collidedX = false;
        boolean collidedY = false;
        Vector2f originalVelocity = new Vector2f(vel);

        // X-axis collision
        if (pos.x + bodyRadius > GameConstants.BOUNDARY_RIGHT) {
            pos.x = GameConstants.BOUNDARY_RIGHT - bodyRadius;
            collidedX = true;
        } else if (pos.x - bodyRadius < GameConstants.BOUNDARY_LEFT) {
            pos.x = GameConstants.BOUNDARY_LEFT + bodyRadius;
            collidedX = true;
        }

        // Y-axis collision
        if (pos.y + bodyRadius > GameConstants.BOUNDARY_TOP) {
            pos.y = GameConstants.BOUNDARY_TOP - bodyRadius;
            collidedY = true;
        } else if (pos.y - bodyRadius < GameConstants.BOUNDARY_BOTTOM) {
            pos.y = GameConstants.BOUNDARY_BOTTOM + bodyRadius;
            collidedY = true;
        }

        // Apply collision response - simpler now without wall sliding
        if (collidedX) {
            vel.x = -originalVelocity.x * bounceFactor;
        }
        if (collidedY) {
            vel.y = -originalVelocity.y * bounceFactor;
        }
    }

    protected void handleTurretCollisions(float deltaTime) {
        Vector2f pos = getPositionDirect();
        Vector2f vel = getVelocityDirect();
        Vector2f[] vertices = turretCollider.getWorldVertices();

        // Find collision boundaries
        float minX = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (Vector2f vertex : vertices) {
            minX = Math.min(minX, vertex.x);
            maxX = Math.max(maxX, vertex.x);
            minY = Math.min(minY, vertex.y);
            maxY = Math.max(maxY, vertex.y);
        }

        // Calculate rotation impact
        float rotationSpeed = (rotation - lastRotation) / deltaTime;
        float normalizedRotationSpeed = (float) Math.tanh(rotationSpeed * GameConstants.ROTATION_SPEED_SCALE);
        boolean isClockwise = rotationSpeed > 0;

        float totalMoment = GameConstants.calculateMomentOfInertia(turretWidth, turretLength, bodyRadius);
        float basePushForce = totalMoment * Math.abs(normalizedRotationSpeed) * GameConstants.BASE_FORCE_MULTIPLIER;

        Vector2f pushOffForce = new Vector2f();
        boolean collision = false;
        float pushAngle = 0.0f;  // Moved outside if blocks

        // Right wall collision
        if (maxX > GameConstants.BOUNDARY_RIGHT) {
            collision = true;
            Vector2f impactPoint = getImpactPoint(vertices, GameConstants.BOUNDARY_RIGHT);
            float leverageRatio = (1.0f + Math.abs(impactPoint.y - pos.y) / turretLength)
                    * GameConstants.LEVERAGE_MULTIPLIER;

            // For right wall: clockwise -> down, counterclockwise -> up
            pushAngle = isClockwise ? -(float)Math.PI/2 : (float)Math.PI/2;

            addPushForce(pushOffForce, pushAngle, basePushForce * leverageRatio);
            pos.x = GameConstants.BOUNDARY_RIGHT - (maxX - pos.x);
        }
        // Left wall collision
        else if (minX < GameConstants.BOUNDARY_LEFT) {
            collision = true;
            Vector2f impactPoint = getImpactPoint(vertices, GameConstants.BOUNDARY_LEFT);
            float leverageRatio = (1.0f + Math.abs(impactPoint.y - pos.y) / turretLength)
                    * GameConstants.LEVERAGE_MULTIPLIER;

            // For left wall: clockwise -> up, counterclockwise -> down
            pushAngle = isClockwise ? (float)Math.PI/2 : -(float)Math.PI/2;

            addPushForce(pushOffForce, pushAngle, basePushForce * leverageRatio);
            pos.x = GameConstants.BOUNDARY_LEFT + (pos.x - minX);
        }

        // Top wall collision
        if (maxY > GameConstants.BOUNDARY_TOP) {
            collision = true;
            Vector2f impactPoint = getImpactPoint(vertices, maxY);
            float leverageRatio = (1.0f + Math.abs(impactPoint.x - pos.x) / turretLength)
                    * GameConstants.LEVERAGE_MULTIPLIER;

            // For top wall: clockwise -> right, counterclockwise -> left
            pushAngle = isClockwise ? 0 : (float)Math.PI;  // INVERTED THIS LINE

            addPushForce(pushOffForce, pushAngle, basePushForce * leverageRatio);
            pos.y = GameConstants.BOUNDARY_TOP - (maxY - pos.y);
        }
        // Bottom wall collision
        else if (minY < GameConstants.BOUNDARY_BOTTOM) {
            collision = true;
            Vector2f impactPoint = getImpactPoint(vertices, minY);
            float leverageRatio = (1.0f + Math.abs(impactPoint.x - pos.x) / turretLength)
                    * GameConstants.LEVERAGE_MULTIPLIER;

            // For bottom wall: clockwise -> left, counterclockwise -> right
            pushAngle = isClockwise ? (float)Math.PI : 0;  // INVERTED THIS LINE

            addPushForce(pushOffForce, pushAngle, basePushForce * leverageRatio);
            pos.y = GameConstants.BOUNDARY_BOTTOM + (pos.y - minY);
        }

        if (collision) {
            // Calculate normal force component
            Vector2f normalForce = new Vector2f();
            if (maxX > GameConstants.BOUNDARY_RIGHT || minX < GameConstants.BOUNDARY_LEFT) {
                normalForce.x = maxX > GameConstants.BOUNDARY_RIGHT ? -1 : 1;
            }
            if (maxY > GameConstants.BOUNDARY_TOP || minY < GameConstants.BOUNDARY_BOTTOM) {
                normalForce.y = maxY > GameConstants.BOUNDARY_TOP ? -1 : 1;
            }
            normalForce.normalize().mul(basePushForce * GameConstants.NORMAL_FORCE_RATIO);

            // Scale the tangential (push-off) force
            pushOffForce.mul(GameConstants.TANGENTIAL_FORCE_RATIO);

            // Combine forces
            pushOffForce.add(normalForce);

            float massScale = GameConstants.MASS_SCALE / (getMass() + 1.0f);
            pushOffForce.mul(massScale * GameConstants.DAMPING_FACTOR);
            vel.add(pushOffForce);

            // Debug output
            System.out.println("Wall collision - Rotation: " + (isClockwise ? "Clockwise" : "Counter-clockwise") +
                    " Push angle: " + Math.toDegrees(pushAngle) +
                    " Force: " + pushOffForce);
        }

        lastRotation = rotation;
    }

    private void addPushForce(Vector2f currentForce, float angle, float magnitude) {
        currentForce.add(
                new Vector2f(
                        (float)Math.cos(angle) * magnitude,
                        (float)Math.sin(angle) * magnitude
                )
        );
    }

    // Helper method to find the impact point along a wall
    private Vector2f getImpactPoint(Vector2f[] vertices, float wallValue) {
        Vector2f impactPoint = new Vector2f();
        float closestDist = Float.POSITIVE_INFINITY;

        for (Vector2f vertex : vertices) {
            float dist = Math.abs(vertex.y - wallValue);  // For horizontal walls
            if (dist < closestDist) {
                closestDist = dist;
                impactPoint.set(vertex);
            }
        }

        return impactPoint;
    }

    public void cleanup() {
        glDeleteVertexArrays(circleVAO);
        glDeleteVertexArrays(turretVAO);
    }

    // Getters and setters
    public float getRotation() { return rotation; }
    public void setRotation(float rotation) { this.rotation = rotation; }
    public float getTurretWidth() { return turretWidth; }
    public float getTurretLength() { return turretLength; }
    public float getBodyRadius() { return bodyRadius; }
    // Add setter for text renderer
    public void setTextRenderer(TextRenderer textRenderer) {
        this.textRenderer = textRenderer;
    }

    // Add getter/setter for name tag
    public void setNameTag(String nameTag) {
        this.nameTag = nameTag;
    }

    public String getNameTag() {
        return nameTag;
    }
}
