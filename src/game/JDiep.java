package game;

import engine.core.logging.Logger;

public class JDiep {
    private static final Logger logger = Logger.getLogger(JDiep.class);

    public static void main(String[] args) {
        Logger.setGlobalMinimumLevel(Logger.Level.DEBUG);

        boolean testMode = false;
        int testFrames = 5;

        // Check for test mode argument
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--test")) {
                testMode = true;
                if (i + 1 < args.length) {
                    try {
                        testFrames = Integer.parseInt(args[i + 1]);
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid test frames argument, using default: {}", testFrames);
                    }
                }
                break;
            }
        }

        try {
            // Create and run game
            JDiepGame game = new JDiepGame(testMode, testFrames);
            game.run();
        } catch (Exception e) {
            logger.error("Fatal error occurred:");
            e.printStackTrace();
            System.exit(1);
        } finally {
            cleanup();
        }
    }

    private static void cleanup() {
        // Additional cleanup if needed
    }
}