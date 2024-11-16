package engine.core.graphics.systems;

import engine.core.graphics.Renderer;
import engine.core.graphics.Camera;
import engine.core.graphics.components.GridComponent;
import engine.ecs.System;
import engine.ecs.Entity;
import engine.core.logging.Logger;
import org.joml.Vector4f;

import static org.lwjgl.opengl.GL30.*;

public class GridRenderSystem extends System {
    private static final Logger logger = Logger.getLogger(GridRenderSystem.class);
    private final Renderer renderer;
    private final Camera camera;

    public GridRenderSystem(Renderer renderer, Camera camera) {
        this.renderer = renderer;
        this.camera = camera;
        logger.setClassLevel(GridRenderSystem.class, Logger.Level.DEBUG);
        logger.debug("Created GridRenderSystem with renderer and camera");
    }

    @Override
    public void initialize(engine.ecs.World world) {
        super.initialize(world);
        logger.info("Initializing GridRenderSystem");

        // Create grid entity when system initializes
        createGridEntity();
    }

    private void createGridEntity() {
        Entity gridEntity = world.createEntity();
        // Make grid more visible - lighter gray color
        GridComponent grid = new GridComponent(30.0f, 500.0f, new Vector4f(0.3f, 0.3f, 0.3f, 1.0f));
        gridEntity.addComponent(grid);

        logger.info("Creating grid with spacing={}, worldSize={}", grid.getSpacing(), grid.getWorldSize());

        // Generate grid vertices
        float[] vertices = generateGridVertices(grid.getSpacing(), grid.getWorldSize());
        logger.debug("Generated {} grid vertices", vertices.length);

        // Create OpenGL buffers
        int vao = glGenVertexArrays();
        int vbo = glGenBuffers();

        logger.debug("Created VAO={}, VBO={}", vao, vbo);

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        // Verify buffer creation
        int error = glGetError();
        if (error != GL_NO_ERROR) {
            logger.error("OpenGL error after buffer creation: 0x{}", Integer.toHexString(error));
        }

        grid.setVAO(vao);
        grid.setVBO(vbo);
        grid.setVertexCount(vertices.length / 2);

        logger.info("Grid initialized with VAO: {}, vertices: {}", vao, grid.getVertexCount());
    }

    private float[] generateGridVertices(float spacing, float worldSize) {
        int numLines = (int)((worldSize * 2) / spacing) + 1;
        float[] vertices = new float[numLines * 4 * 2 * 2];  // 2 points per line, 2 coordinates per point
        int idx = 0;

        logger.debug("Generating grid with {} lines", numLines);

        // Vertical lines
        for (float x = -worldSize; x <= worldSize; x += spacing) {
            vertices[idx++] = x;                // x1
            vertices[idx++] = -worldSize;       // y1
            vertices[idx++] = x;                // x2
            vertices[idx++] = worldSize;        // y2
        }

        // Horizontal lines
        for (float y = -worldSize; y <= worldSize; y += spacing) {
            vertices[idx++] = -worldSize;       // x1
            vertices[idx++] = y;                // y1
            vertices[idx++] = worldSize;        // x2
            vertices[idx++] = y;                // y2
        }

        return vertices;
    }

    @Override
    public void update(float deltaTime) {
        var gridEntities = world.getEntitiesWithComponent(GridComponent.class);
        logger.debug("Rendering {} grid entities", gridEntities.size());

        for (Entity entity : gridEntities) {
            GridComponent grid = entity.getComponent(GridComponent.class);

            // Bind VAO and draw grid
            glBindVertexArray(grid.getVAO());

            int error = glGetError();
            if (error != GL_NO_ERROR) {
                logger.error("OpenGL error after VAO bind: 0x{}", Integer.toHexString(error));
            }

            logger.debug("Drawing grid with VAO={}, vertexCount={}, color={}",
                    grid.getVAO(), grid.getVertexCount(), grid.getColor());

            renderer.renderGrid(
                    camera.getViewProjectionMatrix(),
                    grid.getVertexCount(),
                    grid.getColor()
            );

            error = glGetError();
            if (error != GL_NO_ERROR) {
                logger.error("OpenGL error after grid render: 0x{}", Integer.toHexString(error));
            }
        }
    }

    public void cleanup() {
        logger.info("Cleaning up GridRenderSystem");
        var gridEntities = world.getEntitiesWithComponent(GridComponent.class);
        for (Entity entity : gridEntities) {
            GridComponent grid = entity.getComponent(GridComponent.class);
            glDeleteVertexArrays(grid.getVAO());
            glDeleteBuffers(grid.getVBO());
            logger.debug("Deleted grid VAO={} and VBO={}", grid.getVAO(), grid.getVBO());
        }
    }

    @Override
    public boolean isRenderSystem() {
        return true;
    }
}