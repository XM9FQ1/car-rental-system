package hu.nye.carrental.fx;

import hu.nye.carrental.CarrentalApplication;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * JavaFX desktop application.
 * Starts Spring (database, repositories, services) WITHOUT a web server,
 * then shows the native JavaFX screens.
 */
public class FxApp extends Application {

    private static HostServices hostServices;

    private ConfigurableApplicationContext context;
    private String stylesheet;

    /** Opens a web page in the default browser (used by "Find image online"). */
    public static void openInBrowser(String url) {
        if (hostServices != null) {
            hostServices.showDocument(url);
        }
    }

    @Override
    public void start(Stage stage) {
        hostServices = getHostServices();
        loadFonts();
        stylesheet = getClass().getResource("/fx/app.css").toExternalForm();
        styleEveryWindow();

        Label logo = new Label("Car Rental");
        logo.setGraphic(Icons.of(Icons.CAR, "splash-icon"));
        logo.getStyleClass().add("splash-title");
        Label loading = new Label("Starting Car Rental System...");
        loading.getStyleClass().add("splash-text");
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(36, 36);
        VBox splash = new VBox(18, logo, spinner, loading);
        splash.setAlignment(Pos.CENTER);
        splash.getStyleClass().add("splash");

        Scene scene = new Scene(splash, 1320, 840);

        stage.setTitle("Car Rental System");
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
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
                Platform.runLater(() -> {
                    spinner.setVisible(false);
                    loading.setText("Could not start the application:\n" + e.getMessage());
                });
            }
        }, "spring-starter");
        starter.setDaemon(true);
        starter.start();
    }

    /** Loads the bundled Inter font (used by app.css) before any screen is shown. */
    private void loadFonts() {
        for (String file : new String[] {"Inter-Regular", "Inter-Medium", "Inter-SemiBold", "Inter-Bold"}) {
            try (var in = FxApp.class.getResourceAsStream("/fx/fonts/" + file + ".ttf")) {
                if (in != null) {
                    Font.loadFont(in, 13);
                }
            } catch (Exception e) {
                System.err.println("Could not load font " + file + ": " + e.getMessage());
            }
        }
    }

    /** Adds app.css to every window (main window AND all pop-up dialogs), so they look the same. */
    private void styleEveryWindow() {
        Window.getWindows().addListener((ListChangeListener<Window>) change -> {
            while (change.next()) {
                for (Window window : change.getAddedSubList()) {
                    if (window.getScene() != null) {
                        addStylesheet(window.getScene());
                    }
                    window.sceneProperty().addListener((obs, oldScene, newScene) -> {
                        if (newScene != null) {
                            addStylesheet(newScene);
                        }
                    });
                }
            }
        });
    }

    private void addStylesheet(Scene scene) {
        if (!scene.getStylesheets().contains(stylesheet)) {
            scene.getStylesheets().add(stylesheet);
        }
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
