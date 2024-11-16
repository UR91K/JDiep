package engine.core.graphics;

import engine.ecs.Entity;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import engine.core.logging.Logger;

public class Renderer {
    private static final Vector4f CLEAR_COLOR = new Vector4f(0.1f, 0.1f, 0.1f
            , 1.0f);

    private static final Logger logger = Logger.getLogger(Renderer.class);

    private final Shader defaultShader;
    private final Matrix4f projectionMatrix;
    private final Camera camera;
    private boolean isInitialized = false;

    private int circleVAO;
    private int quadVAO;
    private int frameCount = 0;

    public Renderer(Camera camera) {
        logger.info("Initializing renderer");

        long context = glfwGetCurrentContext();
        if (context == 0) {
            logger.error("Attempting to initialize renderer without OpenGL context!");
            throw new IllegalStateException("No OpenGL context present during renderer initialization");
        }

        this.camera = camera;
        this.projectionMatrix = new Matrix4f();

        try {
            logger.debug("Creating default shader");
            this.defaultShader = new Shader();

            // Load and compile default shaders
            defaultShader.attachVertexShader("shaders/default_vertex.glsl");
            defaultShader.attachFragmentShader("shaders/default_fragment.glsl");
            defaultShader.link();

            createCircleMesh();
            createQuadMesh();

            // Check initial OpenGL state
            int error = glGetError();
            if (error != GL_NO_ERROR) {
                logger.error("OpenGL error after initialization: 0x{}", Integer.toHexString(error));
            } else {
                logger.debug("No OpenGL errors after initialization");
                isInitialized = true;
            }

        } catch (Exception e) {
            logger.error("Failed to initialize renderer", e);
            throw new RuntimeException("Renderer initialization failed", e);
        }

        logger.info("Renderer initialization complete");
    }

    public void setViewport(int width, int height) {
        logger.debug("Setting viewport: width={}, height={}", width, height);
        glViewport(0, 0, width, height);
        float aspectRatio = (float) width / height;
        projectionMatrix.identity().ortho(
                -aspectRatio * 30.0f,
                aspectRatio * 30.0f,
                -30.0f,
                30.0f,
                -1.0f,
                1.0f
        );
    }

    private void createCircleMesh() {
        logger.debug("Creating circle mesh with 32 segments");
        float[] vertices = generateCircleVertices(32);
        circleVAO = createVAO(vertices);
        logger.trace("Circle VAO created: {}", circleVAO);
    }

    private void createQuadMesh() {
        logger.debug("Creating quad mesh");
        float[] vertices = {
                -0.5f, -0.5f,
                0.5f, -0.5f,
                0.5f, 0.5f,
                -0.5f, 0.5f
        };
        quadVAO = createVAO(vertices);
        logger.trace("Quad VAO created: {}", quadVAO);
    }

    private int createVAO(float[] vertices) {
        int vao = glGenVertexArrays();
        int vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);

