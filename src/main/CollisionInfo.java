package main;

import org.joml.Vector2f;

public class CollisionInfo {
    public final Vector2f normal;
    public final float depth;
    public final Vector2f point;

    public CollisionInfo(Vector2f normal, float depth, Vector2f point) {
        this.normal = normal;
        this.depth = depth;
        this.point = point;
    }
}