package engine.event;


public abstract class WindowEvent extends Event {

    public static class Resize extends WindowEvent {
        private final int width;
        private final int height;

        public Resize(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public int getWidth() { return width; }
        public int getHeight() { return height; }
    }

    public static class Close extends WindowEvent {}

    public static class Focus extends WindowEvent {
        private final boolean focused;

        public Focus(boolean focused) {
            this.focused = focused;
        }

        public boolean isFocused() { return focused; }
    }
}
