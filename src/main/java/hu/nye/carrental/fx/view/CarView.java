package hu.nye.carrental.fx.view;

import hu.nye.carrental.fx.DefaultCarImages;
import hu.nye.carrental.fx.Dialogs;
import hu.nye.carrental.fx.FxApp;
import hu.nye.carrental.fx.Icons;
import hu.nye.carrental.fx.ImageCache;
import hu.nye.carrental.model.Brand;
import hu.nye.carrental.model.Car;
import hu.nye.carrental.model.CarStatus;
import hu.nye.carrental.model.Category;
import hu.nye.carrental.repository.BrandRepository;
import hu.nye.carrental.repository.CarRepository;
import hu.nye.carrental.repository.CategoryRepository;
import hu.nye.carrental.repository.RentalRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
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
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cars screen (JavaFX): photo cards with filters, and an editor with
 * brand / model / year / category / plate / price / status / photo.
 */
public class CarView extends VBox {

    private static final double CARD_WIDTH = 280;
    private static final double IMAGE_HEIGHT = 165;
    private static final double PREVIEW_WIDTH = 320;
    private static final double PREVIEW_HEIGHT = 190;
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");

    private final CarRepository carRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final RentalRepository rentalRepository;
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private final TextField searchField = new TextField();
    private final ComboBox<Brand> brandFilter = new ComboBox<>();
    private final ComboBox<Category> categoryFilter = new ComboBox<>();
    private final ComboBox<CarStatus> statusFilter = new ComboBox<>();
    private final FlowPane grid = new FlowPane(18, 18);
    private final Label countLabel = new Label();
    private List<Car> allCars = new ArrayList<>();

