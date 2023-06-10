package com.rashidmayes.clairvoyance.controller;

import com.aerospike.client.AerospikeClient;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.rashidmayes.clairvoyance.ClairvoyanceFxApplication;
import com.rashidmayes.clairvoyance.model.ApplicationModel;
import com.rashidmayes.clairvoyance.model.NodeInfoMapper;
import com.rashidmayes.clairvoyance.util.ClairvoyanceLogger;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;

import java.util.logging.Level;
import java.util.stream.Stream;

public class NamespaceController {

    @FXML
    private TextArea namespaceTextArea;

    private final ObjectWriter objectWriter;
    private final NodeInfoMapper nodeInfoMapper = new NodeInfoMapper();

    public NamespaceController() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(Include.NON_NULL);
        objectWriter = objectMapper.writerWithDefaultPrettyPrinter();
    }

    @FXML
    public void initialize() {
        // TODO: 10/06/2023 as for now it only gets first namespace and dumps info
        // TODO: 10/06/2023 it should get namespace id and perform dump for it
        ApplicationModel.INSTANCE.runInBackground(() -> {
            ClairvoyanceLogger.logger.info("starting fetching namespace info");
            try {
                AerospikeClient client = ClairvoyanceFxApplication.getClient();
                var namespaceResult = Stream.of(client.getNodes())
                        .map(nodeInfoMapper::getNodeInfo)
                        .flatMap(nodeInfo -> nodeInfo.getNamespaces().stream())
                        //.filter(namespaceInfo -> namespaceInfo.getName())
                        .findFirst()
                        .map(this::json)
                        .orElse("no data");
                Platform.runLater(() -> {
                    namespaceTextArea.appendText(namespaceResult);
                });
                ClairvoyanceLogger.logger.info("fetching namespace info completed");
            } catch (Exception e) {
                ClairvoyanceLogger.logger.log(Level.SEVERE, e.getMessage(), e);
                new Alert(Alert.AlertType.ERROR, "there was an error while performing cluster dump - see logs for details")
                        .showAndWait();
            }
        });
    }

    private String json(Object object) {
        try {
            return objectWriter.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
