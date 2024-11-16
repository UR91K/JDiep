package engine.core.input;

import java.util.HashMap;
import java.util.Map;

class InputSnapshot {
    private final Map<Integer, KeyState> keyStates;
    private final MouseStateSnapshot mouseState;
    private final boolean cursorLocked;

    public InputSnapshot(Map<Integer, KeyState> keyStates,
                         MouseStateSnapshot mouseState,
                         boolean cursorLocked) {
        this.keyStates = new HashMap<>(keyStates);
        this.mouseState = mouseState;
        this.cursorLocked = cursorLocked;
    }

    // Add getters as needed
}
