package engine.core.window;

public class WindowConfig {
    private int width = 1280;
    private int height = 720;
    private String title = "JDiep";
    private boolean vsync = true;
    private boolean resizable = true;
    private int multisamples = 0;

    // Builder pattern for configuration
    public static class Builder {
        private final WindowConfig config = new WindowConfig();

        public Builder width(int width) {
            config.width = width;
            return this;
        }

        public Builder height(int height) {
            config.height = height;
            return this;
        }

        public Builder title(String title) {
            config.title = title;
            return this;
        }

        public Builder vsync(boolean vsync) {
            config.vsync = vsync;
            return this;
        }

        public Builder resizable(boolean resizable) {
            config.resizable = resizable;
            return this;
        }

        public Builder multisamples(int samples) {
            config.multisamples = samples;
            return this;
        }

        public WindowConfig build() {
            return config;
        }
    }

    // Getters
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public String getTitle() { return title; }
    public boolean isVsync() { return vsync; }
    public boolean isResizable() { return resizable; }
    public int getMultisamples() { return multisamples; }
}
