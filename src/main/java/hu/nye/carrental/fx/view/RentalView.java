package hu.nye.carrental.fx.view;

import hu.nye.carrental.fx.Dialogs;
import hu.nye.carrental.fx.Icons;
import hu.nye.carrental.fx.ImageCache;
import hu.nye.carrental.model.Car;
import hu.nye.carrental.model.CarStatus;
import hu.nye.carrental.model.Customer;
import hu.nye.carrental.model.InsurancePlan;
import hu.nye.carrental.model.Rental;
import hu.nye.carrental.repository.CarRepository;
import hu.nye.carrental.repository.CustomerRepository;
import hu.nye.carrental.repository.InsurancePlanRepository;
import hu.nye.carrental.repository.RentalRepository;
import hu.nye.carrental.service.RentalService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
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
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.StringConverter;
import org.springframework.context.ApplicationContext;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Rentals screen (JavaFX): table with filters, new rental with insurance choice
 * and a live price breakdown, and returning a car.
 */
public class RentalView extends VBox {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private final RentalRepository rentalRepository;
    private final CarRepository carRepository;
    private final CustomerRepository customerRepository;
    private final InsurancePlanRepository insuranceRepository;
    private final RentalService rentalService;

    private final TableView<Rental> table = new TableView<>();
    private final TextField searchField = new TextField();
    private final ToggleGroup filterGroup = new ToggleGroup();
    private final Label countLabel = new Label();
    private List<Rental> allRentals = new ArrayList<>();

