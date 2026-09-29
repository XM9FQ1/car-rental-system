package hu.nye.carrental.fx;

import javafx.application.Application;

/**
 * Entry point of the JavaFX desktop application.
 * (A separate main class is needed so JavaFX starts correctly from a normal jar.)
 */
public final class FxLauncher {

    private FxLauncher() {
    }

    public static void main(String[] args) {
        System.setProperty("spring.devtools.restart.enabled", "false");
        System.setProperty("java.awt.headless", "false");
        Application.launch(FxApp.class, args);
    }
}
