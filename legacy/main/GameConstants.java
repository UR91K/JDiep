package main;

import org.joml.Vector4f;

public final class GameConstants {
    // Prevent instantiation
    private GameConstants() {}

    // World constants
    public static final float WORLD_SIZE = 500.0f;
    public static final float GRID_SPACING = 2.0f;

    // World boundaries
    public static final float BOUNDARY_LEFT = -WORLD_SIZE;
    public static final float BOUNDARY_RIGHT = WORLD_SIZE;
    public static final float BOUNDARY_TOP = WORLD_SIZE;
    public static final float BOUNDARY_BOTTOM = -WORLD_SIZE;

    // Player constants
    public static final float PLAYER_MOVE_SPEED = 27.0f;
    public static final float PLAYER_FRICTION = 1.0f;
    public static final float PLAYER_BOUNCE_FACTOR = 0.5f;
    public static final float PLAYER_WALL_SLIDE_FACTOR = 0.5f; // Controls friction when sliding along walls
    // Tank size relationships
    public static final float TURRET_WIDTH_RATIO = 0.4f;     // Turret width as fraction of radius
    public static final float TURRET_LENGTH_RATIO = 1.5f;    // Turret length as fraction of radius

    // Calculate actual turret dimensions based on tank size
    public static float getTurretWidth(float tankRadius) {
        return tankRadius * TURRET_WIDTH_RATIO;
    }

    public static float getTurretLength(float tankRadius) {
        return tankRadius * TURRET_LENGTH_RATIO;
    }


    // Camera constants
    public static final float CAMERA_DEADZONE = 2.0f;
    public static final float CAMERA_DEFAULT_ZOOM = 1.0f;
    public static final float CAMERA_MIN_ZOOM = 0.1f;
    public static final float CAMERA_MAX_ZOOM = 5.0f;
    public static final float CAMERA_SPRING_STIFFNESS = 4.0f;
    public static final float CAMERA_SPRING_DAMPING = 3.0f;
    public static final float CAMERA_MIN_SPEED_THRESHOLD = 0.001f;

    // View constants
    public static final float DEFAULT_VIEW_SIZE = 30.0f;

    // Visual constants
    public static final Vector4f GRID_COLOR = rgb(38, 38, 38);
    public static final Vector4f CLEAR_COLOR = rgb(24, 24, 24);

    public static final float CLEAR_COLOR_R = CLEAR_COLOR.x;
    public static final float CLEAR_COLOR_G = CLEAR_COLOR.y;
    public static final float CLEAR_COLOR_B = CLEAR_COLOR.z;
    public static final float CLEAR_COLOR_A = CLEAR_COLOR.w;

    // Stroke width
    public static final float STROKE_WIDTH = 3.5f;

    // Player colors
    public static final Vector4f PLAYER_FILL_COLOR = rgb(165, 48, 48);
    public static final Vector4f PLAYER_STROKE_COLOR = rgb(117, 36, 56);

    // Turret colors
    public static final Vector4f TURRET_FILL_COLOR = rgb(87, 114, 119);
    public static final Vector4f TURRET_STROKE_COLOR = rgb(57, 74, 80);

    // Base tank properties
    public static final float PLAYER_DEFAULT_RADIUS = 3.0f;

    // Default player turret dimensions
    public static final float PLAYER_TURRET_WIDTH = 2.0f;
    public static final float PLAYER_TURRET_LENGTH = 3.0f;
    public static final float TURRET_PUSH_FORCE = 4.0f;


    // Debug Menu colors
    public static final Vector4f DEBUG_MENU_TEXT_COLOR = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
    public static final Vector4f DEBUG_MENU_TITLE_COLOR = new Vector4f(1.0f, 1.0f, 0.5f, 1.0f);
    public static final Vector4f DEBUG_MENU_SECTION_COLOR = new Vector4f(0.5f, 1.0f, 0.5f, 1.0f);
    public static final Vector4f DEBUG_MENU_SLIDER_TRACK_COLOR = new Vector4f(0.3f, 0.3f, 0.3f, 1.0f);
    public static final Vector4f DEBUG_MENU_SLIDER_HANDLE_COLOR = new Vector4f(0.8f, 0.8f, 0.8f, 1.0f);


    // Physics constants for turret collisions - revised for stability
    public static final float ROTATION_SPEED_SCALE = 0.1f;
    public static final float BASE_FORCE_MULTIPLIER = 17.0f;
    public static final float LEVERAGE_MULTIPLIER = 1.2f;
    public static final float MASS_SCALE = 1.0f;
    public static final float DAMPING_FACTOR = 0.8f;
    public static final float MOMENT_OF_INERTIA_SCALE = 0.2f;
    public static final float NORMAL_FORCE_RATIO = 0.9f;
    public static final float TANGENTIAL_FORCE_RATIO = 0.6f;

    // Helper method for moment of inertia calculations
    public static float calculateMomentOfInertia(float width, float length, float radius) {
        float turretMoment = (width * length * length) / 6.0f;
        float bodyMoment = radius * radius;
        return (turretMoment + bodyMoment) * MOMENT_OF_INERTIA_SCALE;
    }


    public static Vector4f rgb(int r, int g, int b) {
        float normalizedR = r / 255.0f;
        float normalizedG = g / 255.0f;
        float normalizedB = b / 255.0f;
        float alpha = 1.0f; // default to fully opaque

        return new Vector4f(normalizedR, normalizedG, normalizedB, alpha);
    }
}