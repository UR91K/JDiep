package engine.core.graphics;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import engine.core.time.Time;
import engine.core.window.Window;
import engine.core.logging.Logger;

public class Camera {
    private static final Logger logger = Logger.getLogger(Camera.class);

    private Vector2f position;
    private Vector2f velocity;
    private Vector2f target;
    private float zoom;
    private float springStiffness;
    private float damping;
    private float minZoom;
    private float maxZoom;
    private float viewHeight;
    private Matrix4f viewMatrix;
    private Matrix4f projectionMatrix;
    private Matrix4f viewProjectionMatrix;
    private boolean matricesDirty;

    public Camera(Vector2f startPosition) {
        this.position = new Vector2f(startPosition);
        this.velocity = new Vector2f();
        this.target = new Vector2f(startPosition);
        this.zoom = 1.0f;
        this.springStiffness = 4.0f;
        this.damping = 3.0f;
        this.minZoom = 0.1f;
        this.maxZoom = 5.0f;
        this.viewHeight = 30.0f * 2.0f;
        this.viewMatrix = new Matrix4f();
        this.projectionMatrix = new Matrix4f();
        this.viewProjectionMatrix = new Matrix4f();
        this.matricesDirty = true;

        logger.info("Camera initialized at position={}, zoom={}", startPosition, zoom);
        logger.debug("Camera parameters: springStiffness={}, damping={}, viewHeight={}",
                springStiffness, damping, viewHeight);
    }

    public Vector2f screenToWorld(float screenX, float screenY) {
        logger.trace("Converting screen coordinates ({}, {}) to world coordinates", screenX, screenY);

        if (matricesDirty) {
            updateMatrices();
        }

        Matrix4f invViewProj = new Matrix4f(viewProjectionMatrix).invert();
        float ndcX = (2.0f * screenX) / Window.getWidth() - 1.0f;
        float ndcY = 1.0f - (2.0f * screenY) / Window.getHeight();
        Vector4f worldPos = new Vector4f(ndcX, ndcY, 0, 1.0f);
        worldPos.mul(invViewProj);

        Vector2f result = new Vector2f(worldPos.x, worldPos.y);
        logger.trace("Converted to world coordinates: {}", result);
        return result;
    }

    private void updateMatrices() {
        logger.trace("Updating camera matrices");

        viewMatrix.identity()
                .translate(-position.x, -position.y, 0.0f)
                .scale(zoom);

        float aspectRatio = (float)Window.getWidth() / Window.getHeight();
        float viewWidth = viewHeight * aspectRatio;
        projectionMatrix.identity().ortho(
                -viewWidth/2,
                viewWidth/2,
                -viewHeight/2,
                viewHeight/2,
                -1.0f,
                1.0f
        );

        viewProjectionMatrix.set(projectionMatrix).mul(viewMatrix);
        matricesDirty = false;

        logger.trace("Matrices updated: aspectRatio={}, viewWidth={}", aspectRatio, viewWidth);
    }

    public void update() {
        float deltaTime = Time.getDeltaTime();
        logger.trace("Updating camera position, deltaTime={}", deltaTime);

        // Calculate spring force
        float dx = position.x - target.x;
        float dy = position.y - target.y;
        float fx = -springStiffness * dx;
        float fy = -springStiffness * dy;

        // Apply damping force
        fx -= damping * velocity.x;
        fy -= damping * velocity.y;

        // Update velocity and position
        velocity.x += fx * deltaTime;
        velocity.y += fy * deltaTime;
        Vector2f oldPosition = new Vector2f(position);
        position.x += velocity.x * deltaTime;
        position.y += velocity.y * deltaTime;

        float speedSquared = velocity.lengthSquared();
        if (speedSquared < 0.001f) {
            if (!velocity.equals(0, 0)) {
                logger.trace("Camera movement stopped due to low speed");
            }
            velocity.zero();
        } else {
            logger.trace("Camera moved: {} -> {}, velocity={}", oldPosition, position, velocity);
        }

        matricesDirty = true;
    }

    public void setTarget(Vector2f target) {
        logger.debug("Setting camera target: {} -> {}", this.target, target);
        this.target.set(target);
    }

    public void adjustZoom(float factor) {
        float newZoom = zoom * factor;
        logger.debug("Adjusting zoom: {} -> {}", zoom, newZoom);
        setZoom(newZoom);
    }

    public void setZoom(float zoom) {
        float oldZoom = this.zoom;
        this.zoom = Math.max(minZoom, Math.min(maxZoom, zoom));

        if (this.zoom != zoom) {
            logger.debug("Zoom clamped from {} to {} (limits: {}, {})",
                    zoom, this.zoom, minZoom, maxZoom);
        }

        if (oldZoom != this.zoom) {
            logger.info("Zoom changed: {} -> {}", oldZoom, this.zoom);
            matricesDirty = true;
        }
    }

    public void setPosition(Vector2f position) {
        logger.debug("Setting camera position: {} -> {}", this.position, position);
        this.position.set(position);
        matricesDirty = true;
    }

    public void setViewHeight(float height) {
        logger.debug("Setting view height: {} -> {}", this.viewHeight, height);
        this.viewHeight = height;
        matricesDirty = true;
    }

    public void setSpringStiffness(float stiffness) {
        logger.debug("Setting spring stiffness: {} -> {}", this.springStiffness, stiffness);
        this.springStiffness = stiffness;
    }

    public void setDamping(float damping) {
        logger.debug("Setting damping: {} -> {}", this.damping, damping);
        this.damping = damping;
    }

    public void setZoomLimits(float min, float max) {
        logger.info("Setting zoom limits: [{}, {}] -> [{}, {}]",
                this.minZoom, this.maxZoom, min, max);
        this.minZoom = min;
        this.maxZoom = max;
        this.zoom = Math.max(min, Math.min(max, zoom));
    }

    // Getter methods remain the same but with trace logging
    public Matrix4f getViewMatrix() {
        if (matricesDirty) {
            updateMatrices();
        }
        return viewMatrix;
    }

    public Matrix4f getProjectionMatrix() {
        if (matricesDirty) {
            updateMatrices();
        }
        return projectionMatrix;
    }

    public Matrix4f getViewProjectionMatrix() {
        if (matricesDirty) {
            updateMatrices();
        }
        return viewProjectionMatrix;
    }

    public Vector2f getPosition() {
        return new Vector2f(position);
    }
}