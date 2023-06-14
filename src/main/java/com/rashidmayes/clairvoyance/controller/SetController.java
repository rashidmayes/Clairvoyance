package com.rashidmayes.clairvoyance.controller;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.IAerospikeClient;
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
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class SetController {

    @FXML
    private GridPane rootPane;
    @FXML
    public GridPane paginationGrid;
    @FXML
    private TextArea recordDetails;

    private static final int ROWS_PER_PAGE = 20;
    // safety net to prevent from too much of memory consumption
    private static final int MAX_RECORDS_FETCH = 100_000;

    private final ChangeListener<? super Scene> loadTableChangeListener = loadTableChangeListener();
    private final ChangeListener<? super RecordRow> rowClickedListener = rowClickedListener();

    private final TableView<RecordRow> dataTable = new TableView<>(FXCollections.observableArrayList());
    private final List<RecordRow> buffer = Collections.synchronizedList(new LinkedList<>());

    private volatile boolean actionCancelled;
    private final AtomicReference<SetScanner> scannerReference = new AtomicReference<>();

    public SetController() {}

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
            actionCancelled = false;
            Platform.runLater(() -> {
                dataTable.getColumns().clear();
                dataTable.getItems().clear();
                if (!paginationGrid.getChildren().isEmpty() && paginationGrid.getChildren().get(0).getClass() == Pagination.class) {
                    System.err.println("removing pagination");
                    paginationGrid.getChildren().remove(0);
                }
                recordDetails.setText(null);
            });
            var scanner = scannerReference.get();
            scanner.scan();
        });
    }

    @FXML
    public void cancelAction(ActionEvent event) {
        event.consume();
        ClairvoyanceLogger.logger.info("canceling scan if any active");
        actionCancelled = true;
        scannerReference.get().cancelScan();
    }

    private Callback<Integer, Node> createPage() {
        return index -> {
            ClairvoyanceLogger.logger.info("page index {}", index);
            int fromIndex = index * ROWS_PER_PAGE;
            int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, buffer.size());
            if (fromIndex > toIndex) {
                return null;
            }
            var page = buffer.subList(fromIndex, toIndex);
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
                scannerReference.set(new SetScanner(
                        info.getNamespace(), info.getName(), MAX_RECORDS_FETCH, getAerospikeClient(),
                        new SetScanner.ScanCallbacks() {

                            @Override
                            public void scanSuccessCallback(List<RecordRow> buffer) {
                                Platform.runLater(() -> updateViewFromBuffer(buffer));
                            }

                            @Override
                            public void scanTerminated(List<RecordRow> buffer) {
                                Platform.runLater(() -> updateViewFromBuffer(buffer));
                            }
                        }
                ));
                ClairvoyanceLogger.logger.info("fetching set {}", info.getName());

                ApplicationModel.INSTANCE.runInBackground(() -> {
                    scannerReference.get().scan();
                });
            }
        };
    }

    private List<RecordRow> blockingScanSet(SetInfo info) {
        var client = getAerospikeClient();
        var result = new LinkedList<RecordRow>();
        var index = new AtomicInteger();

        client.scanAll(null, info.getNamespace(), info.getName(),
                (key, record) -> {
                    if (actionCancelled || result.size() > MAX_RECORDS_FETCH) {
                        ClairvoyanceLogger.logger.error(
                                "scan terminated (actionCancelled? {}, max records exceeded? {})",
                                actionCancelled, result.size() > MAX_RECORDS_FETCH
                        );
                        throw new AerospikeException.ScanTerminated();
                    }
                    var recordRow = new RecordRow(key, record);
                    recordRow.setIndex(index.getAndIncrement());
                    result.add(recordRow);
                }
        );
        ClairvoyanceLogger.logger.info("scan completed");
        return result;
    }

    private IAerospikeClient getAerospikeClient() {
        var clientResult = ApplicationModel.INSTANCE.getAerospikeClient();
        if (clientResult.hasError()) {
            throw new AerospikeException(clientResult.getError());
        }
        return clientResult.getData();
    }

    private void updateViewFromBuffer(List<RecordRow> result) {
        var indexColumn = createIndexColumn();
        dataTable.getColumns().add(indexColumn);

        var digestColumn = createDigestColumn();
        dataTable.getColumns().add(digestColumn);

        var columns = getColumnsNames();
        var columnsToAdd = new HashSet<String>();

        for (RecordRow recordRow : result) {
            var recordBins = recordRow.getRecord().bins.keySet();
            for (String recordBin : recordBins) {
                if (!columns.contains(recordBin)) {
                    columns.add(recordBin);
                    columnsToAdd.add(recordBin);
                }
            }
            buffer.add(recordRow);
        }

        dataTable.getColumns().addAll(createDataColumns(columnsToAdd));
        dataTable.getItems().addAll(result);

        var pagination = new Pagination((result.size() / ROWS_PER_PAGE) + 1, 0);
        pagination.setPageFactory(createPage());
        pagination.prefWidthProperty().bind(paginationGrid.widthProperty());
        pagination.prefHeightProperty().bind(paginationGrid.heightProperty());

        paginationGrid.getChildren().add(0, pagination);
    }

    private Set<TableColumn<RecordRow, String>> createDataColumns(HashSet<String> columnsToAdd) {
        return columnsToAdd.stream()
                .map(text -> {
                    var column = new TableColumn<RecordRow, String>(text);
                    column.setMinWidth(200);
                    column.setCellValueFactory(new NoSQLCellFactory(text));
                    return column;
                })
                .collect(Collectors.toSet());
    }

    public Set<String> getColumnsNames() {
        return dataTable.getColumns()
                .stream()
                .map(TableColumnBase::getText)
                .collect(Collectors.toSet());
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
        indexColumn.setCellValueFactory(param -> {
            RecordRow recordRow = param.getValue();
            if (recordRow != null) {
                return new SimpleIntegerProperty(recordRow.getIndex());
            }
            return new SimpleIntegerProperty(0);
        });
        return indexColumn;
    }

    private static TableColumn<RecordRow, String> createDigestColumn() {
        var digestColumn = new TableColumn<RecordRow, String>("Digest");
        digestColumn.setMinWidth(200);
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
