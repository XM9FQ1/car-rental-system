package hu.nye.carrental.desktop;

import javafx.application.Application;

/**
 * Masaüstü uygulamasının giriş noktası.
 * (JavaFX'in düzgün başlaması için Application'dan türemeyen ayrı bir main sınıfı gerekir.)
 */
public final class DesktopLauncher {

    private DesktopLauncher() {
    }

    public static void main(String[] args) {
        System.setProperty("spring.devtools.restart.enabled", "false");
        System.setProperty("java.awt.headless", "false");
        Application.launch(DesktopApp.class, args);
    }
}