    public RentalView(ApplicationContext context) {
        this.rentalRepository = context.getBean(RentalRepository.class);
        this.carRepository = context.getBean(CarRepository.class);
        this.customerRepository = context.getBean(CustomerRepository.class);
        this.insuranceRepository = context.getBean(InsurancePlanRepository.class);
        this.rentalService = context.getBean(RentalService.class);

        getStyleClass().add("page");

        Label title = new Label("Rentals");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Start a rental with insurance, and return cars. Double-click an active rental to return it.");
        subtitle.getStyleClass().add("page-subtitle");
        VBox titles = new VBox(2, title, subtitle);
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        Button newButton = new Button("+ New rental");
        newButton.getStyleClass().add("primary-button");
        newButton.setOnAction(event -> openNewRental());
        HBox header = new HBox(12, titles, headerSpacer, newButton);
        header.setAlignment(Pos.CENTER_LEFT);

        HBox toggles = new HBox(6);
        for (String name : List.of("All", "Active", "Overdue", "Closed")) {
            ToggleButton toggle = new ToggleButton(name);
            toggle.setUserData(name);
            toggle.setToggleGroup(filterGroup);
            toggles.getChildren().add(toggle);
        }
        filterGroup.selectToggle(filterGroup.getToggles().get(0));
        filterGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                filterGroup.selectToggle(oldToggle);
            } else {
                applyFilters();
            }
        });
        searchField.setPromptText("Search customer, car or plate");
        searchField.setPrefWidth(260);
        searchField.textProperty().addListener((obs, oldText, newText) -> applyFilters());
        Region filterSpacer = new Region();
        HBox.setHgrow(filterSpacer, Priority.ALWAYS);
        countLabel.getStyleClass().add("hint");
        HBox filters = new HBox(12, toggles, searchField, filterSpacer, countLabel);
        filters.setAlignment(Pos.CENTER_LEFT);

        buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        Button returnButton = new Button("Return car");
        returnButton.getStyleClass().add("secondary-button");
        returnButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        returnButton.setOnAction(event -> openReturn(table.getSelectionModel().getSelectedItem()));
        Label hint = new Label("Select an active rental, then click \"Return car\".");
        hint.getStyleClass().add("hint");
        HBox actions = new HBox(10, returnButton, hint);
        actions.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(header, filters, table, actions);
        refresh();
    }

    /** Reloads the rentals from the database. */
    public void refresh() {
        allRentals = new ArrayList<>();
        for (Rental rental : rentalRepository.findAllWithDetails()) {
            allRentals.add(rental);
        }
        allRentals.sort(Comparator.comparing(Rental::isActive).reversed()
                .thenComparing(Rental::getStartDate, Comparator.nullsLast(Comparator.reverseOrder())));
        applyFilters();
    }

    private void applyFilters() {
        String filter = filterGroup.getSelectedToggle() == null
                ? "All" : String.valueOf(filterGroup.getSelectedToggle().getUserData());
        String text = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);

        List<Rental> visible = allRentals.stream()
                .filter(rental -> switch (filter) {
                    case "Active" -> rental.isActive();
                    case "Overdue" -> rental.isActive() && rental.isOverdue();
                    case "Closed" -> !rental.isActive();
                    default -> true;
                })
                .filter(rental -> text.isEmpty()
                        || (rental.getCustomer().getFullName() + " " + rental.getCar().getDisplayName()
                        + " " + rental.getCar().getPlateNumber()).toLowerCase(Locale.ROOT).contains(text))
                .toList();
        table.setItems(FXCollections.observableArrayList(visible));
        countLabel.setText(visible.size() + " of " + allRentals.size() + " rentals");
    }

    private void buildTable() {
        TableColumn<Rental, Rental> photoColumn = new TableColumn<>("");
        photoColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        photoColumn.setCellFactory(column -> new TableCell<>() {
            private final ImageView view = new ImageView();
            private final StackPane box = new StackPane(view);
            private String loadedUrl;

            {
                box.setMinSize(64, 40);
                box.setMaxSize(64, 40);
                box.getStyleClass().add("car-image");
                Rectangle clip = new Rectangle(64, 40);
                clip.setArcWidth(10);
                clip.setArcHeight(10);
                box.setClip(clip);
            }

            @Override
            protected void updateItem(Rental rental, boolean empty) {
                super.updateItem(rental, empty);
                if (empty || rental == null) {
                    setGraphic(null);
                    loadedUrl = null;
                    return;
                }
                String url = rental.getCar().getImageUrl();
                if (url == null || !url.equals(loadedUrl)) {
                    view.setImage(null);
                    loadedUrl = url;
                    ImageCache.load(url, image -> {
                        if (image != null && url.equals(loadedUrl)) {
                            showCover(view, image, 64, 40);
                        }
                    });
                }
                setGraphic(box);
            }
        });
        photoColumn.setPrefWidth(80);
        photoColumn.setMinWidth(80);
        photoColumn.setMaxWidth(80);
        photoColumn.setSortable(false);

        table.getColumns().add(photoColumn);
        table.getColumns().add(textColumn("Car", 170, r -> r.getCar().getDisplayName() + "\n" + r.getCar().getPlateNumber()));
        table.getColumns().add(textColumn("Customer", 140, r -> r.getCustomer().getFullName()));
        table.getColumns().add(textColumn("Start", 95, r -> format(r.getStartDate())));
        table.getColumns().add(textColumn("Planned end", 95, r -> format(r.getPlannedEndDate())));
        table.getColumns().add(textColumn("Returned", 95, r -> format(r.getReturnDate())));
        table.getColumns().add(textColumn("Insurance", 95, Rental::getInsuranceName));
        table.getColumns().add(textColumn("Daily rate", 85, r -> money(dailyRate(r))));
        table.getColumns().add(textColumn("Total", 105, r -> r.isActive()
                ? money(toDecimal(r.getEstimatedPrice())) + " (est.)"
                : money(toDecimal(r.getTotalPrice()))));

        TableColumn<Rental, Rental> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Rental rental, boolean empty) {
                super.updateItem(rental, empty);
                if (empty || rental == null) {
                    setGraphic(null);
                    return;
                }
                Label badge;
                if (!rental.isActive()) {
                    badge = badge("Closed", "badge-neutral");
                } else if (rental.isOverdue()) {
                    badge = badge("Overdue", "badge-danger");
                } else {
                    badge = badge("Active", "badge-info");
                }
                setGraphic(badge);
            }
        });
        statusColumn.setPrefWidth(90);
        table.getColumns().add(statusColumn);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(56);
        table.setPlaceholder(new Label("No rentals match the filter."));
        table.setRowFactory(tableView -> {
            TableRow<Rental> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && row.getItem() != null) {
                    openReturn(row.getItem());
                }
            });
            return row;
        });
    }

    private static TableColumn<Rental, String> textColumn(String title, double width, Function<Rental, String> value) {
        TableColumn<Rental, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    // ------------------------------------------------------------------ new rental

    private void openNewRental() {
        List<Customer> customers = customerRepository.findAllByOrderByLastNameAscFirstNameAsc();
        List<Car> cars = new ArrayList<>();
        for (Car car : carRepository.search(null, null, null)) {
            if (car.getStatus() == CarStatus.AVAILABLE) {
                cars.add(car);
            }
        }
        List<InsurancePlan> plans = insuranceRepository.findAllByOrderByDailyPriceAscNameAsc();
        if (customers.isEmpty()) {
            Dialogs.error("There are no customers yet. Add a customer first.");
            return;
        }
        if (cars.isEmpty()) {
            Dialogs.error("There is no available car right now.");
            return;
        }

        ComboBox<Customer> customerBox = new ComboBox<>(FXCollections.observableArrayList(customers));
        customerBox.setConverter(converter(Customer::getFullName));
        customerBox.setPromptText("Choose a customer");
        customerBox.setMaxWidth(Double.MAX_VALUE);

        ComboBox<Car> carBox = new ComboBox<>(FXCollections.observableArrayList(cars));
        carBox.setConverter(converter(car -> car.getDisplayName() + "  ·  " + car.getPlateNumber()
                + "  ·  " + money(toDecimal(car.getDailyPrice())) + " / day"));
        carBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Car car, boolean empty) {
                super.updateItem(car, empty);
                if (empty || car == null) {
                    setText(null);
                    return;
                }
                setText(car.getDisplayName() + "  ·  " + car.getCategory().getName() + "  ·  "
                        + car.getPlateNumber() + "  ·  " + money(toDecimal(car.getDailyPrice())) + " / day");
            }
        });
        carBox.setPromptText("Choose an available car");
        carBox.setMaxWidth(Double.MAX_VALUE);

        DatePicker startPicker = new DatePicker(LocalDate.now());
        DatePicker endPicker = new DatePicker(LocalDate.now().plusDays(3));

        StackPane carPhoto = new StackPane();
        carPhoto.getStyleClass().add("car-image");
        carPhoto.setMinSize(300, 170);
        carPhoto.setMaxSize(300, 170);
        Region photoPlaceholder = Icons.of(Icons.CAR, "car-placeholder");
        ImageView photoView = new ImageView();
        carPhoto.getChildren().addAll(photoPlaceholder, photoView);
        Rectangle clip = new Rectangle(300, 170);
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        carPhoto.setClip(clip);

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.addRow(0, new Label("Customer"), customerBox);
        form.addRow(1, new Label("Car"), carBox);
        form.addRow(2, new Label("Start date"), startPicker);
        form.addRow(3, new Label("Planned end"), endPicker);
        customerBox.setPrefWidth(300);
        carBox.setPrefWidth(300);
        VBox left = new VBox(14, carPhoto, form);

        Label insuranceTitle = new Label("Insurance");
        insuranceTitle.getStyleClass().add("list-title");
        Label insuranceHint = new Label("Choose the protection level. The deductible is what the customer pays in case of damage.");
        insuranceHint.getStyleClass().add("hint");
        insuranceHint.setWrapText(true);
        insuranceHint.setMaxWidth(360);
        ToggleGroup planGroup = new ToggleGroup();
        VBox planBox = new VBox(8);
        for (InsurancePlan plan : plans) {
            ToggleButton option = new ToggleButton();
            option.setUserData(plan);
            option.setToggleGroup(planGroup);
            option.getStyleClass().add("plan-option");
            option.setMaxWidth(Double.MAX_VALUE);
            option.setPrefWidth(360);
            option.setGraphic(planSummary(plan));
            planBox.getChildren().add(option);
        }
        if (!planGroup.getToggles().isEmpty()) {
            planGroup.selectToggle(planGroup.getToggles().get(0));
        }
        planGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null && oldToggle != null) {
                planGroup.selectToggle(oldToggle);
            }
        });

        Label carLine = new Label();
        Label insuranceLine = new Label();
        Label totalLine = new Label();
        totalLine.getStyleClass().add("total-line");
        Label daysLine = new Label();
        daysLine.getStyleClass().add("hint");
        VBox breakdown = new VBox(6, styled(new Label("Price breakdown"), "list-title"),
                carLine, insuranceLine, styled(new Region(), "divider"), totalLine, daysLine);
        breakdown.getStyleClass().add("breakdown");

        VBox right = new VBox(10, insuranceTitle, insuranceHint, planBox, breakdown);

        Runnable update = () -> {
            Car car = carBox.getValue();
            InsurancePlan plan = planGroup.getSelectedToggle() == null
                    ? null : (InsurancePlan) planGroup.getSelectedToggle().getUserData();
            long days = days(startPicker.getValue(), endPicker.getValue());
            BigDecimal carDaily = car == null ? BigDecimal.ZERO : toDecimal(car.getDailyPrice());
            BigDecimal insuranceDaily = plan == null ? BigDecimal.ZERO : toDecimal(plan.getDailyPrice());
            BigDecimal carTotal = carDaily.multiply(BigDecimal.valueOf(days));
            BigDecimal insuranceTotal = insuranceDaily.multiply(BigDecimal.valueOf(days));
            carLine.setText("Car:  " + money(carDaily) + " × " + days + " days = " + money(carTotal));
            insuranceLine.setText("Insurance" + (plan == null ? "" : " (" + plan.getName() + ")") + ":  "
                    + money(insuranceDaily) + " × " + days + " days = " + money(insuranceTotal));
            totalLine.setText("Estimated total:  " + money(carTotal.add(insuranceTotal)));
            daysLine.setText(days + (days == 1 ? " day" : " days")
                    + " (the final price is calculated when the car is returned)");

            photoView.setImage(null);
            photoPlaceholder.setVisible(true);
            if (car != null) {
                String url = car.getImageUrl();
                ImageCache.load(url, image -> {
                    if (image != null && carBox.getValue() == car) {
                        showCover(photoView, image, 300, 170);
                        photoPlaceholder.setVisible(false);
                    }
                });
            }
        };
        carBox.valueProperty().addListener((obs, oldValue, newValue) -> update.run());
        startPicker.valueProperty().addListener((obs, oldValue, newValue) -> update.run());
        endPicker.valueProperty().addListener((obs, oldValue, newValue) -> update.run());
        planGroup.selectedToggleProperty().addListener((obs, oldValue, newValue) -> update.run());
        update.run();

        HBox content = new HBox(28, left, right);
        content.setPadding(new Insets(10, 4, 4, 4));

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("New rental");
        dialog.setHeaderText("Start a new rental");
        ButtonType startType = new ButtonType("Start rental", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(startType, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(content);

        Button startButton = (Button) dialog.getDialogPane().lookupButton(startType);
        startButton.addEventFilter(ActionEvent.ACTION, event -> {
            String error = null;
            if (customerBox.getValue() == null) {
                error = "Please choose a customer.";
            } else if (carBox.getValue() == null) {
                error = "Please choose a car.";
            } else if (startPicker.getValue() == null || endPicker.getValue() == null) {
                error = "Please choose the start and the planned end date.";
            } else if (endPicker.getValue().isBefore(startPicker.getValue())) {
                error = "The planned end date cannot be before the start date.";
            }
            if (error == null) {
                InsurancePlan plan = planGroup.getSelectedToggle() == null
                        ? null : (InsurancePlan) planGroup.getSelectedToggle().getUserData();
                try {
                    rentalService.createRental(carBox.getValue().getId(), customerBox.getValue().getId(),
                            startPicker.getValue(), endPicker.getValue(), plan == null ? null : plan.getId());
                } catch (RuntimeException e) {
                    error = e.getMessage() == null ? "The rental could not be created." : e.getMessage();
                }
            }
            if (error != null) {
                Dialogs.error(error);
                event.consume();
            }
        });

        dialog.showAndWait();
        refresh();
    }

    private Node planSummary(InsurancePlan plan) {
        Label name = new Label(plan.getName());
        name.getStyleClass().add("plan-name");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        BigDecimal daily = toDecimal(plan.getDailyPrice());
        Label price = new Label(daily.signum() == 0 ? "Included" : "+ " + money(daily) + " / day");
        price.getStyleClass().add("plan-price");
        HBox top = new HBox(8, name, spacer, price);
        top.setAlignment(Pos.CENTER_LEFT);

        BigDecimal deductible = toDecimal(plan.getDeductible());
        Label deductibleLabel = deductible.signum() == 0
                ? badge("No deductible", "badge-success")
                : badge("Deductible: " + money(deductible), "badge-warning");

        Label description = new Label(plan.getDescription() == null ? "" : plan.getDescription());
        description.getStyleClass().add("hint");
        description.setWrapText(true);
        description.setMaxWidth(320);

        VBox box = new VBox(6, top, deductibleLabel, description);
        box.setAlignment(Pos.TOP_LEFT);
        box.setPrefWidth(330);
        return box;
    }

    // ------------------------------------------------------------------ return

    private void openReturn(Rental rental) {
        if (rental == null) {
            return;
        }
        if (!rental.isActive()) {
            Dialogs.info("This rental is already closed (returned on " + format(rental.getReturnDate()) + ").");
            return;
        }

        DatePicker returnPicker = new DatePicker(LocalDate.now());
        Label carLine = new Label();
        Label insuranceLine = new Label();
        Label totalLine = new Label();
        totalLine.getStyleClass().add("total-line");
        Label lateLine = new Label();
        lateLine.getStyleClass().add("hint");

        Runnable update = () -> {
            long days = days(rental.getStartDate(), returnPicker.getValue());
            BigDecimal carDaily = toDecimal(rental.getDailyPrice());
            BigDecimal insuranceDaily = toDecimal(rental.getInsuranceDailyPriceOrZero());
            BigDecimal carTotal = carDaily.multiply(BigDecimal.valueOf(days));
            BigDecimal insuranceTotal = insuranceDaily.multiply(BigDecimal.valueOf(days));
            carLine.setText("Car:  " + money(carDaily) + " × " + days + " days = " + money(carTotal));
            insuranceLine.setText("Insurance (" + rental.getInsuranceName() + "):  " + money(insuranceDaily)
                    + " × " + days + " days = " + money(insuranceTotal));
            totalLine.setText("Total to pay:  " + money(carTotal.add(insuranceTotal)));
            LocalDate planned = rental.getPlannedEndDate();
            LocalDate chosen = returnPicker.getValue();
            if (planned != null && chosen != null && chosen.isAfter(planned)) {
                long late = ChronoUnit.DAYS.between(planned, chosen);
                lateLine.setText("Returned " + late + (late == 1 ? " day" : " days") + " late (planned: "
                        + format(planned) + ").");
            } else {
                lateLine.setText("Planned end: " + format(planned));
            }
        };
        returnPicker.valueProperty().addListener((obs, oldValue, newValue) -> update.run());
        update.run();

        GridPane info = new GridPane();
        info.setHgap(12);
        info.setVgap(8);
        info.addRow(0, new Label("Car"), new Label(rental.getCar().getDisplayName() + "  ·  " + rental.getCar().getPlateNumber()));
        info.addRow(1, new Label("Customer"), new Label(rental.getCustomer().getFullName()));
        info.addRow(2, new Label("Start date"), new Label(format(rental.getStartDate())));
        info.addRow(3, new Label("Return date"), returnPicker);

        VBox breakdown = new VBox(6, carLine, insuranceLine, styled(new Region(), "divider"),
                totalLine, lateLine);
        breakdown.getStyleClass().add("breakdown");
        VBox content = new VBox(16, info, breakdown);
        content.setPadding(new Insets(10, 4, 4, 4));
        content.setPrefWidth(460);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Return car");
        dialog.setHeaderText("Return " + rental.getCar().getDisplayName());
        ButtonType returnType = new ButtonType("Return car", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(returnType, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(content);

        Button returnButton = (Button) dialog.getDialogPane().lookupButton(returnType);
        returnButton.addEventFilter(ActionEvent.ACTION, event -> {
            String error = null;
            if (returnPicker.getValue() == null) {
                error = "Please choose the return date.";
            } else {
                try {
                    rentalService.returnCar(rental.getId(), returnPicker.getValue());
                } catch (RuntimeException e) {
                    error = e.getMessage() == null ? "The car could not be returned." : e.getMessage();
                }
            }
            if (error != null) {
                Dialogs.error(error);
                event.consume();
            }
        });

        dialog.showAndWait();
        refresh();
    }

    // ------------------------------------------------------------------ helpers

    private static BigDecimal dailyRate(Rental rental) {
        return toDecimal(rental.getDailyPrice()).add(toDecimal(rental.getInsuranceDailyPriceOrZero()));
    }

    /** Same rule as the Rental entity: at least 1 day. */
    private static long days(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return 1;
        }
        return Math.max(1, ChronoUnit.DAYS.between(start, end));
    }

    private static <T extends Node> T styled(T node, String styleClass) {
        node.getStyleClass().add(styleClass);
        return node;
    }

    private static Label badge(String text, String styleClass) {
        Label badge = new Label(text);
        badge.getStyleClass().addAll("badge", styleClass);
        return badge;
    }

    private static String format(LocalDate date) {
        return date == null ? "-" : date.format(DATE);
    }

    private static String money(BigDecimal value) {
        return MONEY.format(value);
    }

    /** Works whether the price is stored as BigDecimal or as a number. */
    private static BigDecimal toDecimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(value));
    }

    private static <T> StringConverter<T> converter(Function<T, String> toText) {
        return new StringConverter<>() {
            @Override
            public String toString(T item) {
                return item == null ? "" : toText.apply(item);
            }

            @Override
            public T fromString(String text) {
                return null;
            }
        };
    }

    static void showCover(ImageView view, Image image, double width, double height) {
        double imageWidth = image.getWidth();
        double imageHeight = image.getHeight();
        double targetRatio = width / height;
        Rectangle2D viewport;
        if (imageWidth / imageHeight > targetRatio) {
            double cropWidth = imageHeight * targetRatio;
            viewport = new Rectangle2D((imageWidth - cropWidth) / 2, 0, cropWidth, imageHeight);
        } else {
            double cropHeight = imageWidth / targetRatio;
            viewport = new Rectangle2D(0, (imageHeight - cropHeight) / 2, imageWidth, cropHeight);
        }
        view.setImage(image);
        view.setViewport(viewport);
        view.setPreserveRatio(false);
        view.setSmooth(true);
        view.setFitWidth(width);
        view.setFitHeight(height);
    }
}
