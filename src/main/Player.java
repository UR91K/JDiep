package main;

import org.joml.Vector2f;

import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

public class Player extends Tank {
    public Player(Vector2f startPos) {
        super(startPos,
                GameConstants.PLAYER_DEFAULT_RADIUS,
                GameConstants.PLAYER_TURRET_WIDTH,
                GameConstants.PLAYER_TURRET_LENGTH);

        this.moveSpeed = GameConstants.PLAYER_MOVE_SPEED;
        this.friction = GameConstants.PLAYER_FRICTION;
        this.bounceFactor = GameConstants.PLAYER_BOUNCE_FACTOR;
    }

    public void updateRotation(Vector2f mouseWorldPos) {
        float dx = mouseWorldPos.x - position.x;
        float dy = mouseWorldPos.y - position.y;
        rotation = (float) Math.atan2(dy, dx);
    }
}