package game.entities.tanks;

import org.joml.Vector2f;

public class DummyTank extends BaseTank {
    private static int dummyCount = 0;
    private final int dummyId;

    public DummyTank(Vector2f position, TankStats stats) {
        super(position, createDummyStats(stats));
        this.dummyId = ++dummyCount;
    }

    private static TankStats createDummyStats(TankStats baseStats) {
        TankStats stats = TankStats.clone(baseStats);
        // Disable movement for dummy tanks
        stats.moveSpeed = 0;
        stats.friction = 0;
        return stats;
    }

    @Override
    public boolean isDummy() {
        return true;
    }

    public int getDummyId() {
        return dummyId;
    }

    public static void resetDummyCount() {
        dummyCount = 0;
    }
}