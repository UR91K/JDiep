package engine.core.graphics.systems;

import engine.core.graphics.Renderer;
import engine.core.graphics.Camera;
import engine.core.graphics.components.RenderableComponent;
import engine.core.logging.Logger;
import engine.ecs.Component;
import engine.ecs.Entity;
import engine.physics.components.RigidBodyComponent;
import engine.physics.components.Transform2DComponent;
import game.entities.tanks.BaseTank;

import java.util.Set;

/**
 * Core rendering system that handles rendering of all entities with
 * RenderableComponent
 */
public class RenderSystem extends engine.ecs.System {
    private static final Logger logger = Logger.getLogger(RenderSystem.class);
    private final Renderer renderer;
    private final Camera camera;
    private long frameCount = 0;

    public RenderSystem(Renderer renderer, Camera camera) {
        if (renderer == null || camera == null) {
            throw new IllegalArgumentException("Renderer and camera must not be null");
        }
        this.renderer = renderer;
        this.camera = camera;
        logger.info("Render system initialized with renderer={} and camera={}",
                renderer.getClass().getSimpleName(),
                camera.getClass().getSimpleName());
    }

    @Override
    public void update(float deltaTime) {
        var renderables = world.getEntitiesWithComponent(RenderableComponent.class);
        logger.debug("Rendering {} entities", renderables.size());

        for (Entity entity : renderables) {
            if (entity instanceof BaseTank) {
                BaseTank tank = (BaseTank)entity;
                logger.debug("PRE-RENDER Tank - transform pos: {}, rigidbody pos: {}",
                        tank.getComponent(Transform2DComponent.class).getPosition(),
                        tank.getComponent(RigidBodyComponent.class).getPosition());
            }

            RenderableComponent renderable = entity.getComponent(RenderableComponent.class);
            renderable.render(renderer);

            if (entity instanceof BaseTank) {
                BaseTank tank = (BaseTank)entity;
                logger.debug("POST-RENDER Tank - transform pos: {}, rigidbody pos: {}",
                        tank.getComponent(Transform2DComponent.class).getPosition(),
                        tank.getComponent(RigidBodyComponent.class).getPosition());
            }
        }
    }

    public Renderer getRenderer() {
        logger.trace("Renderer requested");
        return renderer;
    }

    public Camera getCamera() {
        logger.trace("Camera requested");
        return camera;
    }
}