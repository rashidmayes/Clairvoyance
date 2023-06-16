package com.rashidmayes.clairvoyance.controller;

import com.aerospike.client.IAerospikeClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.rashidmayes.clairvoyance.ClairvoyanceFxApplication;
import com.rashidmayes.clairvoyance.model.ApplicationModel;
import com.rashidmayes.clairvoyance.model.NamespaceInfo;
import com.rashidmayes.clairvoyance.model.NodeInfoMapper;
import com.rashidmayes.clairvoyance.util.ClairvoyanceLogger;
import com.rashidmayes.clairvoyance.util.ClairvoyanceObjectMapper;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.util.stream.Stream;

public class NamespaceController {

    @FXML
    public WebView namespaceWebView;

    private final NodeInfoMapper nodeInfoMapper = new NodeInfoMapper();

    public NamespaceController() {}

    @FXML
    public void initialize() {
        ApplicationModel.INSTANCE.runInBackground(() -> {
            ClairvoyanceLogger.logger.info("starting fetching namespace info");
            try {
                var client = ClairvoyanceFxApplication.getClient();
                var namespaceInfo = (NamespaceInfo) namespaceWebView.getUserData();
                var namespaceResult = getNamespaceInfoJson(client, namespaceInfo);
                renderResult(namespaceResult);
                ClairvoyanceLogger.logger.info("fetching namespace info completed");
            } catch (Exception e) {
                ClairvoyanceLogger.logger.error(e.getMessage(), e);
                ClairvoyanceFxApplication.displayAlert("there was an error while performing cluster dump - see logs for details");
            }
        });
    }

    private void renderResult(String namespaceResult) {
        Platform.runLater(() -> {
            var webEngine = namespaceWebView.getEngine();
            webEngine.loadContent(getHtml());
            webEngine.getLoadWorker()
                    .stateProperty()
                    .addListener(getStateChangeListener(namespaceResult, webEngine));
        });
    }

    private ChangeListener<Worker.State> getStateChangeListener(String namespaceResult, WebEngine webEngine) {
        return (ov, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                webEngine.executeScript("update(" + namespaceResult + ")");
            }
        };
    }

    private String getNamespaceInfoJson(IAerospikeClient client, NamespaceInfo namespaceInfo) {
        return Stream.of(client.getNodes())
                .map(nodeInfoMapper::getNodeInfo)
                .flatMap(nodeInfo -> nodeInfo.getNamespaces().stream())
                .filter(info -> info.getName().equals(namespaceInfo.getName()))
                .findFirst()
                .map(this::json)
                .orElse("{}");
    }

    private String getHtml() {
        try (var inputStream = getClass().getClassLoader().getResourceAsStream("html/namespace.html")) {
            return new String(inputStream.readAllBytes());
        } catch (Exception exception) {
            ClairvoyanceLogger.logger.error(exception.getMessage(), exception);
        }
        return "";
    }

    private String json(Object object) {
        try {
            return ClairvoyanceObjectMapper.objectWriter.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
