//TextRenderer.java
//LEGACY IMPLEMENTATION
//USE THIS FOR REFERENCE
package main;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBTruetype.*;

public class TextRenderer {
    private static final int BITMAP_WIDTH = 1024;
    private static final int BITMAP_HEIGHT = 1024;
    private static final float FONT_HEIGHT = 16.0f;
    private static final int FIRST_CHAR = 32;
    private static final int NUM_CHARS = 96;

    private int textureId;
    private STBTTBakedChar.Buffer charData;
    private int vao;
    private int vbo;
    private TextShaderHandler textShader;

    public TextRenderer(String fontPath) {
        try {
            System.out.println("Loading font: " + fontPath);

            // Initialize text shader
            textShader = new TextShaderHandler();

            // Load font file
            byte[] ttfBytes = Files.readAllBytes(Paths.get(fontPath));
            ByteBuffer ttfBuffer = BufferUtils.createByteBuffer(ttfBytes.length);
            ttfBuffer.put(ttfBytes).flip();

            // Create bitmap for font
            ByteBuffer bitmap = BufferUtils.createByteBuffer(BITMAP_WIDTH * BITMAP_HEIGHT);
            charData = STBTTBakedChar.malloc(NUM_CHARS);

            // Bake font bitmap
            int result = stbtt_BakeFontBitmap(ttfBuffer, FONT_HEIGHT, bitmap,
                    BITMAP_WIDTH, BITMAP_HEIGHT, FIRST_CHAR, charData);

            if (result <= 0) {
                throw new RuntimeException("Failed to bake font bitmap. Result: " + result);
            }

            System.out.println("Font baking successful. Result: " + result);

            // Create and setup texture
            textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RED,
                    BITMAP_WIDTH, BITMAP_HEIGHT, 0,
                    GL_RED, GL_UNSIGNED_BYTE, bitmap);

            // Set texture parameters
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            // Create VAO and VBO for rendering
            vao = glGenVertexArrays();
            vbo = glGenBuffers();

            glBindVertexArray(vao);
            glBindBuffer(GL_ARRAY_BUFFER, vbo);

            // Single vec4 vertex attribute containing pos and tex coords
            glBufferData(GL_ARRAY_BUFFER, 16 * Float.BYTES, GL_DYNAMIC_DRAW);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 4, GL_FLOAT, false, 0, 0);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load font: " + e.getMessage());
        }
    }

    public float getTextWidth(String text) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer x = stack.floats(0.0f);
            FloatBuffer y = stack.floats(0.0f);
            STBTTAlignedQuad q = STBTTAlignedQuad.malloc(stack);

            float width = 0;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c < FIRST_CHAR || c >= FIRST_CHAR + NUM_CHARS) continue;

                stbtt_GetBakedQuad(charData, BITMAP_WIDTH, BITMAP_HEIGHT,
                        c - FIRST_CHAR, x, y, q, true);
                width = q.x1();
            }
            return width;
        }
    }

    public float getLineHeight() {
        return FONT_HEIGHT * 1.5f;  // Add some vertical spacing between lines
    }

    public void renderText(Matrix4f projection, String text, float startX, float startY, Vector4f color) {
        textShader.useShaderProgram();
        textShader.setUniform("projection", projection);
        textShader.setUniform("color", color);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId);
        glUniform1i(glGetUniformLocation(textShader.getShaderProgram(), "text"), 0);

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer vertices = stack.mallocFloat(16);
            FloatBuffer x = stack.floats(0.0f);
            FloatBuffer y = stack.floats(0.0f);
            STBTTAlignedQuad q = STBTTAlignedQuad.malloc(stack);

            x.put(0, startX);
            y.put(0, 0);  // Reset y position for each line

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c < FIRST_CHAR || c >= FIRST_CHAR + NUM_CHARS) continue;

                stbtt_GetBakedQuad(charData, BITMAP_WIDTH, BITMAP_HEIGHT,
                        c - FIRST_CHAR, x, y, q, true);

                vertices.clear();
                vertices.put(q.x0()).put(q.y0() + startY).put(q.s0()).put(q.t0());
                vertices.put(q.x1()).put(q.y0() + startY).put(q.s1()).put(q.t0());
                vertices.put(q.x1()).put(q.y1() + startY).put(q.s1()).put(q.t1());
                vertices.put(q.x0()).put(q.y1() + startY).put(q.s0()).put(q.t1());
                vertices.flip();

                glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
                glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
            }
        }
    }

    public void cleanup() {
        if (charData != null) {
            charData.free();
        }
        if (textShader != null) {
            textShader.cleanup();
        }
        glDeleteTextures(textureId);
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }
}