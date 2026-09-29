package hu.nye.carrental.fx.view;

import hu.nye.carrental.fx.Dialogs;
import hu.nye.carrental.model.Brand;
import hu.nye.carrental.model.Car;
import hu.nye.carrental.model.CarStatus;
import hu.nye.carrental.model.Category;
import hu.nye.carrental.repository.BrandRepository;
import hu.nye.carrental.repository.CarRepository;
import hu.nye.carrental.repository.CategoryRepository;
import hu.nye.carrental.repository.RentalRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

/** Cars screen (JavaFX): filters, list, add, edit, delete. */
public class CarView extends VBox {

    private static final List<String> FIELD_ORDER =
            List.of("brand", "category", "plateNumber", "dailyPrice", "status");

    private final CarRepository carRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final RentalRepository rentalRepository;
    private final Validator validator;

    private final TableView<Car> table = new TableView<>();
    private final ComboBox<Brand> brandFilter = new ComboBox<>();
    private final ComboBox<Category> categoryFilter = new ComboBox<>();
    private final ComboBox<CarStatus> statusFilter = new ComboBox<>();

    /** What the form produced: the car + whether the price text was not a valid number. */
    private record CarInput(Car car, boolean priceInvalid) {
    }

    public CarView(CarRepository carRepository, BrandRepository brandRepository,
                   CategoryRepository categoryRepository, RentalRepository rentalRepository,
                   Validator validator) {
        this.carRepository = carRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.rentalRepository = rentalRepository;
        this.validator = validator;
        getStyleClass().add("page");

        Label heading = new Label("Cars");
        heading.getStyleClass().add("page-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button addButton = new Button("+ New car");
        addButton.getStyleClass().add("primary-button");
        addButton.setOnAction(event -> openEditor(null));
        HBox header = new HBox(12, heading, spacer, addButton);
        header.setAlignment(Pos.CENTER_LEFT);

        configure(brandFilter, Brand::getName, "All brands");
        configure(categoryFilter, Category::getName, "All categories");
        configure(statusFilter, CarStatus::getLabel, "All statuses");
        brandFilter.getItems().add(null);
        brandFilter.getItems().addAll(brandRepository.findAllByOrderByNameAsc());
        categoryFilter.getItems().add(null);
        categoryFilter.getItems().addAll(categoryRepository.findAllByOrderByNameAsc());
        statusFilter.getItems().add(null);
        statusFilter.getItems().addAll(CarStatus.values());
        brandFilter.setPrefWidth(200);
        categoryFilter.setPrefWidth(200);
        statusFilter.setPrefWidth(200);
        brandFilter.setOnAction(event -> refresh());
        categoryFilter.setOnAction(event -> refresh());
        statusFilter.setOnAction(event -> refresh());

        Button clearButton = new Button("Clear");
        clearButton.getStyleClass().add("secondary-button");
        clearButton.setOnAction(event -> {
            brandFilter.setValue(null);
            categoryFilter.setValue(null);
            statusFilter.setValue(null);
            refresh();
        });
        HBox filters = new HBox(8, brandFilter, categoryFilter, statusFilter, clearButton);
        filters.setAlignment(Pos.CENTER_LEFT);

        TableColumn<Car, String> plateColumn = textColumn("Plate", Car::getPlateNumber, 140);
        plateColumn.setStyle("-fx-font-weight: bold;");
        TableColumn<Car, String> priceColumn = textColumn("Daily price",
                car -> String.format(Locale.US, "%.2f", car.getDailyPrice()), 120);
        priceColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<Car, CarStatus> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getStatus()));
        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(CarStatus status, boolean empty) {
                super.updateItem(status, empty);
                setText(null);
                if (empty || status == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(status.getLabel());
                badge.setStyle(badgeStyle(status));
                setGraphic(badge);
            }
        });
        statusColumn.setPrefWidth(140);

        table.getColumns().add(plateColumn);
        table.getColumns().add(textColumn("Brand", car -> car.getBrand().getName(), 180));
        table.getColumns().add(textColumn("Category", car -> car.getCategory().getName(), 180));
        table.getColumns().add(priceColumn);
        table.getColumns().add(statusColumn);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No cars found."));
        table.setRowFactory(tableView -> {
            TableRow<Car> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && row.getItem() != null) {
                    openEditor(row.getItem());
                }
            });
            return row;
        });
        VBox.setVgrow(table, Priority.ALWAYS);

        Button editButton = new Button("Edit");
        editButton.getStyleClass().add("secondary-button");
        editButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        editButton.setOnAction(event -> openEditor(table.getSelectionModel().getSelectedItem()));

        Button deleteButton = new Button("Delete");
        deleteButton.getStyleClass().add("danger-button");
        deleteButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        deleteButton.setOnAction(event -> delete(table.getSelectionModel().getSelectedItem()));

        Label hint = new Label("Tip: double-click a row to edit it.");
        hint.getStyleClass().add("hint");
        HBox actions = new HBox(8, editButton, deleteButton, hint);
        actions.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(header, filters, table, actions);
    }

    /** Reloads the table using the selected filters. */
    public void refresh() {
        Long brandId = brandFilter.getValue() == null ? null : brandFilter.getValue().getId();
        Long categoryId = categoryFilter.getValue() == null ? null : categoryFilter.getValue().getId();
        CarStatus status = statusFilter.getValue();
        table.setItems(FXCollections.observableArrayList(carRepository.search(brandId, categoryId, status)));
    }

    private TableColumn<Car, String> textColumn(String title, Function<Car, String> getter, double width) {
        TableColumn<Car, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(getter.apply(cell.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    private static String badgeStyle(CarStatus status) {
        String colors = switch (status) {
            case AVAILABLE -> "-fx-background-color: #198754; -fx-text-fill: white;";
            case RENTED -> "-fx-background-color: #ffc107; -fx-text-fill: #212529;";
            case MAINTENANCE -> "-fx-background-color: #6c757d; -fx-text-fill: white;";
        };
        return colors + " -fx-padding: 2 8 2 8; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 11px;";
    }

    /** Shows readable text in a drop-down list; a null item shows nullText (e.g. "All brands"). */
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

    private void openEditor(Car existing) {
        boolean isNew = (existing == null);

        ComboBox<Brand> brand = new ComboBox<>(FXCollections.observableArrayList(brandRepository.findAllByOrderByNameAsc()));
        configure(brand, Brand::getName, "-- Select brand --");
        ComboBox<Category> category = new ComboBox<>(FXCollections.observableArrayList(categoryRepository.findAllByOrderByNameAsc()));
        configure(category, Category::getName, "-- Select category --");
        TextField plate = new TextField(isNew ? "" : existing.getPlateNumber());
        plate.setPromptText("e.g. AA-BC-123");
        TextField price = new TextField(isNew ? "" : existing.getDailyPrice().toPlainString());
        price.setPromptText("e.g. 45.00");
        ComboBox<CarStatus> status = new ComboBox<>();
        configure(status, CarStatus::getLabel, "");

        boolean currentlyRented = !isNew && existing.getStatus() == CarStatus.RENTED;
        if (currentlyRented) {
            status.getItems().add(CarStatus.RENTED);
            status.setValue(CarStatus.RENTED);
            status.setDisable(true);
        } else {
            status.getItems().addAll(CarStatus.AVAILABLE, CarStatus.MAINTENANCE);
            status.setValue(isNew ? CarStatus.AVAILABLE : existing.getStatus());
        }

        if (!isNew) {
            brand.getItems().stream()
                    .filter(b -> b.getId().equals(existing.getBrand().getId()))
                    .findFirst().ifPresent(brand::setValue);
            category.getItems().stream()
                    .filter(c -> c.getId().equals(existing.getCategory().getId()))
                    .findFirst().ifPresent(category::setValue);
        }

        brand.setPrefWidth(300);
        category.setPrefWidth(300);
        status.setPrefWidth(300);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 10, 0, 10));
        grid.addRow(0, new Label("Brand:"), brand);
        grid.addRow(1, new Label("Category:"), category);
        grid.addRow(2, new Label("Plate number:"), plate);
        grid.addRow(3, new Label("Daily price:"), price);
        grid.addRow(4, new Label("Status:"), status);

        Label note = new Label(currentlyRented
                ? "This car is rented. Its status changes when the car is returned (Rentals screen)."
                : "The Rented status is set automatically when a rental is created.");
        note.getStyleClass().add("hint");
        note.setWrapText(true);
        note.setMaxWidth(420);

        Label errors = new Label();
        errors.setStyle("-fx-text-fill: #dc3545;");
        errors.setWrapText(true);
        errors.setMaxWidth(420);
        VBox content = new VBox(12, grid, note, errors);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "New car" : "Edit car");
        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(content);

        Long id = isNew ? null : existing.getId();
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            List<String> problems = validate(buildCar(id, brand, category, plate, price, status));
            if (!problems.isEmpty()) {
                errors.setText(String.join("\n", problems));
                event.consume(); // keep the dialog open
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveType) {
            carRepository.save(buildCar(id, brand, category, plate, price, status).car());
            refresh();
        }
    }

    private CarInput buildCar(Long id, ComboBox<Brand> brand, ComboBox<Category> category,
                              TextField plate, TextField price, ComboBox<CarStatus> status) {
        Car car = new Car();
        car.setId(id);
        car.setBrand(brand.getValue());
        car.setCategory(category.getValue());
        car.setPlateNumber(plate.getText().trim().toUpperCase());
        car.setStatus(status.getValue());

        boolean priceInvalid = false;
        String priceText = price.getText().trim().replace(',', '.');
        if (!priceText.isEmpty()) {
            try {
                car.setDailyPrice(new BigDecimal(priceText));
            } catch (NumberFormatException e) {
                priceInvalid = true;
            }
        }
        return new CarInput(car, priceInvalid);
    }

    /** Same rules as the web version: annotations on Car + unique plate + Rented status rules. */
    private List<String> validate(CarInput input) {
        Car car = input.car();
        List<ConstraintViolation<Car>> violations = new ArrayList<>(validator.validate(car));
        violations.sort(Comparator.comparingInt(v -> FIELD_ORDER.indexOf(v.getPropertyPath().toString())));

        List<String> problems = new ArrayList<>();
        for (ConstraintViolation<Car> violation : violations) {
            boolean isPriceField = "dailyPrice".equals(violation.getPropertyPath().toString());
            if (input.priceInvalid() && isPriceField) {
                continue;
            }
            problems.add("• " + violation.getMessage());
        }
        if (input.priceInvalid()) {
            problems.add("• Daily price must be a number, e.g. 45.00.");
        }

        Long id = car.getId();
        if (!car.getPlateNumber().isEmpty()) {
            boolean plateTaken = (id == null)
                    ? carRepository.existsByPlateNumberIgnoreCase(car.getPlateNumber())
                    : carRepository.existsByPlateNumberIgnoreCaseAndIdNot(car.getPlateNumber(), id);
            if (plateTaken) {
                problems.add("• A car with this plate number already exists.");
            }
        }

        boolean hasActiveRental = id != null && rentalRepository.existsByCar_IdAndReturnDateIsNull(id);
        if (hasActiveRental && car.getStatus() != CarStatus.RENTED) {
            problems.add("• This car has an active rental. Close the rental to make it available again.");
        } else if (!hasActiveRental && car.getStatus() == CarStatus.RENTED) {
            problems.add("• The Rented status is set automatically when a rental is created.");
        }
        return problems;
    }

    private void delete(Car car) {
        if (car == null || !Dialogs.confirm("Are you sure you want to delete car " + car.getPlateNumber() + "?")) {
            return;
        }
        try {
            carRepository.deleteById(car.getId());
            refresh();
        } catch (DataIntegrityViolationException e) {
            Dialogs.error("This car cannot be deleted because it has rentals.");
        }
    }
}