    public CarView(ApplicationContext context) {
        this.carRepository = context.getBean(CarRepository.class);
        this.brandRepository = context.getBean(BrandRepository.class);
        this.categoryRepository = context.getBean(CategoryRepository.class);
        this.rentalRepository = context.getBean(RentalRepository.class);

        getStyleClass().add("page");

        Label title = new Label("Cars");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Your fleet with photos. Double-click a card to edit it.");
        subtitle.getStyleClass().add("page-subtitle");
        VBox titles = new VBox(2, title, subtitle);
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        Button addButton = new Button("+ New car");
        addButton.getStyleClass().add("primary-button");
        addButton.setOnAction(event -> openEditor(null));
        HBox header = new HBox(12, titles, headerSpacer, addButton);
        header.setAlignment(Pos.CENTER_LEFT);

        searchField.setPromptText("Search brand, model or plate");
        searchField.setPrefWidth(240);
        searchField.textProperty().addListener((obs, oldText, newText) -> applyFilters());
        setupFilter(brandFilter, brandRepository.findAllByOrderByNameAsc(), "All brands", Brand::getName);
        setupFilter(categoryFilter, categoryRepository.findAllByOrderByNameAsc(), "All categories", Category::getName);
        setupFilter(statusFilter, List.of(CarStatus.values()), "All statuses", CarStatus::getLabel);
        brandFilter.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        categoryFilter.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        statusFilter.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        Region filterSpacer = new Region();
        HBox.setHgrow(filterSpacer, Priority.ALWAYS);
        countLabel.getStyleClass().add("hint");
        HBox filters = new HBox(10, searchField, brandFilter, categoryFilter, statusFilter, filterSpacer, countLabel);
        filters.setAlignment(Pos.CENTER_LEFT);

        grid.setPadding(new Insets(4, 4, 24, 4));
        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("page-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(header, filters, scroll);
        refresh();
    }

    /** Reloads the cars from the database. */
    public void refresh() {
        allCars = new ArrayList<>(carRepository.search(null, null, null));
        for (Car car : allCars) {
            if (car.getImageUrl() == null || car.getImageUrl().isBlank()) {
                String url = DefaultCarImages.find(car.getBrand().getName(), car.getModel());
                if (url != null) {
                    car.setImageUrl(url);
                    carRepository.save(car);
                }
            }
        }
        applyFilters();
    }

    private void applyFilters() {
        String text = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        Brand brand = brandFilter.getValue();
        Category category = categoryFilter.getValue();
        CarStatus status = statusFilter.getValue();

        List<Car> visible = allCars.stream()
                .filter(car -> brand == null || car.getBrand().getId().equals(brand.getId()))
                .filter(car -> category == null || car.getCategory().getId().equals(category.getId()))
                .filter(car -> status == null || car.getStatus() == status)
                .filter(car -> text.isEmpty()
                        || (car.getBrand().getName() + " " + car.getModel() + " " + car.getPlateNumber())
                        .toLowerCase(Locale.ROOT).contains(text))
                .toList();

        grid.getChildren().clear();
        for (Car car : visible) {
            grid.getChildren().add(card(car));
        }
        if (visible.isEmpty()) {
            Label empty = new Label(allCars.isEmpty() ? "No cars yet. Click \"+ New car\" to add one."
                    : "No cars match the filters.");
            empty.getStyleClass().add("hint");
            grid.getChildren().add(empty);
        }
        countLabel.setText(visible.size() + " of " + allCars.size() + " cars");
    }

    private Node card(Car car) {
        StackPane imageBox = new StackPane();
        imageBox.getStyleClass().add("car-image");
        imageBox.setMinSize(CARD_WIDTH, IMAGE_HEIGHT);
        imageBox.setMaxSize(CARD_WIDTH, IMAGE_HEIGHT);
        Region placeholder = Icons.of(Icons.CAR, "car-placeholder");
        ImageView imageView = new ImageView();
        Label statusBadge = statusBadge(car.getStatus());
        StackPane.setAlignment(statusBadge, Pos.TOP_LEFT);
        StackPane.setMargin(statusBadge, new Insets(10));
        imageBox.getChildren().addAll(placeholder, imageView, statusBadge);
        Rectangle clip = new Rectangle(CARD_WIDTH, IMAGE_HEIGHT);
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        imageBox.setClip(clip);
        ImageCache.load(car.getImageUrl(), image -> {
            if (image != null) {
                showCover(imageView, image, CARD_WIDTH, IMAGE_HEIGHT);
                placeholder.setVisible(false);
            }
        });

        Label name = new Label(car.getBrand().getName() + " " + car.getModel());
        name.getStyleClass().add("car-name");
        Label details = new Label(car.getYear() + "  ·  " + car.getCategory().getName()
                + "  ·  " + car.getPlateNumber());
        details.getStyleClass().add("hint");

        Label price = new Label(car.getDailyPrice() == null ? "-" : MONEY.format(car.getDailyPrice()));
        price.getStyleClass().add("car-price");
        Label perDay = new Label("/ day");
        perDay.getStyleClass().add("hint");
        HBox priceLine = new HBox(4, price, perDay);
        priceLine.setAlignment(Pos.BASELINE_LEFT);

        Button edit = new Button("Edit");
        edit.getStyleClass().add("secondary-button");
        edit.setOnAction(event -> openEditor(car));
        Button delete = new Button("Delete");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(event -> delete(car));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bottom = new HBox(8, priceLine, spacer, edit, delete);
        bottom.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(8, imageBox, name, details, bottom);
        card.getStyleClass().addAll("card", "car-card");
        card.setPrefWidth(CARD_WIDTH + 24);
        card.setMaxWidth(CARD_WIDTH + 24);
        card.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                openEditor(car);
            }
        });
        return card;
    }

    private void openEditor(Car existing) {
        boolean isNew = existing == null;
        boolean hasActiveRental = !isNew && rentalRepository.existsByCar_IdAndReturnDateIsNull(existing.getId());

        List<Brand> brands = brandRepository.findAllByOrderByNameAsc();
        List<Category> categories = categoryRepository.findAllByOrderByNameAsc();
        if (brands.isEmpty() || categories.isEmpty()) {
            Dialogs.error("Please add at least one brand and one category first.");
            return;
        }

        ComboBox<Brand> brandBox = new ComboBox<>(FXCollections.observableArrayList(brands));
        brandBox.setConverter(converter(Brand::getName));
        ComboBox<Category> categoryBox = new ComboBox<>(FXCollections.observableArrayList(categories));
        categoryBox.setConverter(converter(Category::getName));
        TextField modelField = new TextField();
        modelField.setPromptText("e.g. Corolla");
        TextField yearField = new TextField();
        yearField.setPromptText("e.g. 2023");
        TextField plateField = new TextField();
        plateField.setPromptText("e.g. AA-AD-301");
        TextField priceField = new TextField();
        priceField.setPromptText("e.g. 45.00");
        ComboBox<CarStatus> statusBox = new ComboBox<>();
        statusBox.setConverter(converter(CarStatus::getLabel));
        TextField imageField = new TextField();
        imageField.setPromptText("https://... (JPG or PNG link)");
        Label statusHint = new Label();
        statusHint.getStyleClass().add("hint");
        statusHint.setWrapText(true);

        if (hasActiveRental) {
            statusBox.setItems(FXCollections.observableArrayList(CarStatus.RENTED));
            statusBox.setValue(CarStatus.RENTED);
            statusBox.setDisable(true);
            statusHint.setText("This car is rented now. The status changes back when it is returned.");
        } else {
            statusBox.setItems(FXCollections.observableArrayList(CarStatus.AVAILABLE, CarStatus.MAINTENANCE));
            statusBox.setValue(CarStatus.AVAILABLE);
            statusHint.setText("\"Rented\" is set automatically when a rental starts.");
        }

        if (!isNew) {
            brands.stream().filter(b -> b.getId().equals(existing.getBrand().getId()))
                    .findFirst().ifPresent(brandBox::setValue);
            categories.stream().filter(c -> c.getId().equals(existing.getCategory().getId()))
                    .findFirst().ifPresent(categoryBox::setValue);
            modelField.setText(existing.getModel());
            yearField.setText(existing.getYear() == null ? "" : String.valueOf(existing.getYear()));
            plateField.setText(existing.getPlateNumber());
            priceField.setText(existing.getDailyPrice() == null ? "" : String.valueOf(existing.getDailyPrice()));
            if (!hasActiveRental && existing.getStatus() == CarStatus.MAINTENANCE) {
                statusBox.setValue(CarStatus.MAINTENANCE);
            }
            imageField.setText(existing.getImageUrl() == null ? "" : existing.getImageUrl());
        }

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        int row = 0;
        form.addRow(row++, new Label("Brand"), brandBox);
        form.addRow(row++, new Label("Model"), modelField);
        form.addRow(row++, new Label("Year"), yearField);
        form.addRow(row++, new Label("Category"), categoryBox);
        form.addRow(row++, new Label("Plate number"), plateField);
        form.addRow(row++, new Label("Daily price"), priceField);
        form.addRow(row++, new Label("Status"), statusBox);
        form.add(statusHint, 1, row);
        for (Node node : List.of(brandBox, categoryBox, statusBox)) {
            ((Region) node).setMaxWidth(Double.MAX_VALUE);
        }
        modelField.setPrefWidth(240);
        statusHint.setMaxWidth(240);

        StackPane preview = new StackPane();
        preview.getStyleClass().add("car-image");
        preview.setMinSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);
        preview.setMaxSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);
        Region previewPlaceholder = Icons.of(Icons.CAR, "car-placeholder");
        ImageView previewImage = new ImageView();
        Label previewMessage = new Label();
        previewMessage.getStyleClass().add("preview-message");
        previewMessage.setMaxWidth(PREVIEW_WIDTH - 16);
        previewMessage.visibleProperty().bind(previewMessage.textProperty().isNotEmpty());
        StackPane.setAlignment(previewMessage, Pos.BOTTOM_CENTER);
        StackPane.setMargin(previewMessage, new Insets(8));
        preview.getChildren().addAll(previewPlaceholder, previewImage, previewMessage);
        Rectangle clip = new Rectangle(PREVIEW_WIDTH, PREVIEW_HEIGHT);
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        preview.setClip(clip);

        Runnable updatePreview = () -> {
            String url = ImageCache.normalize(imageField.getText());
            previewImage.setImage(null);
            previewPlaceholder.setVisible(true);
            if (url == null || url.isBlank()) {
                previewMessage.setText("No photo yet");
                return;
            }
            previewMessage.setText("Loading photo...");
            ImageCache.load(url, image -> {
                if (!url.equals(ImageCache.normalize(imageField.getText()))) {
                    return;
                }
                if (image == null) {
                    previewMessage.setText("Could not load this link. Use a direct JPG/PNG image link.");
                } else {
                    showCover(previewImage, image, PREVIEW_WIDTH, PREVIEW_HEIGHT);
                    previewPlaceholder.setVisible(false);
                    previewMessage.setText("");
                }
            });
        };
        imageField.setOnAction(event -> updatePreview.run());
        imageField.focusedProperty().addListener((obs, wasFocused, focused) -> {
            if (!focused) {
                updatePreview.run();
            }
        });

        Button findOnline = new Button("Find image online");
        findOnline.getStyleClass().add("secondary-button");
        findOnline.setOnAction(event -> {
            String brandName = brandBox.getValue() == null ? "" : brandBox.getValue().getName();
            String query = (brandName + " " + modelField.getText() + " " + yearField.getText()).trim();
            if (query.isEmpty()) {
                Dialogs.error("Choose a brand and type a model first.");
                return;
            }
            FxApp.openInBrowser("https://www.google.com/search?tbm=isch&q="
                    + URLEncoder.encode(query + " car", StandardCharsets.UTF_8));
        });
        Button showPreview = new Button("Preview");
        showPreview.getStyleClass().add("secondary-button");
        showPreview.setOnAction(event -> updatePreview.run());
        HBox imageButtons = new HBox(8, findOnline, showPreview);

        Label imageLabel = new Label("Photo");
        imageLabel.getStyleClass().add("list-title");
        Label imageHelp = new Label("1. Click \"Find image online\".\n"
                + "2. Open a photo, right-click it, choose \"Copy Image Address\".\n"
                + "3. Paste the link above and click \"Preview\".");
        imageHelp.getStyleClass().add("hint");
        imageHelp.setWrapText(true);
        imageHelp.setMaxWidth(PREVIEW_WIDTH);
        imageField.setPrefWidth(PREVIEW_WIDTH);
        VBox photoColumn = new VBox(10, imageLabel, preview, imageField, imageButtons, imageHelp);

        HBox content = new HBox(28, form, photoColumn);
        content.setPadding(new Insets(10, 4, 4, 4));
        updatePreview.run();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "New car" : "Edit car");
        dialog.setHeaderText(isNew ? "Add a new car to the fleet" : existing.getDisplayName());
        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(content);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            CarForm formValues = new CarForm(brandBox.getValue(), modelField.getText(), yearField.getText(),
                    categoryBox.getValue(), plateField.getText(), priceField.getText(),
                    statusBox.getValue(), imageField.getText());
            String error = save(existing, formValues);
            if (error != null) {
                Dialogs.error(error);
                event.consume();
            }
        });

        dialog.showAndWait();
        refresh();
    }

    /** Values typed into the editor. */
    private record CarForm(Brand brand, String model, String year, Category category,
                           String plate, String price, CarStatus status, String imageUrl) {
    }

    /** Validates and saves. Returns an error message, or null when saved. */
    private String save(Car existing, CarForm form) {
        String yearText = form.year() == null ? "" : form.year().trim();
        String priceText = form.price() == null ? "" : form.price().trim().replace(',', '.');
        if (yearText.isEmpty()) {
            return "Year is required.";
        }
        Integer year;
        try {
            year = Integer.valueOf(yearText);
        } catch (NumberFormatException e) {
            return "Year must be a whole number, for example 2023.";
        }
        if (priceText.isEmpty()) {
            return "Daily price is required.";
        }
        BigDecimal price;
        try {
            price = new BigDecimal(priceText);
        } catch (NumberFormatException e) {
            return "Daily price must be a number, for example 45.00.";
        }

        String plate = form.plate() == null ? "" : form.plate().trim().toUpperCase(Locale.ROOT);
        String imageUrl = ImageCache.normalize(form.imageUrl());

        Car car = existing == null ? new Car() : existing;
        car.setBrand(form.brand());
        car.setCategory(form.category());
        car.setModel(form.model() == null ? "" : form.model().trim());
        car.setYear(year);
        car.setPlateNumber(plate);
        car.setDailyPrice(price);
        car.setStatus(form.status());
        car.setImageUrl(imageUrl == null || imageUrl.isBlank() ? null : imageUrl);

        Set<ConstraintViolation<Car>> violations = validator.validate(car);
        if (!violations.isEmpty()) {
            return violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .sorted()
                    .collect(Collectors.joining("\n"));
        }
        boolean plateTaken = existing == null
                ? carRepository.existsByPlateNumberIgnoreCase(plate)
                : carRepository.existsByPlateNumberIgnoreCaseAndIdNot(plate, existing.getId());
        if (plateTaken) {
            return "A car with this plate number already exists.";
        }
        carRepository.save(car);
        return null;
    }

    private void delete(Car car) {
        if (!Dialogs.confirm("Are you sure you want to delete " + car.getDisplayName()
                + " (" + car.getPlateNumber() + ")?")) {
            return;
        }
        if (rentalRepository.existsByCar_IdAndReturnDateIsNull(car.getId())) {
            Dialogs.error("This car is rented right now, so it cannot be deleted.");
            return;
        }
        try {
            carRepository.deleteById(car.getId());
        } catch (DataIntegrityViolationException e) {
            Dialogs.error("This car cannot be deleted because it has rental history.");
        }
        refresh();
    }

    /** Fills the box like CSS "background-size: cover" (crops, never stretches). */
    private static void showCover(ImageView view, Image image, double width, double height) {
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

    private static Label statusBadge(CarStatus status) {
        Label badge = new Label(status == null ? "-" : status.getLabel());
        String style = status == null ? "badge-neutral" : switch (status) {
            case AVAILABLE -> "badge-success";
            case RENTED -> "badge-warning";
            default -> "badge-neutral";
        };
        badge.getStyleClass().addAll("badge", style);
        return badge;
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

    private static <T> void setupFilter(ComboBox<T> box, List<T> items, String allText, Function<T, String> toText) {
        List<T> withAll = new ArrayList<>();
        withAll.add(null);
        withAll.addAll(items);
        box.setItems(FXCollections.observableArrayList(withAll));
        Callback<ListView<T>, ListCell<T>> factory = list -> new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : (item == null ? allText : toText.apply(item)));
            }
        };
        box.setCellFactory(factory);
        box.setButtonCell(factory.call(null));
        box.setPromptText(allText);
        box.getSelectionModel().selectFirst();
    }
}
