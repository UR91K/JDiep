package engine.event;

public class KeyEvent extends Event {
    private final int key;
    private final int action;
    private final int mods;

    public KeyEvent(int key, int action, int mods) {
        this.key = key;
        this.action = action;
        this.mods = mods;
    }

    public int getKey() { return key; }
    public int getAction() { return action; }
    public int getMods() { return mods; }
}

