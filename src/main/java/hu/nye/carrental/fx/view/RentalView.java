package hu.nye.carrental.fx.view;

import hu.nye.carrental.fx.Dialogs;
import hu.nye.carrental.model.Car;
import hu.nye.carrental.model.CarStatus;
import hu.nye.carrental.model.Customer;
import hu.nye.carrental.model.Rental;
import hu.nye.carrental.repository.CarRepository;
import hu.nye.carrental.repository.CustomerRepository;
import hu.nye.carrental.repository.RentalRepository;
import hu.nye.carrental.service.RentalException;
import hu.nye.carrental.service.RentalService;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

/** Rentals screen (JavaFX): list, create a rental, return a car. */
public class RentalView extends VBox {

    private final RentalRepository rentalRepository;
    private final CarRepository carRepository;
    private final CustomerRepository customerRepository;
    private final RentalService rentalService;

    private final TableView<Rental> table = new TableView<>();
    private final ToggleGroup showGroup = new ToggleGroup();
    private final ToggleButton allButton = new ToggleButton("All");
    private final ToggleButton activeButton = new ToggleButton("Active");
    private final ToggleButton closedButton = new ToggleButton("Closed");

    public RentalView(RentalRepository rentalRepository, CarRepository carRepository,
                      CustomerRepository customerRepository, RentalService rentalService) {
        this.rentalRepository = rentalRepository;
        this.carRepository = carRepository;
        this.customerRepository = customerRepository;
        this.rentalService = rentalService;
        getStyleClass().add("page");

        Label heading = new Label("Rentals");
        heading.getStyleClass().add("page-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button addButton = new Button("+ New rental");
        addButton.getStyleClass().add("primary-button");
        addButton.setOnAction(event -> openNewRentalDialog());
        HBox header = new HBox(12, heading, spacer, addButton);
        header.setAlignment(Pos.CENTER_LEFT);

        allButton.setToggleGroup(showGroup);
        activeButton.setToggleGroup(showGroup);
        closedButton.setToggleGroup(showGroup);
        allButton.setSelected(true);
        showGroup.selectedToggleProperty().addListener((observable, oldToggle, newToggle) -> {
            if (newToggle == null) {
                oldToggle.setSelected(true); // one button must always stay selected
                return;
            }
            refresh();
        });
        HBox toggles = new HBox(0, allButton, activeButton, closedButton);

        TableColumn<Rental, String> priceColumn = textColumn("Price", rental -> rental.isActive()
                ? money(rental.getEstimatedPrice()) + " (est.)"
                : money(rental.getTotalPrice()), 130);
        priceColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<Rental, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(cell -> new SimpleStringProperty(statusOf(cell.getValue())));
        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                setText(null);
                if (empty || status == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(status);
                badge.setStyle(badgeStyle(status));
                setGraphic(badge);
            }
        });
        statusColumn.setPrefWidth(100);

