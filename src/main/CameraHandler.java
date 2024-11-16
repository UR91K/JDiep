//CameraHandler.java
//LEGACY IMPLEMENTATION
//USE THIS FOR REFERENCE
package main;

import org.joml.Matrix4f;
import org.joml.Vector2f;

class CameraHandler {
    private Vector2f position;
    private Vector2f velocity;
    private Vector2f target;
    private float springStiffness = GameConstants.CAMERA_SPRING_STIFFNESS;
    private float damping = GameConstants.CAMERA_SPRING_DAMPING;
    private float deadzone = GameConstants.CAMERA_DEADZONE;
    private float zoomLevel = GameConstants.CAMERA_DEFAULT_ZOOM;

    public CameraHandler(Vector2f startPosition) {
        this.position = new Vector2f(startPosition);
        this.velocity = new Vector2f(0, 0);
        this.target = new Vector2f(startPosition);
    }

    public void update(Vector2f playerPosition, float deltaTime) {
        // update target pos
        target.set(playerPosition);

        // calculate spring force (F = -kx)
        float dx = position.x - target.x;
        float dy = position.y - target.y;

        // apply spring force
        float fx = -springStiffness * dx;
        float fy = -springStiffness * dy;

        // apply damping force (F = -cv)
        fx -= damping * velocity.x;
        fy -= damping * velocity.y;

        // update velocity (a = F/m, assuming mass = 1)
        velocity.x += fx * deltaTime;
        velocity.y += fy * deltaTime;

        // update pos
        position.x += velocity.x * deltaTime;
        position.y += velocity.y * deltaTime;

        // Stop movement if below threshold
        float speed = (float) Math.sqrt(velocity.x * velocity.x + velocity.y * velocity.y);
        if (speed < GameConstants.CAMERA_MIN_SPEED_THRESHOLD) {
            velocity.zero();
        }
    }

    public void setSpringStiffness(float stiffness) {
        this.springStiffness = stiffness;
    }

    public void setDamping(float damping) {
        this.damping = damping;
    }

    public Matrix4f getViewMatrix() {
        // To zoom around the camera target (player):
        // 1. Translate to origin
        // 2. Apply zoom
        // 3. Translate back
        return new Matrix4f()
                .translate(-position.x, -position.y, 0.0f)            // Move to origin
                .translate(position.x, position.y, 0.0f)             // Move back to position
                .scale(zoomLevel)                                    // Apply zoom
                .translate(-position.x, -position.y, 0.0f);          // Apply camera offset
    }

    public void setZoom(float zoom) {
        this.zoomLevel = Math.max(0.1f, zoom);
    }

    public void adjustZoom(float factor) {
        float newZoom = this.zoomLevel * factor;
        if (newZoom >= GameConstants.CAMERA_MIN_ZOOM && newZoom <= GameConstants.CAMERA_MAX_ZOOM) {
            this.zoomLevel = newZoom;
        }
    }

    public void setDeadzone(float deadzone) {
        this.deadzone = deadzone;
    }

    public Vector2f getPosition() {
        return new Vector2f(position);
    }

    public float getZoom() {
        return zoomLevel;
    }

    public Vector2f getTarget() {
        return new Vector2f(target);
    }

    public void setPosition(Vector2f newPosition) {
        position.set(newPosition);
        target.set(newPosition);
        velocity.zero();
    }

    public Vector2f screenToWorld(float screenX, float screenY, int windowWidth, int windowHeight) {
        float ndcX = (2.0f * screenX) / windowWidth - 1.0f;
        float ndcY = 1.0f - (2.0f * screenY) / windowHeight;

        float aspectRatio = (float) windowWidth / windowHeight;
        float viewWidth = GameConstants.DEFAULT_VIEW_SIZE * aspectRatio / zoomLevel;
        float viewHeight = GameConstants.DEFAULT_VIEW_SIZE / zoomLevel;

        return new Vector2f(
                position.x + (ndcX * viewWidth),
                position.y + (ndcY * viewHeight)
        );
    }
}