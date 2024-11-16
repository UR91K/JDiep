package game.entities.tanks;

import engine.core.graphics.Camera;
import engine.core.input.InputManager;
import game.components.LocalPlayerControlComponent;
import org.joml.Vector2f;

public class LocalPlayerTank extends PlayerTank {
    public LocalPlayerTank(Vector2f position, TankStats stats, String playerName,
                           InputManager inputManager, Camera camera) {
        super(position, stats, playerName);

        // Add control component
        addComponent(new LocalPlayerControlComponent(inputManager, camera));
    }
}
