package engine.core.graphics.components;

import engine.ecs.Component;
import org.joml.Vector4f;

public class GridComponent extends Component {
    private final float spacing;
    private final float worldSize;
    private final Vector4f color;
    private int vertexCount;
    private int vao;
    private int vbo;

    public GridComponent(float spacing, float worldSize, Vector4f color) {
        this.spacing = spacing;
        this.worldSize = worldSize;
        this.color = color;
    }

    public float getSpacing() { return spacing; }
    public float getWorldSize() { return worldSize; }
    public Vector4f getColor() { return new Vector4f(color); }
    public int getVertexCount() { return vertexCount; }
    public int getVAO() { return vao; }
    public int getVBO() { return vbo; }

    public void setVAO(int vao) { this.vao = vao; }
    public void setVertexCount(int count) { this.vertexCount = count; }
    public void setVBO(int vbo) { this.vbo = vbo; }
}