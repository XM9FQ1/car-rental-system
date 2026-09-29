package hu.nye.carrental.fx;

import hu.nye.carrental.CarrentalApplication;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * JavaFX desktop application.
 * Starts Spring (database, repositories, services) WITHOUT a web server,
 * then shows the native JavaFX screens.
 */
public class FxApp extends Application {

    private ConfigurableApplicationContext context;

    @Override
    public void start(Stage stage) {
        Label loading = new Label("Starting Car Rental System...");
        loading.setStyle("-fx-font-size: 16px;");
        StackPane splash = new StackPane(loading);

        Scene scene = new Scene(splash, 1200, 780);
        scene.getStylesheets().add(getClass().getResource("/fx/app.css").toExternalForm());

        stage.setTitle("Car Rental System");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();

        Thread starter = new Thread(() -> {
            try {
                SpringApplication app = new SpringApplication(CarrentalApplication.class);
                app.setAdditionalProfiles("desktop");
                app.setWebApplicationType(WebApplicationType.NONE);
                app.setHeadless(false);
                context = app.run(getParameters().getRaw().toArray(new String[0]));
                Platform.runLater(() -> scene.setRoot(new MainView(context)));
            } catch (Exception e) {
                Platform.runLater(() -> loading.setText("Could not start the application:\n" + e.getMessage()));
            }
        }, "spring-starter");
        starter.setDaemon(true);
        starter.start();
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
