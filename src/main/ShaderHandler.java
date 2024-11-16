//ShaderHandler.java
//LEGACY IMPLEMENTATION
//USE THIS FOR REFERENCE
package main;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;

public class ShaderHandler {
    private int shaderProgram;
    private GLFWErrorCallback errorCallback;

    public ShaderHandler() {
        this.shaderProgram = createShaderProgram();
        if (this.shaderProgram == 0) {
            throw new RuntimeException("Failed to create shader program");
        }
        printActiveUniforms();
    }

    public void cleanup() {
        glDeleteProgram(shaderProgram);
        if (errorCallback != null) {
            errorCallback.free();
        }
    }

    private String loadShaderSource(String fileName) {
        StringBuilder shaderSource = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new FileReader("shaders/" + fileName));
            String line;
            while ((line = reader.readLine()) != null) {
                shaderSource.append(line).append("\n");
            }
            reader.close();
        } catch (IOException e) {
            System.err.println("Could not read file: " + fileName);
            e.printStackTrace();
            System.exit(-1);
        }
        return shaderSource.toString();
    }

    private int createShaderProgram() {
        int vertexShader = compileShader(GL_VERTEX_SHADER, "vertex.glsl");
        int fragmentShader = compileShader(GL_FRAGMENT_SHADER, "fragment.glsl");

        int program = glCreateProgram();
        if (program == 0) {
            return 0;
        }

        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);

        if (!checkShaderLinkErrors(program)) {
            glDeleteProgram(program);
            return 0;
        }

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        return program;
    }

    int compileShader(int type, String fileName) {
        String source = loadShaderSource(fileName);

        System.out.println("Shader source for " + fileName + ":");
        System.out.println(source);
        System.out.println("END OF SHADER SOURCE \n");

        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);

        int success = glGetShaderi(shader, GL_COMPILE_STATUS);
        if (success == GL_FALSE) {
            int len = glGetShaderi(shader, GL_INFO_LOG_LENGTH);
            String infoLog = glGetShaderInfoLog(shader, len);
            System.err.println("ERROR: " + fileName + "\n\tShader compilation failed.");
            System.err.println(infoLog);
            glDeleteShader(shader);
            return 0;
        }

        return shader;
    }

    boolean checkShaderLinkErrors(int program) {
        IntBuffer success = BufferUtils.createIntBuffer(1);
        glGetProgramiv(program, GL_LINK_STATUS, success);
        if (success.get(0) == GL_FALSE) {
            int len = glGetProgrami(program, GL_INFO_LOG_LENGTH);
            String infoLog = glGetProgramInfoLog(program, len);
            System.err.println("Shader program linking failed:");
            System.err.println(infoLog);
            return false;
        }
        return true;
    }

    public void printActiveUniforms() {
        IntBuffer numUniforms = BufferUtils.createIntBuffer(1);
        glGetProgramiv(shaderProgram, GL_ACTIVE_UNIFORMS, numUniforms);
        int uniformCount = numUniforms.get(0);

        IntBuffer size = BufferUtils.createIntBuffer(1);
        IntBuffer type = BufferUtils.createIntBuffer(1);

        for (int i = 0; i < uniformCount; i++) {
            String name = glGetActiveUniform(shaderProgram, i, size, type);
            int location = glGetUniformLocation(shaderProgram, name);
            System.out.println("Uniform " + i + " Type: " + type.get(0) +
                    " Name: " + name + " Location: " + location);
        }
    }

    public void useShaderProgram() {
        glUseProgram(shaderProgram);
    }

    public int getShaderProgram() {
        return shaderProgram;
    }

    public void setUniform(String name, Matrix4f value) {
        int location = glGetUniformLocation(shaderProgram, name);
        if (location != -1) {
            float[] data = new float[16];
            value.get(data);
            glUniformMatrix4fv(location, false, data);
        } else {
            System.err.println("Warning: uniform '" + name + "' not found in shader program.");
        }
    }

    public void setUniform(String name, Vector4f value) {
        int location = glGetUniformLocation(shaderProgram, name);
        if (location != -1) {
            glUniform4f(location, value.x, value.y, value.z, value.w);
        } else {
            System.err.println("Warning: uniform '" + name + "' not found in shader program.");
        }
    }

    public void setUniform(String name, boolean value) {
        int location = glGetUniformLocation(shaderProgram, name);
        if (location != -1) {
            glUniform1i(location, value ? 1 : 0);
        } else {
            System.err.println("Warning: uniform '" + name + "' not found in shader program.");
        }
    }
}