package engine.core.graphics.components;

import engine.ecs.Component;
import engine.core.graphics.Renderer;

/**
 * Component for entities that need to be rendered
 */
public abstract class RenderableComponent extends Component {
    protected int renderLayer = 0;

    /**
     * Render this component's visuals
     */
    public abstract void render(Renderer renderer);

    public int getRenderLayer() {
        return renderLayer;
    }

    public void setRenderLayer(int layer) {
        this.renderLayer = layer;
    }
}