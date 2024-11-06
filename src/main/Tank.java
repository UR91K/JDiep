package main;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

public abstract class Tank {
    protected Vector2f position;
    protected Vector2f velocity;
    protected float rotation = 0.0f;
    protected float radius;
    protected float turretWidth;  // Independent turret width
    protected float turretLength; // Independent turret length

    // Mesh data
    protected int circleVAO;
    protected int turretVAO;

    // Tank characteristics
    protected float moveSpeed;
    protected float friction;
    protected float bounceFactor;

    protected float mass = 1.0f;  // Default mass/scale factor
    protected float baseRadius;    // Store the original radius
    protected float baseTurretWidth;  // Store the original turret width
    protected float baseTurretLength; // Store the original turret length

    public Tank(Vector2f position, float radius, float turretWidth, float turretLength) {
        this.position = new Vector2f(position);
        this.velocity = new Vector2f(0, 0);

        // Store base dimensions
        this.baseRadius = radius;
        this.baseTurretWidth = turretWidth;
        this.baseTurretLength = turretLength;

        // Apply initial scaling
        updateMassScaling();

        createMeshes();
    }

    protected void updateMassScaling() {
        // Scale all dimensions based on mass
        this.radius = baseRadius * mass;
        this.turretWidth = baseTurretWidth * mass;
        this.turretLength = baseTurretLength * mass;
    }

    public void setMass(float newMass) {
        this.mass = newMass;
        updateMassScaling();
        // Recreate meshes with new dimensions
        createMeshes();
    }



    protected void createMeshes() {
        createCircleMesh();
        createTurretMesh();
    }

    protected float calculateTurretOffset() {
        float r = radius;
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
            vertices[i * 2] = (float) Math.cos(angle) * radius;
            vertices[i * 2 + 1] = (float) Math.sin(angle) * radius;
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
        float startX = radius - offset;
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

        glLineWidth(1.0f);
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

    public void update(Vector2f moveDir, float deltaTime) {
        updateMovement(moveDir, deltaTime);
        handleCollisions();
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

    protected void handleCollisions() {
        boolean collidedX = false;
        boolean collidedY = false;
        Vector2f originalVelocity = new Vector2f(velocity);

        // X-axis collision
        if (position.x + radius > GameConstants.BOUNDARY_RIGHT) {
            position.x = GameConstants.BOUNDARY_RIGHT - radius;
            collidedX = true;
        } else if (position.x - radius < GameConstants.BOUNDARY_LEFT) {
            position.x = GameConstants.BOUNDARY_LEFT + radius;
            collidedX = true;
        }

        // Y-axis collision
        if (position.y + radius > GameConstants.BOUNDARY_TOP) {
            position.y = GameConstants.BOUNDARY_TOP - radius;
            collidedY = true;
        } else if (position.y - radius < GameConstants.BOUNDARY_BOTTOM) {
            position.y = GameConstants.BOUNDARY_BOTTOM + radius;
            collidedY = true;
        }

        // Apply collision response
        if (collidedX) {
            velocity.x = -originalVelocity.x * bounceFactor;
            velocity.y = originalVelocity.y;
        }
        if (collidedY) {
            velocity.y = -originalVelocity.y * bounceFactor;
            velocity.x = originalVelocity.x;
        }
    }

    public void cleanup() {
        glDeleteVertexArrays(circleVAO);
        glDeleteVertexArrays(turretVAO);
    }

    // Getters and setters
    public Vector2f getPosition() { return new Vector2f(position); }

    public float getRadius() { return radius; }

    public float getMass() {
        return mass;
    }

    public Vector2f getVelocity() {
        return velocity;
    }
}
