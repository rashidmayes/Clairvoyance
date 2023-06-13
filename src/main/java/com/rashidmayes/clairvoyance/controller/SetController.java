package com.rashidmayes.clairvoyance.controller;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.listener.RecordSequenceListener;
import com.aerospike.client.policy.Priority;
import com.aerospike.client.policy.ScanPolicy;
import com.rashidmayes.clairvoyance.ClairvoyanceFxApplication;
import com.rashidmayes.clairvoyance.NoSQLCellFactory;
import com.rashidmayes.clairvoyance.model.ApplicationModel;
import com.rashidmayes.clairvoyance.model.RecordRow;
import com.rashidmayes.clairvoyance.model.SetInfo;
import com.rashidmayes.clairvoyance.util.ClairvoyanceLogger;
import com.rashidmayes.clairvoyance.util.ClairvoyanceObjectMapper;
import com.rashidmayes.clairvoyance.util.FileUtil;
import gnu.crypto.util.Base64;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class SetController {

    private static final ExecutorService SET_EXECUTOR = new ThreadPoolExecutor(
            2, 4, 5, TimeUnit.MINUTES, new LinkedBlockingQueue<>(),
            runnable -> {
                var thread = Executors.defaultThreadFactory().newThread(runnable);
                thread.setName("set-controller-thread-" + thread.threadId());
                thread.setDaemon(true);
                return thread;
            }
    );

    private final List<SavableKey> IN_MEMORY_FILE_MOCK = Collections.synchronizedList(new ArrayList<>());

    @FXML
    private GridPane rootPane;
    @FXML
    private ListView<Integer> pages;
    @FXML
    private TextArea recordDetails;
    @FXML
    private TableView<RecordRow> dataTable;

    // safety net to prevent from too much of disk space consumption
    private static final int MAX_RECORDS_FETCH = 1_000_000;

    // used to control in memory buffer and saving keys to file batch size
    private static final int MAX_RECORDS_IN_MEMORY_BUFFER_SIZE = 100;

    // todo: should create a tmp file for 10_000 records and another one for records exceeding this value and so on
    private static final int MAX_RECORDS_TMP_FILE_SIZE = 10_000;

    private final List<RecordRow> buffer = Collections.synchronizedList(new LinkedList<>());

    private final AtomicInteger recordCounter = new AtomicInteger(0);

    private volatile boolean actionCancelled;

    private final AtomicReference<File> tmpRootDir = new AtomicReference<>();

    public SetController() {
    }

    @FXML
    public void initialize() {
        // TODO: 12/06/2023 have to manually remove listeners at the end
        //  because of risk of memory leaks
        rootPane.sceneProperty()
                .addListener(loadTableChangeListener());
        dataTable.getSelectionModel()
                .selectedItemProperty()
                .addListener(rowClickedAction());
        pages.setCellFactory(listView -> new PageRootCell());
        pages.getSelectionModel()
                .selectedItemProperty()
                .addListener(selectPageClickedAction());
    }

    @FXML
    public void cancelAction(ActionEvent actionEvent) {
        ClairvoyanceLogger.logger.info("canceling scan if any active");
        actionCancelled = true;
    }

    @FXML
    public void refreshAction(ActionEvent event) {
        var setInfo = (SetInfo) rootPane.getUserData();
        ClairvoyanceLogger.logger.info("refreshing set: {}", setInfo.getId());
        resetActionCancelledFlag();
        buffer.clear();
        ClairvoyanceLogger.logger.info("buffer cleared");
        recordCounter.set(0);
        ClairvoyanceLogger.logger.info("record counter reset");
        IN_MEMORY_FILE_MOCK.clear();
        ClairvoyanceLogger.logger.info("IN_MEMORY_FILE_MOCK cleared");
        scanSetAction(setInfo).run();
    }

    @FXML
    public void clearAction(ActionEvent actionEvent) {
        Platform.runLater((this::clearTable));
    }

    @Getter
    @RequiredArgsConstructor
    public static class SavableKey {

        private final int index;
        private final byte[] digest;

    }

    private void batchProcessRecordsFromScan(List<RecordRow> records) {
        ClairvoyanceLogger.logger.info("batch processing records from scan...");
        var keysBuffer = new LinkedList<SavableKey>();

//        var fileNumber = recordCounter.get() / MAX_RECORDS_TMP_FILE_SIZE;
//        var file = new File(tmpRootDir, fileNumber + ".data");

        for (var record : records) {
            var recordIndex = recordCounter.getAndIncrement();
            record.setIndex(recordIndex);

            if (recordIndex >= MAX_RECORDS_FETCH) {
                throw new AerospikeException.ScanTerminated(new IllegalStateException("too many records! >>safety net<<"));
            }

            // put X first records into the buffer, for all records put their keys into file
            if (buffer.size() < MAX_RECORDS_IN_MEMORY_BUFFER_SIZE) {
                buffer.add(record);
                ClairvoyanceLogger.logger.info("record added to buffer (key {})", record.getKey());
            } else {
                // todo: maybe we can already refresh the view showing first page while next pages are loading
            }
            keysBuffer.add(new SavableKey(recordIndex, record.getKey().digest));
            ClairvoyanceLogger.logger.info("record added to keys buffer (key {})", record.getKey());
        }

        Platform.runLater(() -> {
            var counter = recordCounter.get();
            var page = counter / 100;
            ClairvoyanceLogger.logger.info("counted page {}", page);
            var exists = new HashSet<>(pages.getItems())
                    .contains(page);
            if (!exists) {
                pages.getItems().add(page);
                ClairvoyanceLogger.logger.info("added new page {}", page);
            }
        });

        IN_MEMORY_FILE_MOCK.addAll(keysBuffer);
        ClairvoyanceLogger.logger.info("added keys to in memory file mock");

        // todo: put keysBuffer to file
//        getTmpFileForIndex(recordCounter.get());

        // TODO: 12/06/2023 add page number to list if we have more than one

        // todo: delete tmp file on jvm exit
//    } finally {
//            try {
//                FileUtils.forceDeleteOnExit(file);
//            } catch (IOException e) {
//                ClairvoyanceLogger.logger.error(e.getMessage());
//            }
//        }
    }

    private void getRowsForKeys(List<SavableKey> keys, String namespace, String set) {
        var result = ApplicationModel.INSTANCE.getAerospikeClient();
        if (result.hasError()) {
            // TODO: 13/06/2023 best idea?
            throw new IllegalStateException(result.getError());
        }
        if (keys == null || keys.isEmpty()) {
            return;
        }

        var indexes = new HashMap<byte[], Integer>();

        SET_EXECUTOR.execute(() -> {
            Key[] keysArray = keys.stream()
                    .peek(key -> indexes.put(key.getDigest(), key.getIndex()))
                    .map(key -> new Key(namespace, key.getDigest(), set, null))
                    .toArray(Key[]::new);
            var client = result.getData();
            client.get(
                    null,
                    new RecordSequenceListener() {
                        @Override
                        public void onRecord(Key key, Record record) throws AerospikeException {
                            if (actionCancelled) {
                                throw new AerospikeException.ScanTerminated();
                            }
                            if (buffer.size() < MAX_RECORDS_IN_MEMORY_BUFFER_SIZE) {
                                var recordRow = new RecordRow(key, record);
                                recordRow.setIndex(indexes.getOrDefault(key.digest, -1));
                                buffer.add(recordRow);
                                ClairvoyanceLogger.logger.info("record added to buffer (key {})", key);
                            } else {
                                ClairvoyanceLogger.logger.warn("cannot fetch record with key {} - no space in buffer", key);
                            }
                        }

                        @Override
                        public void onSuccess() {
                            displayRecordsFromBuffer();
                        }

                        @Override
                        public void onFailure(AerospikeException exception) {
                            ClairvoyanceLogger.logger.error(exception.getMessage(), exception);
                        }
                    },
                    keysArray);
        });
    }

    private File getTmpFileForIndex(int index) {
        var fileNumber = index / MAX_RECORDS_TMP_FILE_SIZE;
        return new File(tmpRootDir.get(), fileNumber + ".data");
    }

    private List<SavableKey> readSavedKeys(int fromIndex, int toIndex) {
        ClairvoyanceLogger.logger.info("reading saved keys from in memory mock");
        if (fromIndex > IN_MEMORY_FILE_MOCK.size() - 1) {
            return List.of();
        }
        var result = new LinkedList<SavableKey>();
        for (int i = fromIndex; i < toIndex; i++) {
            if (i < IN_MEMORY_FILE_MOCK.size()) {
                result.add(IN_MEMORY_FILE_MOCK.get(i));
            }
        }
        ClairvoyanceLogger.logger.info("returning saved keys from in memory mock");
        return result;
    }

    private void loadPage(int pageNumber) {
        ClairvoyanceLogger.logger.info("load page requested for {}", pageNumber);
        buffer.clear();
        ClairvoyanceLogger.logger.info("buffer cleared");

        int startIndex = pageNumber * 100;
        int endIndex = startIndex + 100;
        var info = (SetInfo) rootPane.getUserData();

        ClairvoyanceLogger.logger.info("startIndex {}, endIndex {}", startIndex, endIndex);
        SET_EXECUTOR.execute(() -> {
            var keys = readSavedKeys(startIndex, endIndex);
            getRowsForKeys(keys, info.getNamespace(), info.getName());
        });
    }

    private void displayRecordsFromBuffer() {
        Platform.runLater(() -> {
            try {
                ClairvoyanceLogger.logger.info("displaying records from buffer");
                dataTable.getItems().clear();
                ClairvoyanceLogger.logger.info("data table cleared");

                var processedColumns = dataTable.getColumns()
                        .stream()
                        .map(TableColumnBase::getText)
                        .collect(Collectors.toSet());
                for (var recordRow : buffer) {
                    // create columns that do not exist in the table
                    var recordColumns = recordRow.getRecord().bins.keySet();
                    for (var recordColumn : recordColumns) {
                        if (!processedColumns.contains(recordColumn)) {
                            processedColumns.add(recordColumn);

                            var column = new TableColumn<RecordRow, String>(recordColumn);
                            column.setMinWidth(200);
                            column.setCellValueFactory(new NoSQLCellFactory(recordColumn));

                            dataTable.getColumns().add(column);
                            ClairvoyanceLogger.logger.info("column {} added to the table", column.getText());
                        }
                    }
                    // add record to the table
                    dataTable.getItems().add(recordRow);
                }
            } catch (Exception e) {
                ClairvoyanceLogger.logger.error("error when displaying data from memory");
                ClairvoyanceLogger.logger.error(e.getMessage(), e);
            }
        });
    }

    private ChangeListener<? super Scene> loadTableChangeListener() {
        return (observable, oldScene, newScene) -> {
            ClairvoyanceLogger.logger.info("clearing table");
            clearTable();

            if (newScene == null) {
                ClairvoyanceLogger.logger.info("stopping scan, clearing stuff");
                //stop scan
                //clean files
                actionCancelled = true;
                if (tmpRootDir.get() != null) {
                    ApplicationModel.INSTANCE.runInBackground(() -> {
                        // delete tmp directory
                        FileUtils.deleteQuietly(tmpRootDir.get());
                    });
                }
            } else {
                var info = (SetInfo) rootPane.getUserData();
                ClairvoyanceLogger.logger.info("fetching set {}", info.getName());

                // TODO: 12/06/2023 move it somewhere?
                // create index column in table
                var indexColumn = createIndexColumn();
                dataTable.getColumns().add(indexColumn);

                // create digest column in table
                var digestColumn = createDigestColumn();
                dataTable.getColumns().add(digestColumn);

                SET_EXECUTOR.execute(scanSetAction(info));
            }
        };
    }

    private Runnable scanSetAction(SetInfo info) {
        return () -> {
            try {
                createTmpDirectory(info);

                var client = ClairvoyanceFxApplication.getClient();
                var buffer = Collections.synchronizedList(new LinkedList<RecordRow>());

                client.scanAll(
                        createScanPolicy(),
                        new RecordSequenceListener() {

                            @Override
                            public void onRecord(Key key, Record record) throws AerospikeException {
                                onScanRecordReceived(key, record, buffer);
                            }

                            @Override
                            public void onSuccess() {
                                onScanAllSuccess(buffer, info);
                            }

                            @Override
                            public void onFailure(AerospikeException exception) {
                                ClairvoyanceLogger.logger.info(exception.getMessage());
                            }
                        },
                        info.getNamespace(),
                        info.getName()
                );
                ClairvoyanceLogger.logger.info("submitted scan all request");
            } catch (AerospikeException.ScanTerminated exception) {
                ClairvoyanceLogger.logger.info(exception.getMessage());
            } catch (Exception exception) {
                ClairvoyanceLogger.logger.error(exception.getMessage(), exception);
            }
        };
    }

    private void onScanRecordReceived(Key key, Record record, List<RecordRow> buffer) {
        ClairvoyanceLogger.logger.info("record received for key {}", key);
        if (actionCancelled) {
            throw new AerospikeException.ScanTerminated();
        }
        if (buffer.size() < MAX_RECORDS_IN_MEMORY_BUFFER_SIZE) {
            buffer.add(new RecordRow(key, record));
        } else {
            batchProcessRecordsFromScan(buffer);
            buffer.clear();
            ClairvoyanceLogger.logger.info("buffer cleared");
        }
    }

    private void onScanAllSuccess(List<RecordRow> internalBuffer, SetInfo mSetInfo) {
        ClairvoyanceLogger.logger.info("scanned all records");
        batchProcessRecordsFromScan(internalBuffer);
        internalBuffer.clear();
        ClairvoyanceLogger.logger.info("{} scans complete", mSetInfo.getName());

        displayRecordsFromBuffer();
    }

    private ScanPolicy createScanPolicy() {
        var scanPolicy = new ScanPolicy();
        scanPolicy.priority = Priority.LOW;
        return scanPolicy;
    }

    private void createTmpDirectory(SetInfo mSetInfo) {
//        var rootDir = tmpRootDir.get();

        var rootDir = new File(System.getProperty("java.io.tmpdir"));
        rootDir = new File(rootDir, "clairvoyance");
        rootDir = new File(rootDir, FileUtil.prettyFileName(mSetInfo.getNamespace()));
        rootDir = new File(rootDir, FileUtil.prettyFileName(mSetInfo.getName()));
        var result = rootDir.mkdirs();
        ClairvoyanceLogger.logger.info("created tmp directory for set {} -> {}", mSetInfo.getName(), result);

        tmpRootDir.set(rootDir);
    }

    static class PageRootCell extends ListCell<Integer> {

        @Override
        public void updateItem(Integer item, boolean empty) {
            super.updateItem(item, empty);
            if (item == null) {
                setText(null);
            } else {
                setText(String.valueOf(item));
            }
        }

    }

    private ChangeListener<? super RecordRow> rowClickedAction() {
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

    private ChangeListener<? super Integer> selectPageClickedAction() {
        return (observable, oldPageNumber, newPageNumber) -> {
            ClairvoyanceLogger.logger.info("some page selected");
            resetActionCancelledFlag();
            if (newPageNumber != null) {
                try {
                    loadPage(newPageNumber);
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

    private void clearTable() {
        resetActionCancelledFlag();
        buffer.clear();
        ClairvoyanceLogger.logger.info("buffer cleared");
        dataTable.getItems().clear();
        ClairvoyanceLogger.logger.info("data table cleared");
    }

    private void resetActionCancelledFlag() {
        ClairvoyanceLogger.logger.info("resetting actionCancelled flag");
        if (actionCancelled) {
            actionCancelled = false;
        }
    }

}
