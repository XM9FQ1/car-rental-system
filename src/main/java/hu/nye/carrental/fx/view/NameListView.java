package hu.nye.carrental.fx.view;

import hu.nye.carrental.fx.Dialogs;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

/**
 * Shared screen for simple lists that only have a name (Brands, Categories):
 * table + New / Edit / Delete. Subclasses only say how to load and save the data.
 */
public abstract class NameListView<T> extends VBox {

    private final String entityName;
    private final TableView<T> table = new TableView<>();

    protected NameListView(String title, String entityName) {
        this.entityName = entityName;
        getStyleClass().add("page");

        Label heading = new Label(title);
        heading.getStyleClass().add("page-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button addButton = new Button("+ New " + entityName.toLowerCase());
        addButton.getStyleClass().add("primary-button");
        addButton.setOnAction(event -> openEditor(null));
        HBox header = new HBox(12, heading, spacer, addButton);
        header.setAlignment(Pos.CENTER_LEFT);

        TableColumn<T, String> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(idOf(cell.getValue()))));
        idColumn.setPrefWidth(80);
        idColumn.setMaxWidth(120);
        TableColumn<T, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(cell -> new SimpleStringProperty(nameOf(cell.getValue())));
        table.getColumns().add(idColumn);
        table.getColumns().add(nameColumn);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No " + title.toLowerCase() + " yet."));
        table.setRowFactory(tableView -> {
            TableRow<T> row = new TableRow<>();
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

        getChildren().addAll(header, table, actions);
    }

    /** Reloads the table from the database. */
    public void refresh() {
        table.setItems(FXCollections.observableArrayList(findAll()));
    }

    private void openEditor(T existing) {
        boolean isNew = (existing == null);
        TextInputDialog dialog = new TextInputDialog(isNew ? "" : nameOf(existing));
        dialog.setTitle((isNew ? "New " : "Edit ") + entityName.toLowerCase());
        dialog.setHeaderText(null);
        dialog.setContentText("Name:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }
        String name = result.get().trim();

        if (name.isEmpty()) {
            Dialogs.error(entityName + " name is required.");
            return;
        }
        if (name.length() > 50) {
            Dialogs.error(entityName + " name can be at most 50 characters.");
            return;
        }
        Long excludeId = isNew ? null : idOf(existing);
        if (nameExists(name, excludeId)) {
            Dialogs.error("A " + entityName.toLowerCase() + " with this name already exists.");
            return;
        }

        T entity = isNew ? newEntity() : existing;
        setName(entity, name);
        save(entity);
        refresh();
    }

    private void delete(T item) {
        if (item == null || !Dialogs.confirm("Are you sure you want to delete \"" + nameOf(item) + "\"?")) {
            return;
        }
        try {
            deleteById(idOf(item));
            refresh();
        } catch (DataIntegrityViolationException e) {
            Dialogs.error("This " + entityName.toLowerCase()
                    + " cannot be deleted because it is used by one or more cars.");
        }
    }

    protected abstract List<T> findAll();

    protected abstract T newEntity();

    protected abstract Long idOf(T item);

    protected abstract String nameOf(T item);

    protected abstract void setName(T item, String name);

    protected abstract boolean nameExists(String name, Long excludeId);

    protected abstract void save(T item);

    protected abstract void deleteById(Long id);
}
