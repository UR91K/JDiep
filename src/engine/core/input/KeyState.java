package engine.core.input;

public class KeyState {
    private boolean isPressed;
    private boolean isJustPressed;
    private boolean isJustReleased;

    public void update(boolean pressed) {
        isJustPressed = pressed && !isPressed;
        isJustReleased = !pressed && isPressed;
        isPressed = pressed;
    }

    public void reset() {
        isJustPressed = false;
        isJustReleased = false;
    }

    public boolean isPressed() { return isPressed; }
    public boolean isJustPressed() { return isJustPressed; }
    public boolean isJustReleased() { return isJustReleased; }
}
