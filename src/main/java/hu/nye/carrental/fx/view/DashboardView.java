package hu.nye.carrental.fx.view;

import hu.nye.carrental.fx.Icons;
import hu.nye.carrental.model.Car;
import hu.nye.carrental.model.CarStatus;
import hu.nye.carrental.model.Rental;
import hu.nye.carrental.repository.CarRepository;
import hu.nye.carrental.repository.CustomerRepository;
import hu.nye.carrental.repository.RentalRepository;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.springframework.context.ApplicationContext;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Dashboard (home screen): summary cards, current rentals, fleet status and insurance mix.
 * Clicking a summary card opens the related screen.
 */
public class DashboardView extends ScrollPane {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter LONG_DATE =
            DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT_DATE =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private final CarRepository carRepository;
    private final RentalRepository rentalRepository;
    private final CustomerRepository customerRepository;
    private final Consumer<String> navigator;

    public DashboardView(ApplicationContext context, Consumer<String> navigator) {
        this.carRepository = context.getBean(CarRepository.class);
        this.rentalRepository = context.getBean(RentalRepository.class);
        this.customerRepository = context.getBean(CustomerRepository.class);
        this.navigator = navigator;

        getStyleClass().add("page-scroll");
        setFitToWidth(true);
        refresh();
    }

