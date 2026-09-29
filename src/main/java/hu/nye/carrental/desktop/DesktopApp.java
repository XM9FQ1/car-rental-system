package hu.nye.carrental.desktop;

import hu.nye.carrental.CarrentalApplication;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Optional;

public class DesktopApp extends Application {

    private ConfigurableApplicationContext context;

    @Override
    public void start(Stage stage) {
        Label loading = new Label("Starting Car Rental System...");
        loading.setStyle("-fx-font-size: 16px;");
        StackPane root = new StackPane(loading);

        stage.setTitle("Car Rental System");
        stage.setScene(new Scene(root, 1280, 820));
        stage.show();

        // Spring Boot'u arka planda başlat (pencere donmasın)
        Thread starter = new Thread(() -> {
            try {
                SpringApplication app = new SpringApplication(CarrentalApplication.class);
                app.setAdditionalProfiles("desktop");
                app.setHeadless(false);
                context = app.run(getParameters().getRaw().toArray(new String[0]));

                String port = context.getEnvironment().getProperty("local.server.port");
                String url = "http://127.0.0.1:" + port + "/";
                Platform.runLater(() -> showWebView(root, url));
            } catch (Exception e) {
                Platform.runLater(() -> loading.setText("Could not start the application:\n" + e.getMessage()));
            }
        }, "spring-starter");
        starter.setDaemon(true);
        starter.start();
    }

    private void showWebView(StackPane root, String url) {
        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();

        // Silme onayı gibi JavaScript confirm() pencereleri (WebView bunları kendiliğinden göstermez)
        engine.setConfirmHandler(message -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
            alert.setTitle("Car Rental System");
            alert.setHeaderText(null);
            Optional<ButtonType> result = alert.showAndWait();
            return result.isPresent() && result.get() == ButtonType.OK;
        });

        engine.setOnAlert(event -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, event.getData(), ButtonType.OK);
            alert.setTitle("Car Rental System");
            alert.setHeaderText(null);
            alert.showAndWait();
        });

        engine.load(url);
        root.getChildren().setAll(webView);
    }

    @Override
    public void stop() {
        if (context != null) {
            context.close();
        }
        Platform.exit();
        System.exit(0);
    }
}
