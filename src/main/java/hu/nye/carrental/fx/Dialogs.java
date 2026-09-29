package hu.nye.carrental.fx;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/** Small helper for the pop-up messages used by all screens. */
public final class Dialogs {

    private Dialogs() {
    }

    public static void error(String message) {
        show(Alert.AlertType.ERROR, "Error", message);
    }

    public static void info(String message) {
        show(Alert.AlertType.INFORMATION, "Car Rental System", message);
    }

    public static boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle("Please confirm");
        alert.setHeaderText(null);
        return alert.showAndWait().filter(button -> button == ButtonType.OK).isPresent();
    }

    private static void show(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