    /** Reloads all numbers from the database. */
    public void refresh() {
        List<Car> cars = new ArrayList<>();
        carRepository.findAll().forEach(cars::add);
        List<Rental> rentals = new ArrayList<>();
        for (Rental rental : rentalRepository.findAllWithDetails()) {
            rentals.add(rental);
        }

        Map<CarStatus, Integer> carsByStatus = new EnumMap<>(CarStatus.class);
        for (CarStatus status : CarStatus.values()) {
            carsByStatus.put(status, 0);
        }
        for (Car car : cars) {
            if (car.getStatus() != null) {
                carsByStatus.merge(car.getStatus(), 1, Integer::sum);
            }
        }

        List<Rental> active = rentals.stream()
                .filter(Rental::isActive)
                .sorted(Comparator.comparing(Rental::getPlannedEndDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        long overdue = active.stream().filter(Rental::isOverdue).count();
        BigDecimal revenue = rentals.stream()
                .filter(rental -> !rental.isActive())
                .map(rental -> toDecimal(rental.getTotalPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expected = active.stream()
                .map(rental -> toDecimal(rental.getEstimatedPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        VBox page = new VBox(20);
        page.getStyleClass().add("page");

        Label title = new Label("Dashboard");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label(LocalDate.now().format(LONG_DATE) + "  ·  "
                + customerRepository.count() + " customers  ·  " + cars.size() + " cars in the fleet");
        subtitle.getStyleClass().add("page-subtitle");
        VBox header = new VBox(2, title, subtitle);

        GridPane stats = new GridPane();
        stats.setHgap(16);
        stats.setVgap(16);
        for (int i = 0; i < 4; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(25);
            column.setHgrow(Priority.ALWAYS);
            stats.getColumnConstraints().add(column);
        }
        stats.add(statCard(Icons.CAR, "accent-green", "Available cars",
                String.valueOf(carsByStatus.get(CarStatus.AVAILABLE)),
                "of " + cars.size() + " cars", "Cars"), 0, 0);
        stats.add(statCard(Icons.KEY, "accent-blue", "Active rentals",
                String.valueOf(active.size()),
                "expected " + MONEY.format(expected), "Rentals"), 1, 0);
        stats.add(statCard(Icons.WARNING, overdue > 0 ? "accent-red" : "accent-gray", "Overdue",
                String.valueOf(overdue),
                overdue > 0 ? "needs attention" : "all on time", "Rentals"), 2, 0);
        stats.add(statCard(Icons.MONEY, "accent-purple", "Revenue",
                MONEY.format(revenue),
                "from closed rentals", "Rentals"), 3, 0);

        VBox currentRentals = currentRentalsCard(active);
        VBox rightColumn = new VBox(20, fleetCard(carsByStatus, cars.size()), insuranceCard(rentals));
        HBox.setHgrow(currentRentals, Priority.ALWAYS);
        rightColumn.setPrefWidth(340);
        rightColumn.setMinWidth(300);
        HBox lower = new HBox(20, currentRentals, rightColumn);

        page.getChildren().addAll(header, stats, lower);
        setContent(page);
    }

    private Node statCard(String iconPath, String accent, String caption, String value,
                          String note, String target) {
        StackPane iconBox = new StackPane(Icons.of(iconPath, "stat-icon"));
        iconBox.getStyleClass().addAll("stat-icon-box", accent);

        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add("stat-caption");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("stat-value");
        Label noteLabel = new Label(note);
        noteLabel.getStyleClass().add("stat-note");

        VBox texts = new VBox(2, captionLabel, valueLabel, noteLabel);
        HBox card = new HBox(14, iconBox, texts);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().addAll("card", "stat-card");
        card.setMaxWidth(Double.MAX_VALUE);
        card.setOnMouseClicked(event -> navigator.accept(target));
        return card;
    }

    private VBox currentRentalsCard(List<Rental> active) {
        VBox card = card("Current rentals", "Open rentals, earliest due date first");
        if (active.isEmpty()) {
            Label empty = new Label("No active rentals at the moment.");
            empty.getStyleClass().add("hint");
            card.getChildren().add(empty);
        }
        LocalDate today = LocalDate.now();
        for (Rental rental : active.stream().limit(8).toList()) {
            Label car = new Label(rental.getCar().getDisplayName());
            car.getStyleClass().add("list-title");
            Label customer = new Label(rental.getCustomer().getFullName() + "  ·  " + rental.getInsuranceName());
            customer.getStyleClass().add("hint");
            VBox texts = new VBox(2, car, customer);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label badge;
            if (rental.isOverdue()) {
                long days = ChronoUnit.DAYS.between(rental.getPlannedEndDate(), today);
                badge = badge("Overdue " + days + (days == 1 ? " day" : " days"), "badge-danger");
            } else if (rental.getPlannedEndDate() != null && rental.getPlannedEndDate().equals(today)) {
                badge = badge("Due today", "badge-warning");
            } else {
                badge = badge("Due " + format(rental.getPlannedEndDate()), "badge-info");
            }

            HBox row = new HBox(12, Icons.of(Icons.CAR, "list-icon"), texts, spacer, badge);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("list-row");
            row.setOnMouseClicked(event -> navigator.accept("Rentals"));
            card.getChildren().add(row);
        }
        if (active.size() > 8) {
            Label more = new Label("+ " + (active.size() - 8) + " more on the Rentals screen");
            more.getStyleClass().add("hint");
            card.getChildren().add(more);
        }
        return card;
    }

    private VBox fleetCard(Map<CarStatus, Integer> carsByStatus, int total) {
        VBox card = card("Fleet status", null);
        for (CarStatus status : CarStatus.values()) {
            int count = carsByStatus.get(status);
            card.getChildren().add(barRow(status.getLabel(), count, total,
                    "bar-" + status.name().toLowerCase(Locale.ROOT)));
        }
        return card;
    }

    private VBox insuranceCard(List<Rental> rentals) {
        VBox card = card("Insurance chosen", "All rentals so far");
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Rental rental : rentals) {
            counts.merge(rental.getInsuranceName(), 1, Integer::sum);
        }
        if (counts.isEmpty()) {
            Label empty = new Label("No rentals yet.");
            empty.getStyleClass().add("hint");
            card.getChildren().add(empty);
        }
        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> card.getChildren().add(
                        barRow(entry.getKey(), entry.getValue(), rentals.size(), "bar-insurance")));
        return card;
    }

    private Node barRow(String name, int count, int total, String barClass) {
        Label nameLabel = new Label(name);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label countLabel = new Label(String.valueOf(count));
        countLabel.getStyleClass().add("list-title");
        HBox line = new HBox(nameLabel, spacer, countLabel);

        ProgressBar bar = new ProgressBar(total == 0 ? 0 : (double) count / total);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.getStyleClass().addAll("stat-bar", barClass);
        return new VBox(4, line, bar);
    }

    private VBox card(String title, String subtitle) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-title");
        VBox card = new VBox(10, titleLabel);
        if (subtitle != null) {
            Label subtitleLabel = new Label(subtitle);
            subtitleLabel.getStyleClass().add("hint");
            card.getChildren().add(subtitleLabel);
        }
        card.getStyleClass().add("card");
        return card;
    }

    private static Label badge(String text, String styleClass) {
        Label badge = new Label(text);
        badge.getStyleClass().addAll("badge", styleClass);
        return badge;
    }

    private static String format(LocalDate date) {
        return date == null ? "-" : date.format(SHORT_DATE);
    }

    /** Works whether the price is stored as BigDecimal or as a number. */
    private static BigDecimal toDecimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(value));
    }
}
