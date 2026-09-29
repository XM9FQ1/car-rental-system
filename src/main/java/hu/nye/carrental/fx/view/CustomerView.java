package hu.nye.carrental.fx.view;

import hu.nye.carrental.fx.Dialogs;
import hu.nye.carrental.model.Customer;
import hu.nye.carrental.repository.CustomerRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** Customers screen (JavaFX): search, list, add, edit, delete. */
public class CustomerView extends VBox {

    private static final List<String> FIELD_ORDER =
            List.of("firstName", "lastName", "email", "phone", "licenseNumber");

    private final CustomerRepository repository;
    private final Validator validator;
    private final TableView<Customer> table = new TableView<>();
    private final TextField searchField = new TextField();

    public CustomerView(CustomerRepository repository, Validator validator) {
        this.repository = repository;
        this.validator = validator;
        getStyleClass().add("page");

        Label heading = new Label("Customers");
        heading.getStyleClass().add("page-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button addButton = new Button("+ New customer");
        addButton.getStyleClass().add("primary-button");
        addButton.setOnAction(event -> openEditor(null));
        HBox header = new HBox(12, heading, spacer, addButton);
        header.setAlignment(Pos.CENTER_LEFT);

        searchField.setPromptText("Search by name or email");
        searchField.setPrefWidth(360);
        searchField.setOnAction(event -> refresh());
        Button searchButton = new Button("Search");
        searchButton.getStyleClass().add("secondary-button");
        searchButton.setOnAction(event -> refresh());
        Button clearButton = new Button("Clear");
        clearButton.getStyleClass().add("secondary-button");
        clearButton.setOnAction(event -> {
            searchField.clear();
            refresh();
        });
        HBox searchRow = new HBox(8, searchField, searchButton, clearButton);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        table.getColumns().add(column("Name", Customer::getFullName, 200));
        table.getColumns().add(column("Email", Customer::getEmail, 260));
        table.getColumns().add(column("Phone", Customer::getPhone, 160));
        table.getColumns().add(column("License no.", Customer::getLicenseNumber, 140));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No customers found."));
        table.setRowFactory(tableView -> {
            TableRow<Customer> row = new TableRow<>();
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

        getChildren().addAll(header, searchRow, table, actions);
    }

    /** Reloads the table, applying the search text if there is one. */
    public void refresh() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim();
        List<Customer> customers = query.isEmpty()
                ? repository.findAllByOrderByLastNameAscFirstNameAsc()
                : repository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByLastNameAscFirstNameAsc(
                        query, query, query);
        table.setItems(FXCollections.observableArrayList(customers));
    }

    private TableColumn<Customer, String> column(String title, Function<Customer, String> getter, double width) {
        TableColumn<Customer, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(getter.apply(cell.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    private void openEditor(Customer existing) {
        boolean isNew = (existing == null);

        TextField firstName = new TextField(isNew ? "" : existing.getFirstName());
        TextField lastName = new TextField(isNew ? "" : existing.getLastName());
        TextField email = new TextField(isNew ? "" : existing.getEmail());
        email.setPromptText("name@example.com");
        TextField phone = new TextField(isNew ? "" : existing.getPhone());
        phone.setPromptText("+36 30 123 4567");
        TextField license = new TextField(isNew ? "" : existing.getLicenseNumber());

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 10, 0, 10));
        grid.addRow(0, new Label("First name:"), firstName);
        grid.addRow(1, new Label("Last name:"), lastName);
        grid.addRow(2, new Label("Email:"), email);
        grid.addRow(3, new Label("Phone:"), phone);
        grid.addRow(4, new Label("License number:"), license);
        firstName.setPrefWidth(300);

        Label errors = new Label();
        errors.setStyle("-fx-text-fill: #dc3545;");
        errors.setWrapText(true);
        errors.setMaxWidth(420);
        VBox content = new VBox(12, grid, errors);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "New customer" : "Edit customer");
        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(content);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            Customer candidate = buildCustomer(isNew ? null : existing.getId(),
                    firstName, lastName, email, phone, license);
            List<String> problems = validate(candidate);
            if (!problems.isEmpty()) {
                errors.setText(String.join("\n", problems));
                event.consume(); // keep the dialog open
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveType) {
            Customer customer = buildCustomer(isNew ? null : existing.getId(),
                    firstName, lastName, email, phone, license);
            repository.save(customer);
            refresh();
        }
    }

    private Customer buildCustomer(Long id, TextField firstName, TextField lastName,
                                   TextField email, TextField phone, TextField license) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setFirstName(firstName.getText().trim());
        customer.setLastName(lastName.getText().trim());
        customer.setEmail(email.getText().trim().toLowerCase());
        customer.setPhone(phone.getText().trim());
        customer.setLicenseNumber(license.getText().trim().toUpperCase());
        return customer;
    }

    /** Uses the same validation rules as the web version (annotations on Customer) + uniqueness checks. */
    private List<String> validate(Customer customer) {
        List<ConstraintViolation<Customer>> violations = new ArrayList<>(validator.validate(customer));
        violations.sort(Comparator.comparingInt(v -> FIELD_ORDER.indexOf(v.getPropertyPath().toString())));

        List<String> problems = new ArrayList<>();
        for (ConstraintViolation<Customer> violation : violations) {
            problems.add("• " + violation.getMessage());
        }

        Long id = customer.getId();
        if (!customer.getEmail().isEmpty()) {
            boolean emailTaken = (id == null)
                    ? repository.existsByEmailIgnoreCase(customer.getEmail())
                    : repository.existsByEmailIgnoreCaseAndIdNot(customer.getEmail(), id);
            if (emailTaken) {
                problems.add("• A customer with this email already exists.");
            }
        }
        if (!customer.getLicenseNumber().isEmpty()) {
            boolean licenseTaken = (id == null)
                    ? repository.existsByLicenseNumberIgnoreCase(customer.getLicenseNumber())
                    : repository.existsByLicenseNumberIgnoreCaseAndIdNot(customer.getLicenseNumber(), id);
            if (licenseTaken) {
                problems.add("• A customer with this license number already exists.");
            }
        }
        return problems;
    }

    private void delete(Customer customer) {
        if (customer == null || !Dialogs.confirm("Are you sure you want to delete " + customer.getFullName() + "?")) {
            return;
        }
        try {
            repository.deleteById(customer.getId());
            refresh();
        } catch (DataIntegrityViolationException e) {
            Dialogs.error("This customer cannot be deleted because they have rentals.");
        }
    }
}
