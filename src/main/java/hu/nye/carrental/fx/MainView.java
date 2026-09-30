package hu.nye.carrental.fx;

import hu.nye.carrental.fx.view.BrandView;
import hu.nye.carrental.fx.view.CarView;
import hu.nye.carrental.fx.view.CategoryView;
import hu.nye.carrental.fx.view.CustomerView;
import hu.nye.carrental.fx.view.RentalView;
import hu.nye.carrental.repository.BrandRepository;
import hu.nye.carrental.repository.CarRepository;
import hu.nye.carrental.repository.CategoryRepository;
import hu.nye.carrental.repository.CustomerRepository;
import hu.nye.carrental.repository.RentalRepository;
import hu.nye.carrental.service.RentalService;
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
        title.setGraphic(Icons.of(Icons.CAR));
        sidebar.getChildren().add(title);

        addNavButton(sidebar, "Dashboard");
        addNavButton(sidebar, "Rentals");
        addNavButton(sidebar, "Cars");
        addNavButton(sidebar, "Customers");
        addNavButton(sidebar, "Brands");
        addNavButton(sidebar, "Categories");

        javafx.scene.layout.Region menuSpacer = new javafx.scene.layout.Region();
        javafx.scene.layout.VBox.setVgrow(menuSpacer, javafx.scene.layout.Priority.ALWAYS);
        Label footer = new Label("Car Rental System v3.0\nNYE - BAI0168");
        footer.getStyleClass().add("sidebar-footer");
        sidebar.getChildren().addAll(menuSpacer, footer);
        setLeft(sidebar);
        show("Dashboard");
    }

    private void addNavButton(VBox sidebar, String name) {
        Button button = new Button(name);
        button.getStyleClass().add("nav-button");
        button.setGraphic(Icons.nav(name));
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
            case "Dashboard" -> new hu.nye.carrental.fx.view.DashboardView(context, this::show);
            case "Rentals" -> {
                RentalView rentalView = new RentalView(
                        context.getBean(RentalRepository.class),
                        context.getBean(CarRepository.class),
                        context.getBean(CustomerRepository.class),
                        context.getBean(RentalService.class));
                rentalView.refresh();
                yield rentalView;
            }
            case "Cars" -> new hu.nye.carrental.fx.view.CarView(context);
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
            default -> new VBox();
        };
        setCenter(view);
    }
}
