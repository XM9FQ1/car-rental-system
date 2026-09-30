package hu.nye.carrental.fx.view;

import hu.nye.carrental.fx.Dialogs;
import hu.nye.carrental.fx.Icons;
import hu.nye.carrental.model.InsurancePlan;
import hu.nye.carrental.model.Rental;
import hu.nye.carrental.repository.InsurancePlanRepository;
import hu.nye.carrental.repository.RentalRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Insurance screen (JavaFX): the insurance plans a customer can choose when renting a car,
 * shown as price cards (like on a real car rental website).
 */
public class InsuranceView extends VBox {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");

    private final InsurancePlanRepository insuranceRepository;
    private final RentalRepository rentalRepository;
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final FlowPane cards = new FlowPane(18, 18);

    public InsuranceView(ApplicationContext context) {
        this.insuranceRepository = context.getBean(InsurancePlanRepository.class);
        this.rentalRepository = context.getBean(RentalRepository.class);

        getStyleClass().add("page");

        Label title = new Label("Insurance");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Protection plans offered with every rental. "
                + "Price = (car daily price + insurance daily price) × days.");
        subtitle.getStyleClass().add("page-subtitle");
        VBox titles = new VBox(2, title, subtitle);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button addButton = new Button("+ New plan");
        addButton.getStyleClass().add("primary-button");
        addButton.setOnAction(event -> openEditor(null));
        HBox header = new HBox(12, titles, spacer, addButton);
        header.setAlignment(Pos.CENTER_LEFT);

        Label glossary = new Label("TPL = third-party liability   ·   CDW = collision damage waiver   ·   "
                + "TP = theft protection   ·   PAI = personal accident insurance   ·   "
                + "Deductible = the most the customer pays if the car is damaged");
        glossary.getStyleClass().add("hint");
        glossary.setWrapText(true);
        glossary.setMinHeight(Region.USE_PREF_SIZE);

        cards.setPadding(new Insets(4, 4, 24, 4));
        ScrollPane scroll = new ScrollPane(cards);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("page-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(header, glossary, scroll);
        refresh();
    }

    /** Reloads the plans from the database. */
    public void refresh() {
        Map<Long, Integer> usage = new HashMap<>();
        for (Rental rental : rentalRepository.findAllWithDetails()) {
            if (rental.getInsurancePlan() != null) {
                usage.merge(rental.getInsurancePlan().getId(), 1, Integer::sum);
            }
        }
        List<InsurancePlan> plans = insuranceRepository.findAllByOrderByDailyPriceAscNameAsc();
        cards.getChildren().clear();
        for (int i = 0; i < plans.size(); i++) {
            InsurancePlan plan = plans.get(i);
            boolean recommended = plans.size() >= 3 && i == plans.size() / 2;
            cards.getChildren().add(card(plan, usage.getOrDefault(plan.getId(), 0), recommended));
        }
        if (plans.isEmpty()) {
            Label empty = new Label("No insurance plans yet. Click \"+ New plan\" to add one.");
            empty.getStyleClass().add("hint");
            cards.getChildren().add(empty);
        }
    }