        table.getColumns().add(textColumn("#", rental -> String.valueOf(rental.getId()), 50));
        table.getColumns().add(textColumn("Car", rental -> rental.getCar().getPlateNumber() + "  ("
                + rental.getCar().getBrand().getName() + " · " + rental.getCar().getCategory().getName() + ")", 260));
        table.getColumns().add(textColumn("Customer", rental -> rental.getCustomer().getFullName(), 170));
        table.getColumns().add(textColumn("Start", rental -> rental.getStartDate().toString(), 100));
        table.getColumns().add(textColumn("Planned return", rental -> rental.getPlannedEndDate().toString(), 120));
        table.getColumns().add(textColumn("Returned",
                rental -> rental.getReturnDate() == null ? "-" : rental.getReturnDate().toString(), 100));
        table.getColumns().add(priceColumn);
        table.getColumns().add(statusColumn);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No rentals found."));
        table.setRowFactory(tableView -> {
            TableRow<Rental> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && row.getItem() != null && row.getItem().isActive()) {
                    openReturnDialog(row.getItem());
                }
            });
            return row;
        });
        VBox.setVgrow(table, Priority.ALWAYS);

        Button returnButton = new Button("Return car");
        returnButton.setStyle("-fx-background-color: #198754; -fx-text-fill: white; "
                + "-fx-background-radius: 6; -fx-padding: 6 14 6 14; -fx-cursor: hand;");
        returnButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            Rental selected = table.getSelectionModel().getSelectedItem();
            return selected == null || !selected.isActive();
        }, table.getSelectionModel().selectedItemProperty()));
        returnButton.setOnAction(event -> openReturnDialog(table.getSelectionModel().getSelectedItem()));

        Label hint = new Label("Tip: double-click an active rental to return the car.");
        hint.getStyleClass().add("hint");
        HBox actions = new HBox(8, returnButton, hint);
        actions.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(header, toggles, table, actions);
    }

    /** Reloads the table for the selected button (All / Active / Closed). */
    public void refresh() {
        List<Rental> rentals;
        if (activeButton.isSelected()) {
            rentals = rentalRepository.findActiveWithDetails();
        } else if (closedButton.isSelected()) {
            rentals = rentalRepository.findClosedWithDetails();
        } else {
            rentals = rentalRepository.findAllWithDetails();
        }
        table.setItems(FXCollections.observableArrayList(rentals));
    }

    // ------------------------------------------------------------------ new rental

    private void openNewRentalDialog() {
        List<Car> cars = carRepository.search(null, null, CarStatus.AVAILABLE);
        List<Customer> customers = customerRepository.findAllByOrderByLastNameAscFirstNameAsc();
        if (cars.isEmpty()) {
            Dialogs.info("There are no available cars right now.");
            return;
        }
        if (customers.isEmpty()) {
            Dialogs.info("There are no customers yet. Add a customer first.");
            return;
        }

        ComboBox<Car> car = new ComboBox<>(FXCollections.observableArrayList(cars));
        configure(car, c -> c.getPlateNumber() + " - " + c.getBrand().getName() + " "
                + c.getCategory().getName() + " (" + money(c.getDailyPrice()) + " / day)", "-- Select car --");
        ComboBox<Customer> customer = new ComboBox<>(FXCollections.observableArrayList(customers));
        configure(customer, c -> c.getFullName() + " (" + c.getEmail() + ")", "-- Select customer --");
        DatePicker startDate = new DatePicker(LocalDate.now());
        DatePicker plannedEndDate = new DatePicker(LocalDate.now().plusDays(1));
        car.setPrefWidth(360);
        customer.setPrefWidth(360);

        Label estimate = new Label();
        estimate.setStyle("-fx-font-weight: bold;");
        Runnable updateEstimate = () -> {
            Car selectedCar = car.getValue();
            LocalDate start = startDate.getValue();
            LocalDate end = plannedEndDate.getValue();
            if (selectedCar == null || start == null || end == null || end.isBefore(start)) {
                estimate.setText("");
                return;
            }
            long days = Math.max(1, ChronoUnit.DAYS.between(start, end));
            BigDecimal price = selectedCar.getDailyPrice().multiply(BigDecimal.valueOf(days));
            estimate.setText("Estimated price: " + money(price) + " (" + days + " day" + (days == 1 ? "" : "s") + ")");
        };
        car.valueProperty().addListener((obs, oldValue, newValue) -> updateEstimate.run());
        startDate.valueProperty().addListener((obs, oldValue, newValue) -> updateEstimate.run());
        plannedEndDate.valueProperty().addListener((obs, oldValue, newValue) -> updateEstimate.run());

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 10, 0, 10));
        grid.addRow(0, new Label("Car:"), car);
        grid.addRow(1, new Label("Customer:"), customer);
        grid.addRow(2, new Label("Start date:"), startDate);
        grid.addRow(3, new Label("Planned return:"), plannedEndDate);

        Label note = new Label("Only available cars are listed.");
        note.getStyleClass().add("hint");
        Label errors = new Label();
        errors.setStyle("-fx-text-fill: #dc3545;");
        errors.setWrapText(true);
        errors.setMaxWidth(460);
        VBox content = new VBox(12, grid, estimate, note, errors);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("New rental");
        ButtonType createType = new ButtonType("Create rental", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createType, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(content);

        Rental[] created = new Rental[1];
        Button createButton = (Button) dialog.getDialogPane().lookupButton(createType);
        createButton.addEventFilter(ActionEvent.ACTION, event -> {
            List<String> problems = new ArrayList<>();
            if (car.getValue() == null) {
                problems.add("• Please select a car.");
            }
            if (customer.getValue() == null) {
                problems.add("• Please select a customer.");
            }
            if (startDate.getValue() == null) {
                problems.add("• Start date is required.");
            }
            if (plannedEndDate.getValue() == null) {
                problems.add("• Planned return date is required.");
            }
            if (startDate.getValue() != null && plannedEndDate.getValue() != null
                    && plannedEndDate.getValue().isBefore(startDate.getValue())) {
                problems.add("• Planned return date cannot be before the start date.");
            }
            if (problems.isEmpty()) {
                try {
                    created[0] = rentalService.createRental(car.getValue().getId(), customer.getValue().getId(),
                            startDate.getValue(), plannedEndDate.getValue());
                } catch (RentalException e) {
                    problems.add("• " + e.getMessage());
                }
            }
            if (!problems.isEmpty()) {
                errors.setText(String.join("\n", problems));
                event.consume(); // keep the dialog open
            }
        });

        dialog.showAndWait();
        if (created[0] != null) {
            refresh();
            Dialogs.info("Rental created: " + created[0].getCar().getPlateNumber()
                    + " is now rented to " + created[0].getCustomer().getFullName() + ".");
        }
    }

    // ------------------------------------------------------------------ return car

    private void openReturnDialog(Rental rental) {
        if (rental == null || !rental.isActive()) {
            return;
        }

        GridPane details = new GridPane();
        details.setHgap(16);
        details.setVgap(8);
        details.setPadding(new Insets(10, 10, 0, 10));
        details.addRow(0, bold("Car:"), new Label(rental.getCar().getPlateNumber() + " - "
                + rental.getCar().getBrand().getName() + " " + rental.getCar().getCategory().getName()));
        details.addRow(1, bold("Customer:"), new Label(rental.getCustomer().getFullName()));
        details.addRow(2, bold("Start date:"), new Label(rental.getStartDate().toString()));
        details.addRow(3, bold("Planned return:"), new Label(rental.getPlannedEndDate().toString()));
        details.addRow(4, bold("Daily price:"), new Label(money(rental.getDailyPrice())));

        DatePicker returnDate = new DatePicker(LocalDate.now());
        HBox dateRow = new HBox(12, bold("Return date:"), returnDate);
        dateRow.setAlignment(Pos.CENTER_LEFT);
        dateRow.setPadding(new Insets(0, 10, 0, 10));

        Label total = new Label();
        total.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        total.setPadding(new Insets(0, 10, 0, 10));
        Runnable updateTotal = () -> {
            LocalDate date = returnDate.getValue();
            if (date == null || date.isBefore(rental.getStartDate())) {
                total.setText("Total price: -");
                return;
            }
            long days = Math.max(1, ChronoUnit.DAYS.between(rental.getStartDate(), date));
            total.setText("Total price: " + money(rental.calculatePrice(date))
                    + " (" + days + " day" + (days == 1 ? "" : "s") + " × " + money(rental.getDailyPrice()) + ")");
        };
        returnDate.valueProperty().addListener((obs, oldValue, newValue) -> updateTotal.run());
        updateTotal.run();

        Label errors = new Label();
        errors.setStyle("-fx-text-fill: #dc3545;");
        errors.setWrapText(true);
        errors.setMaxWidth(460);
        errors.setPadding(new Insets(0, 10, 0, 10));
        VBox content = new VBox(14, details, dateRow, total, errors);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Return car");
        ButtonType confirmType = new ButtonType("Confirm return", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmType, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(content);

        Rental[] returned = new Rental[1];
        Button confirmButton = (Button) dialog.getDialogPane().lookupButton(confirmType);
        confirmButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                returned[0] = rentalService.returnCar(rental.getId(), returnDate.getValue());
            } catch (RentalException e) {
                errors.setText("• " + e.getMessage());
                event.consume(); // keep the dialog open
            }
        });

        dialog.showAndWait();
        if (returned[0] != null) {
            refresh();
            Dialogs.info("Car " + returned[0].getCar().getPlateNumber() + " returned. Total price: "
                    + money(returned[0].getTotalPrice()));
        }
    }

    // ------------------------------------------------------------------ helpers

    private TableColumn<Rental, String> textColumn(String title, Function<Rental, String> getter, double width) {
        TableColumn<Rental, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(getter.apply(cell.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    private static String statusOf(Rental rental) {
        if (rental.isOverdue()) {
            return "Overdue";
        }
        return rental.isActive() ? "Active" : "Closed";
    }

    private static String badgeStyle(String status) {
        String colors = switch (status) {
            case "Overdue" -> "-fx-background-color: #dc3545; -fx-text-fill: white;";
            case "Active" -> "-fx-background-color: #0d6efd; -fx-text-fill: white;";
            default -> "-fx-background-color: #6c757d; -fx-text-fill: white;";
        };
        return colors + " -fx-padding: 2 8 2 8; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 11px;";
    }

    private static String money(BigDecimal value) {
        return value == null ? "-" : String.format(Locale.US, "%.2f", value);
    }

    private static Label bold(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold;");
        return label;
    }

    private static <T> void configure(ComboBox<T> box, Function<T, String> text, String nullText) {
        box.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : (item == null ? nullText : text.apply(item)));
            }
        });
        box.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? nullText : text.apply(item));
            }
        });
    }
}