        logger.trace("Created VAO: {}, VBO: {}", vao, vbo);
        return vao;
    }

    private float[] generateCircleVertices(int segments) {
        logger.trace("Generating circle vertices with {} segments", segments);
        float[] vertices = new float[(segments + 2) * 2];
        int idx = 0;

        // Center vertex
        vertices[idx++] = 0;
        vertices[idx++] = 0;

        for (int i = 0; i <= segments; i++) {
            float angle = (float) (2 * Math.PI * i / segments);
            vertices[idx++] = (float) Math.cos(angle);
            vertices[idx++] = (float) Math.sin(angle);
        }

        return vertices;
    }

    public void beginFrame() {
        frameCount++;
        logger.debug("Beginning frame {}", frameCount);

        if (!isInitialized) {
            logger.error("Attempting to render with uninitialized renderer!");
            return;
        }

        long context = glfwGetCurrentContext();
        if (context == 0) {
            logger.error("No OpenGL context on current thread!");
            return;
        }

        try {
            // Set clear color
            glClearColor(CLEAR_COLOR.x, CLEAR_COLOR.y, CLEAR_COLOR.z, CLEAR_COLOR.w);
            logger.trace("Set clear color to: ({}, {}, {}, {})",
                    CLEAR_COLOR.x, CLEAR_COLOR.y, CLEAR_COLOR.z, CLEAR_COLOR.w);

            // Clear buffers
            glClear(GL_COLOR_BUFFER_BIT);

            // Enable alpha blending
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            // Check for errors
            int error = glGetError();
            if (error != GL_NO_ERROR) {
                logger.error("OpenGL error at start of frame {}: 0x{}",
                        frameCount, Integer.toHexString(error));
            }
        } catch (Exception e) {
            logger.error("Error in beginFrame {}", frameCount, e);
        }
    }

    public void endFrame() {
        if (!isInitialized) {
            logger.error("Attempting to end frame with uninitialized renderer!");
            return;
        }

        try {
            // Disable blending
            glDisable(GL_BLEND);

            // Unbind current VAO and shader
            glBindVertexArray(0);
            defaultShader.unbind();

            // Check for OpenGL errors
            int error = glGetError();
            if (error != GL_NO_ERROR) {
                logger.error("OpenGL error at end of frame {}: 0x{}",
                        frameCount, Integer.toHexString(error));
            } else {
                logger.debug("Frame {} completed successfully", frameCount);
            }
        } catch (Exception e) {
            logger.error("Error in endFrame {}", frameCount, e);
        }
    }

    public void renderCircle(float x, float y, float radius, Vector4f color) {
        if (!isInitialized) {
            logger.error("Attempting to render circle with uninitialized renderer!");
            return;
        }

        logger.trace("Rendering circle at ({}, {}), radius={}", x, y, radius);
        try {
            defaultShader.bind();
            defaultShader.setUniform("projection", projectionMatrix);
            defaultShader.setUniform("view", camera.getViewMatrix());
            defaultShader.setUniform("model", new Matrix4f()
                    .translate(x, y, 0)
                    .scale(radius));
            defaultShader.setUniform("color", color);

            glBindVertexArray(circleVAO);
            glDrawArrays(GL_TRIANGLE_FAN, 0, 34);

            int error = glGetError();
            if (error != GL_NO_ERROR) {
                logger.error("OpenGL error during circle render: 0x{}",
                        Integer.toHexString(error));
            }
        } catch (Exception e) {
            logger.error("Error rendering circle", e);
        }
    }

    public void renderQuad(float x, float y, float width, float height, float rotation, Vector4f color) {
        if (!isInitialized) {
            logger.error("Attempting to render quad with uninitialized renderer!");
            return;
        }

        logger.trace("Rendering quad at ({}, {}), size=({}, {}), rotation={}",
                x, y, width, height, rotation);
        try {
            defaultShader.bind();
            defaultShader.setUniform("projection", projectionMatrix);
            defaultShader.setUniform("view", camera.getViewMatrix());
            defaultShader.setUniform("model", new Matrix4f()
                    .translate(x, y, 0)
                    .rotate(rotation, 0, 0, 1)
                    .scale(width, height, 1));
            defaultShader.setUniform("color", color);

            glBindVertexArray(quadVAO);
            glDrawArrays(GL_TRIANGLE_FAN, 0, 4);

            int error = glGetError();
            if (error != GL_NO_ERROR) {
                logger.error("OpenGL error during quad render: 0x{}",
                        Integer.toHexString(error));
            }
        } catch (Exception e) {
            logger.error("Error rendering quad", e);
        }
    }

    public void renderQuadOutline(float x, float y, float width, float height,
                                  float rotation, Vector4f color, float strokeWidth) {
        if (!isInitialized) return;

        logger.trace("Rendering quad outline at ({}, {}), size=({}, {}), rotation={}, stroke={}",
                x, y, width, height, rotation, strokeWidth);

        try {
            defaultShader.bind();
            defaultShader.setUniform("projection", projectionMatrix);
            defaultShader.setUniform("view", camera.getViewMatrix());
            defaultShader.setUniform("model", new Matrix4f()
                    .translate(x, y, 0)
                    .rotate(rotation, 0, 0, 1)
                    .scale(width, height, 1));
            defaultShader.setUniform("color", color);

            // Save current line width
            float currentLineWidth = glGetFloat(GL_LINE_WIDTH);

            // Set new line width and draw
            glLineWidth(strokeWidth);
            glBindVertexArray(quadVAO);
            glDrawArrays(GL_LINE_LOOP, 0, 4);

            // Restore line width
            glLineWidth(currentLineWidth);

        } catch (Exception e) {
            logger.error("Error rendering quad outline", e);
        }
    }

    public void renderCircleOutline(float x, float y, float radius, Vector4f color, float strokeWidth) {
        if (!isInitialized) return;

        logger.trace("Rendering circle outline at ({}, {}), radius={}, stroke={}",
                x, y, radius, strokeWidth);

        try {
            defaultShader.bind();
            defaultShader.setUniform("projection", projectionMatrix);
            defaultShader.setUniform("view", camera.getViewMatrix());
            defaultShader.setUniform("model", new Matrix4f()
                    .translate(x, y, 0)
                    .scale(radius));
            defaultShader.setUniform("color", color);

            // Save current line width
            float currentLineWidth = glGetFloat(GL_LINE_WIDTH);

            // Set new line width and draw
            glLineWidth(strokeWidth);
            glBindVertexArray(circleVAO);
            glDrawArrays(GL_LINE_LOOP, 1, 32);  // Skip center vertex

            // Restore line width
            glLineWidth(currentLineWidth);

        } catch (Exception e) {
            logger.error("Error rendering circle outline", e);
        }
    }

    public void renderGrid(Matrix4f viewProjection, int vertexCount, Vector4f color) {
        defaultShader.bind();
        defaultShader.setUniform("projection", viewProjection);
        defaultShader.setUniform("model", new Matrix4f());  // Identity matrix for grid
        defaultShader.setUniform("color", color);

        // Draw grid lines
        glDrawArrays(GL_LINES, 0, vertexCount);

        defaultShader.unbind();
    }

    public void cleanup() {
        logger.info("Cleaning up renderer resources");
        defaultShader.cleanup();
        glDeleteVertexArrays(circleVAO);
        glDeleteVertexArrays(quadVAO);
        logger.debug("Renderer cleanup complete");
    }
}