    private Node card(InsurancePlan plan, int usedIn, boolean recommended) {
        StackPane iconBox = new StackPane(Icons.of(Icons.SHIELD, "stat-icon"));
        iconBox.getStyleClass().addAll("stat-icon-box", recommended ? "accent-blue" : "accent-purple");
        Label name = new Label(plan.getName());
        name.getStyleClass().add("plan-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(12, iconBox, name, spacer);
        top.setAlignment(Pos.CENTER_LEFT);
        if (recommended) {
            Label popular = new Label("Most popular");
            popular.getStyleClass().addAll("badge", "badge-info");
            top.getChildren().add(popular);
        }

        BigDecimal daily = toDecimal(plan.getDailyPrice());
        Label price = new Label(daily.signum() == 0 ? "Included" : MONEY.format(daily));
        price.getStyleClass().add("plan-big-price");
        Label perDay = new Label(daily.signum() == 0 ? "in the car price" : "/ day");
        perDay.getStyleClass().add("hint");
        HBox priceLine = new HBox(6, price, perDay);
        priceLine.setAlignment(Pos.BASELINE_LEFT);

        BigDecimal deductible = toDecimal(plan.getDeductible());
        Label deductibleLabel = new Label(deductible.signum() == 0
                ? "No deductible" : "Deductible: " + MONEY.format(deductible));
        deductibleLabel.getStyleClass().addAll("badge", deductible.signum() == 0 ? "badge-success" : "badge-warning");

        Label description = new Label(plan.getDescription() == null ? "" : plan.getDescription());
        description.setWrapText(true);
        description.getStyleClass().add("plan-description");
        VBox.setVgrow(description, Priority.ALWAYS);
        description.setMaxHeight(Double.MAX_VALUE);
        description.setAlignment(Pos.TOP_LEFT);

        Label used = new Label("Used in " + usedIn + (usedIn == 1 ? " rental" : " rentals"));
        used.getStyleClass().add("hint");

        Button edit = new Button("Edit");
        edit.getStyleClass().add("secondary-button");
        edit.setOnAction(event -> openEditor(plan));
        Button delete = new Button("Delete");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(event -> delete(plan));
        Region bottomSpacer = new Region();
        HBox.setHgrow(bottomSpacer, Priority.ALWAYS);
        HBox bottom = new HBox(8, used, bottomSpacer, edit, delete);
        bottom.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(12, top, priceLine, deductibleLabel, description, bottom);
        card.getStyleClass().addAll("card", "plan-card");
        if (recommended) {
            card.getStyleClass().add("plan-card-recommended");
        }
        card.setPrefWidth(320);
        card.setMinHeight(320);
        card.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                openEditor(plan);
            }
        });
        return card;
    }

    private void openEditor(InsurancePlan existing) {
        boolean isNew = existing == null;
        TextField nameField = new TextField(isNew ? "" : existing.getName());
        nameField.setPromptText("e.g. Premium");
        TextField priceField = new TextField(isNew ? "" : String.valueOf(existing.getDailyPrice()));
        priceField.setPromptText("e.g. 24.00 (0 = included)");
        TextField deductibleField = new TextField(isNew ? "" : String.valueOf(existing.getDeductible()));
        deductibleField.setPromptText("e.g. 500.00 (0 = no deductible)");
        TextArea descriptionArea = new TextArea(isNew || existing.getDescription() == null ? "" : existing.getDescription());
        descriptionArea.setPromptText("What does this plan cover?");
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefRowCount(5);
        descriptionArea.setPrefWidth(360);

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.setPadding(new Insets(10, 4, 4, 4));
        form.addRow(0, new Label("Name"), nameField);
        form.addRow(1, new Label("Daily price"), priceField);
        form.addRow(2, new Label("Deductible"), deductibleField);
        form.addRow(3, new Label("Coverage"), descriptionArea);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "New insurance plan" : "Edit insurance plan");
        dialog.setHeaderText(isNew ? "Add a new insurance plan" : existing.getName());
        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(form);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            String error = save(existing, nameField.getText(), priceField.getText(),
                    deductibleField.getText(), descriptionArea.getText());
            if (error != null) {
                Dialogs.error(error);
                event.consume();
            }
        });

        dialog.showAndWait();
        refresh();
    }

    /** Validates and saves. Returns an error message, or null when saved. */
    private String save(InsurancePlan existing, String nameText, String priceText,
                        String deductibleText, String descriptionText) {
        String name = nameText == null ? "" : nameText.trim();
        BigDecimal price = parse(priceText);
        BigDecimal deductible = parse(deductibleText);
        if (price == null) {
            return "Daily price must be a number, for example 24.00 (use 0 if it is included).";
        }
        if (deductible == null) {
            return "Deductible must be a number, for example 500.00 (use 0 for no deductible).";
        }
        String description = descriptionText == null ? "" : descriptionText.trim();

        InsurancePlan plan;
        if (existing == null) {
            plan = new InsurancePlan(name, description, price, deductible);
        } else {
            plan = existing;
            plan.setName(name);
            plan.setDescription(description);
            plan.setDailyPrice(price);
            plan.setDeductible(deductible);
        }

        Set<ConstraintViolation<InsurancePlan>> violations = validator.validate(plan);
        if (!violations.isEmpty()) {
            return violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .sorted()
                    .collect(Collectors.joining("\n"));
        }
        boolean nameTaken = existing == null
                ? insuranceRepository.existsByNameIgnoreCase(name)
                : insuranceRepository.existsByNameIgnoreCaseAndIdNot(name, existing.getId());
        if (nameTaken) {
            return "An insurance plan with this name already exists.";
        }
        insuranceRepository.save(plan);
        return null;
    }

    private void delete(InsurancePlan plan) {
        if (!Dialogs.confirm("Are you sure you want to delete the \"" + plan.getName() + "\" plan?")) {
            return;
        }
        if (rentalRepository.existsByInsurancePlan_Id(plan.getId())) {
            Dialogs.error("This plan cannot be deleted because it is used by one or more rentals.");
            return;
        }
        try {
            insuranceRepository.deleteById(plan.getId());
        } catch (DataIntegrityViolationException e) {
            Dialogs.error("This plan cannot be deleted because it is used by one or more rentals.");
        }
        refresh();
    }

    private static BigDecimal parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(text.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Works whether the price is stored as BigDecimal or as a number. */
    private static BigDecimal toDecimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(value));
    }
}
