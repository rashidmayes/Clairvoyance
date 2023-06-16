package com.rashidmayes.clairvoyance.controller;

import com.aerospike.client.Key;
import com.rashidmayes.clairvoyance.NoSQLCellFactory;
import com.rashidmayes.clairvoyance.SetScanner;
import com.rashidmayes.clairvoyance.model.ApplicationModel;
import com.rashidmayes.clairvoyance.model.RecordRow;
import com.rashidmayes.clairvoyance.model.SetInfo;
import com.rashidmayes.clairvoyance.util.ClairvoyanceLogger;
import com.rashidmayes.clairvoyance.util.ClairvoyanceObjectMapper;
import gnu.crypto.util.Base64;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class SetController {

    @FXML
    public TextField searchKeyField;
    @FXML
    private GridPane rootPane;
    @FXML
    public GridPane paginationGrid;
    @FXML
    private TextArea recordDetails;

    private static final int ROWS_PER_PAGE = 20;

    private final ChangeListener<? super Scene> loadTableChangeListener = loadTableChangeListener();
    private final ChangeListener<? super RecordRow> rowClickedListener = rowClickedListener();

    private final TableView<RecordRow> dataTable = new TableView<>(FXCollections.observableArrayList());
    private final List<RecordRow> buffer = Collections.synchronizedList(new LinkedList<>());
    private final RecordSearcher recordSearcher = new RecordSearcher();

    private final AtomicReference<SetScanner> scannerReference = new AtomicReference<>();
    private final AtomicReference<SetInfo> setInfoReference = new AtomicReference<>();

    public SetController() {
    }

    @FXML
    public void initialize() {
        rootPane.sceneProperty()
                .addListener(loadTableChangeListener);
        dataTable.getSelectionModel()
                .selectedItemProperty()
                .addListener(rowClickedListener);
    }

    @FXML
    public void refreshAction(ActionEvent event) {
        event.consume();
        ApplicationModel.INSTANCE.runInBackground(() -> {
            buffer.clear();
            Platform.runLater(() -> {
                dataTable.getColumns().clear();
                dataTable.getItems().clear();
                removePagination();
                recordDetails.setText(null);
                searchKeyField.setText("");
                recordSearcher.reset();
                createLoader();
            });
            var scanner = scannerReference.get();
            scanner.scan();
        });
    }

    @FXML
    public void cancelAction(ActionEvent event) {
        event.consume();
        ClairvoyanceLogger.logger.info("canceling scan if any active");
        scannerReference.get().cancelScan();
    }

    @FXML
    public void searchByKey(ActionEvent event) {
        event.consume();
        var set = setInfoReference.get();
        var keyText = searchKeyField.getText();

        if (keyText == null || keyText.isBlank()) {
            recordSearcher.reset();
            Platform.runLater(() -> updateViewFromBuffer(buffer));
        } else {
            var key = new Key(set.getNamespace(), set.getName(), keyText);
            recordSearcher.search(key, buffer);
            Platform.runLater(() -> updateViewFromBuffer(recordSearcher.getSearchResult()));
        }
    }

    private Callback<Integer, Node> createPage() {
        return index -> {
            var list = recordSearcher.isActiveSearch() ? recordSearcher.getSearchResult() : buffer;
            if (index > list.size() - 1) {
                index = 0;
            }
            ClairvoyanceLogger.logger.info("page index {}", index);
            int fromIndex = index * ROWS_PER_PAGE;
            int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, list.size());
            if (fromIndex > toIndex) {
                return null;
            }
            var page = list.subList(fromIndex, toIndex);
            dataTable.setItems(FXCollections.observableArrayList(page));
            return dataTable;
        };
    }

    private ChangeListener<? super Scene> loadTableChangeListener() {
        return (observable, oldScene, newScene) -> {
            if (newScene == null) {
                // user closes the tab
                rootPane.sceneProperty().removeListener(loadTableChangeListener);
                dataTable.getSelectionModel().selectedItemProperty().removeListener(rowClickedListener);
            }
            if (newScene != null) {
                // user opens the tab
                var info = (SetInfo) rootPane.getUserData();
                setInfoReference.set(info);
                scannerReference.set(createScanner(info));
                ClairvoyanceLogger.logger.info("fetching set {}", info.getName());

                Platform.runLater(this::createLoader);

                ApplicationModel.INSTANCE.runInBackground(() -> {
                    scannerReference.get().scan();
                });
            }
        };
    }

    private SetScanner createScanner(SetInfo info) {
        return new SetScanner(
                info.getNamespace(),
                info.getName(),
                new SetScanner.ScanCallbacks() {

                    @Override
                    public void scanSuccessCallback(List<RecordRow> buffer) {
                        scanDoneCallback(buffer);
                    }

                    @Override
                    public void scanTerminated(List<RecordRow> buffer) {
                        scanDoneCallback(buffer);
                    }
                }
        );
    }

    private void scanDoneCallback(List<RecordRow> buffer) {
        this.buffer.clear();
        this.buffer.addAll(buffer);
        Platform.runLater(() -> updateViewFromBuffer(this.buffer));
    }

    private void updateViewFromBuffer(List<RecordRow> buffer) {
        var columnsToAdd = new LinkedList<String>();

        for (var recordRow : buffer) {
            var recordBins = recordRow.getRecord().bins.keySet();
            for (String recordBin : recordBins) {
                if (!columnsToAdd.contains(recordBin)) {
                    columnsToAdd.add(recordBin);
                }
            }
        }

        var c = new LinkedList<TableColumn<RecordRow, ?>>();
        c.add(createIndexColumn());
        c.add(createDigestColumn());
        c.addAll(createDataColumns(columnsToAdd));


        dataTable.getColumns().setAll(c);
        dataTable.getItems().setAll(buffer);

        createPagination(buffer);
    }

    private void createPagination(List<RecordRow> buffer) {
        var pagination = createPaginationComponent(buffer);
        var textField = new TextField();
        textField.setPromptText("jump to page");
        textField.setMaxWidth(90);

        textField.setOnKeyPressed(ke -> {
            if (ke.getCode().equals(KeyCode.ENTER)) {
                textField.setText(textField.getText().trim());
                textField.positionCaret(textField.getText().length());
                setCurrentPageFromInput(pagination, textField);
            }
        });

        var button = new Button("Go");
        button.setOnAction(event -> setCurrentPageFromInput(pagination, textField));

        var hbox = new HBox(5);
        hbox.setAlignment(Pos.CENTER);
        hbox.getChildren().add(textField);
        hbox.getChildren().add(button);

        var vbox = new VBox(5.0);
        vbox.setPadding(new Insets(5));
        vbox.setAlignment(Pos.CENTER);
        vbox.setId("pagination-box");
        vbox.getChildren().add(pagination);
        vbox.getChildren().add(hbox);

        paginationGrid.getChildren().clear();
        paginationGrid.getChildren().add(vbox);
    }

    private void setCurrentPageFromInput(Pagination pagination, TextField textField) {
        var index = textField.getText();
        var page = 0;
        try {
            page = Integer.parseInt(index);
        } catch (NumberFormatException exception) {
            ClairvoyanceLogger.logger.warn("incorrect integer value");
        }
        pagination.setCurrentPageIndex(page - 1);
    }

    private Pagination createPaginationComponent(List<RecordRow> buffer) {
        var pagination = new Pagination((buffer.size() / ROWS_PER_PAGE) + 1, 0);
        pagination.setPageFactory(createPage());
        pagination.prefWidthProperty().bind(paginationGrid.widthProperty());
        pagination.prefHeightProperty().bind(paginationGrid.heightProperty());
        return pagination;
    }

    private void removePagination() {
        if (!paginationGrid.getChildren().isEmpty() && paginationGrid.getChildren().get(0).getId().equals("pagination-box")) {
            paginationGrid.getChildren().clear();
        }
    }

    private List<TableColumn<RecordRow, String>> createDataColumns(List<String> columnsToAdd) {
        return columnsToAdd.stream()
                .map(text -> {
                    var column = new TableColumn<RecordRow, String>(text);
                    column.setMinWidth(200);
                    column.setCellValueFactory(new NoSQLCellFactory(text));
                    return column;
                })
                .toList();
    }

    private ChangeListener<? super RecordRow> rowClickedListener() {
        return (observable, oldRow, newRow) -> {
            ClairvoyanceLogger.logger.info("displaying details of record");
            if (newRow != null) {
                try {
                    var rowDetails = ClairvoyanceObjectMapper.objectWriter.writeValueAsString(newRow);
                    recordDetails.setText(rowDetails);
                } catch (Exception exception) {
                    ClairvoyanceLogger.logger.error(exception.getMessage(), exception);
                }
            }
        };
    }

    private static TableColumn<RecordRow, Number> createIndexColumn() {
        var indexColumn = new TableColumn<RecordRow, Number>("#");
        indexColumn.setMinWidth(50);
        indexColumn.setCellValueFactory(param -> {
            RecordRow recordRow = param.getValue();
            if (recordRow != null) {
                return new SimpleIntegerProperty(recordRow.getIndex());
            }
            return new SimpleIntegerProperty(0);
        });
        return indexColumn;
    }

    private void createLoader() {
        var vbox = new VBox();
        vbox.setAlignment(Pos.CENTER);
        var hbox = new HBox();
        hbox.setAlignment(Pos.CENTER);

        var iconImage = getClass().getClassLoader().getResourceAsStream("images/spinner.gif");
        Objects.requireNonNull(iconImage, "ic_touch.png is missing");
        var loaderGif = new ImageView(new Image(iconImage));

        hbox.getChildren().add(loaderGif);
        vbox.getChildren().add(hbox);

        vbox.prefWidthProperty().bind(paginationGrid.widthProperty());
        vbox.prefHeightProperty().bind(paginationGrid.heightProperty());

        paginationGrid.getChildren().add(vbox);
    }

    private static TableColumn<RecordRow, String> createDigestColumn() {
        var digestColumn = new TableColumn<RecordRow, String>("Digest");
        digestColumn.setMinWidth(220);
        digestColumn.setCellValueFactory(param -> {
            RecordRow recordRow = param.getValue();
            if (recordRow != null) {
                return new SimpleStringProperty(Base64.encode(recordRow.getKey().digest));
            }
            return new SimpleStringProperty("");
        });
        return digestColumn;
    }

}
