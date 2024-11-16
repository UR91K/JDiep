package engine.core.graphics;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import engine.core.logging.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL20.*;

public class Shader {
    private static final Logger logger = Logger.getLogger(Shader.class);

    private int programId;
    private int vertexShaderId;
    private int fragmentShaderId;
    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
    private String vertexPath;
    private String fragmentPath;

    public Shader() {
        programId = glCreateProgram();
        logger.debug("Created new shader program with ID: {}", programId);
    }

    public void attachVertexShader(String path) {
        logger.info("Attaching vertex shader from: {}", path);
        this.vertexPath = path;
        try {
            vertexShaderId = loadShader(path, GL_VERTEX_SHADER);
            logger.debug("Vertex shader ID {} attached to program {}", vertexShaderId, programId);
        } catch (RuntimeException e) {
            logger.error("Failed to attach vertex shader from: {}", path, e);
            throw e;
        }
    }

    public void attachFragmentShader(String path) {
        logger.info("Attaching fragment shader from: {}", path);
        this.fragmentPath = path;
        try {
            fragmentShaderId = loadShader(path, GL_FRAGMENT_SHADER);
            logger.debug("Fragment shader ID {} attached to program {}", fragmentShaderId, programId);
        } catch (RuntimeException e) {
            logger.error("Failed to attach fragment shader from: {}", path, e);
            throw e;
        }
    }

    public void link() {
        logger.info("Linking shader program {} (vertex: {}, fragment: {})",
                programId, vertexPath, fragmentPath);

        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            String error = glGetProgramInfoLog(programId);
            logger.error("Failed to link shader program {}: {}", programId, error);
            throw new RuntimeException("Failed to link shader program: " + error);
        }

        // Cleanup shaders after successful linking
        if (vertexShaderId != 0) {
            logger.trace("Detaching and deleting vertex shader {}", vertexShaderId);
            glDetachShader(programId, vertexShaderId);
            glDeleteShader(vertexShaderId);
        }
        if (fragmentShaderId != 0) {
            logger.trace("Detaching and deleting fragment shader {}", fragmentShaderId);
            glDetachShader(programId, fragmentShaderId);
            glDeleteShader(fragmentShaderId);
        }

        logger.info("Successfully linked shader program {}", programId);
    }

    private int loadShader(String path, int type) {
        logger.debug("Loading {} shader from: {}",
                type == GL_VERTEX_SHADER ? "vertex" : "fragment", path);

        StringBuilder source = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            String line;
            while ((line = reader.readLine()) != null) {
                source.append(line).append("\n");
            }
            reader.close();
        } catch (IOException e) {
            logger.error("Failed to read shader file: {}", path, e);
            throw new RuntimeException("Failed to load shader: " + path, e);
        }

        int shaderId = glCreateShader(type);
        glShaderSource(shaderId, source);

        logger.trace("Compiling shader {}", shaderId);
        glCompileShader(shaderId);

        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == GL_FALSE) {
            String error = glGetShaderInfoLog(shaderId);
            logger.error("Failed to compile shader {}: {}", shaderId, error);
            throw new RuntimeException("Failed to compile shader: " + error);
        }

        glAttachShader(programId, shaderId);
        logger.debug("Successfully compiled and attached shader {} to program {}", shaderId, programId);
        return shaderId;
    }

    public void bind() {
        logger.trace("Binding shader program {}", programId);
        glUseProgram(programId);
    }

    public void unbind() {
        logger.trace("Unbinding shader program {}", programId);
        glUseProgram(0);
    }

    public void cleanup() {
        logger.debug("Cleaning up shader program {}", programId);
        unbind();
        if (programId != 0) {
            glDeleteProgram(programId);
            logger.debug("Deleted shader program {}", programId);
        }
    }

    // Uniform setters with logging
    public void setUniform(String name, float value) {
        int location = glGetUniformLocation(programId, name);
        if (location != -1) {
            logger.trace("Setting float uniform '{}' to {} in program {}", name, value, programId);
            glUniform1f(location, value);
        } else {
            logger.warn("Uniform '{}' not found in shader program {}", name, programId);
        }
    }

    public void setUniform(String name, Vector2f value) {
        int location = glGetUniformLocation(programId, name);
        if (location != -1) {
            logger.trace("Setting Vector2f uniform '{}' to {} in program {}", name, value, programId);
            glUniform2f(location, value.x, value.y);
        } else {
            logger.warn("Uniform '{}' not found in shader program {}", name, programId);
        }
    }

    public void setUniform(String name, Vector3f value) {
        int location = glGetUniformLocation(programId, name);
        if (location != -1) {
            logger.trace("Setting Vector3f uniform '{}' to {} in program {}", name, value, programId);
            glUniform3f(location, value.x, value.y, value.z);
        } else {
            logger.warn("Uniform '{}' not found in shader program {}", name, programId);
        }
    }

    public void setUniform(String name, Vector4f value) {
        int location = glGetUniformLocation(programId, name);
        if (location != -1) {
            logger.trace("Setting Vector4f uniform '{}' to {} in program {}", name, value, programId);
            glUniform4f(location, value.x, value.y, value.z, value.w);
        } else {
            logger.warn("Uniform '{}' not found in shader program {}", name, programId);
        }
    }

    public void setUniform(String name, Matrix4f value) {
        int location = glGetUniformLocation(programId, name);
        if (location != -1) {
            logger.trace("Setting Matrix4f uniform '{}' in program {}", name, programId);
            value.get(matrixBuffer);
            glUniformMatrix4fv(location, false, matrixBuffer);
        } else {
            logger.warn("Uniform '{}' not found in shader program {}", name, programId);
        }
    }

    public void setUniform(String name, boolean value) {
        int location = glGetUniformLocation(programId, name);
        if (location != -1) {
            logger.trace("Setting boolean uniform '{}' to {} in program {}", name, value, programId);
            glUniform1i(location, value ? 1 : 0);
        } else {
            logger.warn("Uniform '{}' not found in shader program {}", name, programId);
        }
    }
}