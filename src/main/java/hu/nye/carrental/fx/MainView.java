package hu.nye.carrental.fx;

import hu.nye.carrental.fx.view.BrandView;
import hu.nye.carrental.fx.view.CarView;
import hu.nye.carrental.fx.view.CategoryView;
import hu.nye.carrental.fx.view.CustomerView;
import hu.nye.carrental.repository.BrandRepository;
import hu.nye.carrental.repository.CarRepository;
import hu.nye.carrental.repository.CategoryRepository;
import hu.nye.carrental.repository.CustomerRepository;
import hu.nye.carrental.repository.RentalRepository;
import jakarta.validation.Validator;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.springframework.context.ApplicationContext;

import java.util.LinkedHashMap;
import java.util.Map;

/** Main window: navigation menu on the left, the selected screen in the centre. */
public class MainView extends BorderPane {

    private final ApplicationContext context;
    private final Map<String, Button> navButtons = new LinkedHashMap<>();

    public MainView(ApplicationContext context) {
        this.context = context;

        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");
        Label title = new Label("Car Rental");
        title.getStyleClass().add("app-title");
        sidebar.getChildren().add(title);

        addNavButton(sidebar, "Rentals");
        addNavButton(sidebar, "Cars");
        addNavButton(sidebar, "Customers");
        addNavButton(sidebar, "Brands");
        addNavButton(sidebar, "Categories");

        setLeft(sidebar);
        show("Cars");
    }

    private void addNavButton(VBox sidebar, String name) {
        Button button = new Button(name);
        button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> show(name));
        navButtons.put(name, button);
        sidebar.getChildren().add(button);
    }

    private void show(String name) {
        navButtons.forEach((key, button) -> {
            button.getStyleClass().remove("active");
            if (key.equals(name)) {
                button.getStyleClass().add("active");
            }
        });

        Node view = switch (name) {
            case "Cars" -> {
                CarView carView = new CarView(
                        context.getBean(CarRepository.class),
                        context.getBean(BrandRepository.class),
                        context.getBean(CategoryRepository.class),
                        context.getBean(RentalRepository.class),
                        context.getBean(Validator.class));
                carView.refresh();
                yield carView;
            }
            case "Customers" -> {
                CustomerView customerView = new CustomerView(
                        context.getBean(CustomerRepository.class), context.getBean(Validator.class));
                customerView.refresh();
                yield customerView;
            }
            case "Brands" -> {
                BrandView brandView = new BrandView(context.getBean(BrandRepository.class));
                brandView.refresh();
                yield brandView;
            }
            case "Categories" -> {
                CategoryView categoryView = new CategoryView(context.getBean(CategoryRepository.class));
                categoryView.refresh();
                yield categoryView;
            }
            default -> comingSoon(name);
        };
        setCenter(view);
    }

    private Node comingSoon(String name) {
        Label label = new Label("The " + name + " screen will be added in the next step.");
        label.getStyleClass().add("hint");
        VBox box = new VBox(label);
        box.getStyleClass().add("page");
        return box;
    }
}
