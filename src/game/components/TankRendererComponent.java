package game.components;

import engine.core.graphics.components.RenderableComponent;
import engine.core.graphics.Renderer;
import engine.core.logging.Logger;
import engine.ecs.Entity;
import engine.physics.components.RigidBodyComponent;
import game.entities.tanks.BaseTank;
import org.joml.Vector2f;
import org.joml.Vector4f;
import engine.physics.components.Transform2DComponent;

public class TankRendererComponent extends RenderableComponent {
    private static final Vector4f TANK_BODY_FILL = new Vector4f(0.647f, 0.188f, 0.188f, 1.0f);
    private static final Vector4f TANK_BODY_STROKE = new Vector4f(0.447f, 0.088f, 0.088f, 1.0f);
    private static final Vector4f TANK_TURRET_FILL = new Vector4f(0.341f, 0.447f, 0.467f, 1.0f);
    private static final Vector4f TANK_TURRET_STROKE = new Vector4f(0.241f, 0.347f, 0.367f, 1.0f);
    private static final Vector4f DUMMY_FILL = new Vector4f(0.4f, 0.4f, 0.4f, 1.0f);
    private static final Vector4f DUMMY_STROKE = new Vector4f(0.3f, 0.3f, 0.3f, 1.0f);

    private static final Logger logger =
            Logger.getLogger(TankRendererComponent.class);

    private final BaseTank tank;

    public TankRendererComponent(BaseTank tank) {
        this.tank = tank;
        logger.debug("Created TankRendererComponent for {}", tank.getClass().getSimpleName());
    }

    @Override
    public void render(Renderer renderer) {
        Vector2f position = tank.getPosition();
        logger.debug("START TANK RENDER - tank pos: {}, transform pos: {}, rigidbody pos: {}",
                position,
                tank.getComponent(Transform2DComponent.class).getPosition(),
                tank.getComponent(RigidBodyComponent.class).getPosition());

        try {
            renderTurret(renderer, position);
            renderBody(renderer, position);

            logger.debug("END TANK RENDER - final position: {}", position);
        } catch (Exception e) {
            logger.error("Failed to render tank", e);
        }
    }

    private void renderBody(Renderer renderer, Vector2f position) {
        float radius = tank.getStats().radius;

        // Render body first (behind turret)
        Vector4f fillColor = tank.isDummy() ? DUMMY_FILL : TANK_BODY_FILL;
        Vector4f strokeColor = tank.isDummy() ? DUMMY_STROKE : TANK_BODY_STROKE;

        renderer.renderCircle(
                position.x,
                position.y,
                radius,
                fillColor
        );

        renderer.renderCircleOutline(
                position.x,
                position.y,
                radius,
                strokeColor,
                2.0f
        );
    }

    private void renderTurret(Renderer renderer, Vector2f position) {
        float radius = tank.getStats().radius;
        float width = radius * tank.getStats().turretWidthRatio;
        float length = radius * tank.getStats().turretLengthRatio;

        // Calculate proper chord intersection
        float halfWidth = width / 2.0f;
        float offset = calculateTurretOffset(radius, width);

        // The chord intersects the circle at y = ±halfWidth
        // x coordinate of intersection is radius - offset
        float intersectX = radius - offset;

        // Calculate center of quad relative to tank center
        // We want the left edge of the quad to align with the chord
        float turretCenterX = intersectX + length/2;

        // Calculate adjusted position for rotation
        float rotation = tank.getTurretRotation();
        float cos = (float)Math.cos(rotation);
        float sin = (float)Math.sin(rotation);

        float adjustedX = position.x + turretCenterX * cos;
        float adjustedY = position.y + turretCenterX * sin;

        // Get colors based on tank type
        Vector4f fillColor = tank.isDummy() ? DUMMY_FILL : TANK_TURRET_FILL;
        Vector4f strokeColor = tank.isDummy() ? DUMMY_STROKE : TANK_TURRET_STROKE;

        // Render filled turret - length includes the section that forms the chord
        renderer.renderQuad(
                adjustedX,
                adjustedY,
                length,  // Length starting from the chord
                width,
                rotation,
                fillColor
        );

        renderer.renderQuadOutline(
                adjustedX,
                adjustedY,
                length,
                width,
                rotation,
                strokeColor,
                2.0f
        );
    }


    private float calculateTurretOffset(float radius, float width) {
        float halfWidth = width / 2.0f;

        // For a chord of width w in a circle of radius r
        // The distance from circle center to chord (offset) is:
        // offset = √(r² - (w/2)²)
        // Then distance from circle edge is: r - offset

        if (halfWidth > radius) {
            logger.warn("Turret width {} exceeds circle diameter {}", width, radius * 2);
            halfWidth = radius;
        }

        float offset = radius - (float)Math.sqrt(radius * radius - halfWidth * halfWidth);

        logger.debug("Chord geometry: radius={}, width={}, offset={}",
                radius, width, offset);

        return offset;
    }
}