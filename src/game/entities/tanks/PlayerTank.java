package game.entities.tanks;

import org.joml.Vector2f;

public class PlayerTank extends BaseTank {
    protected final String playerName;

    public PlayerTank(Vector2f position, TankStats stats, String playerName) {
        super(position, stats);
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }
}
