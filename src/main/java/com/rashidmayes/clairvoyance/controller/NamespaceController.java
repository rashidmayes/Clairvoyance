package com.rashidmayes.clairvoyance.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.rashidmayes.clairvoyance.ClairvoyanceFxApplication;
import com.rashidmayes.clairvoyance.model.ApplicationModel;
import com.rashidmayes.clairvoyance.model.NamespaceInfo;
import com.rashidmayes.clairvoyance.model.NodeInfoMapper;
import com.rashidmayes.clairvoyance.util.ClairvoyanceLogger;
import com.rashidmayes.clairvoyance.util.ClairvoyanceObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;

import java.util.stream.Stream;

public class NamespaceController {

    @FXML
    private GridPane namespaceGridPane;

    @FXML
    private TextArea namespaceTextArea;

    private final NodeInfoMapper nodeInfoMapper = new NodeInfoMapper();

    public NamespaceController() {}

    @FXML
    public void initialize() {
        ApplicationModel.INSTANCE.runInBackground(() -> {
            ClairvoyanceLogger.logger.info("starting fetching namespace info");
            try {
                var client = ClairvoyanceFxApplication.getClient();
                var namespaceInfo = (NamespaceInfo) namespaceGridPane.getUserData();
                var namespaceResult = Stream.of(client.getNodes())
                        .map(nodeInfoMapper::getNodeInfo)
                        .flatMap(nodeInfo -> nodeInfo.getNamespaces().stream())
                        .filter(info -> info.getName().equals(namespaceInfo.getName()))
                        .findFirst()
                        .map(this::json)
                        .orElse("no data");
                Platform.runLater(() -> {
                    namespaceTextArea.appendText(namespaceResult);
                });
                ClairvoyanceLogger.logger.info("fetching namespace info completed");
            } catch (Exception e) {
                ClairvoyanceLogger.logger.error(e.getMessage(), e);
                new Alert(Alert.AlertType.ERROR, "there was an error while performing cluster dump - see logs for details")
                        .showAndWait();
            }
        });
    }

    private String json(Object object) {
        try {
            return ClairvoyanceObjectMapper.objectWriter.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
