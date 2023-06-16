package com.rashidmayes.clairvoyance.controller;

import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.cluster.Node;
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
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.util.stream.Stream;

public class ClusterController {

    @FXML
    private WebView clusterWebView;

    private final NodeInfoMapper nodeInfoMapper;

    public ClusterController() {
        this.nodeInfoMapper = new NodeInfoMapper();
    }

    @FXML
    public void initialize() {
        ApplicationModel.INSTANCE.runInBackground(() -> {
            ClairvoyanceLogger.logger.info("starting cluster dump");
            try {
                var client = ClairvoyanceFxApplication.getClient();
                var json = getClusterInfoJson(client);
                renderResult(json);
                ClairvoyanceLogger.logger.info("cluster dump completed");
            } catch (Exception e) {
                ClairvoyanceLogger.logger.error(e.getMessage(), e);
                ClairvoyanceFxApplication.displayAlert("there was an error while performing cluster dump - see logs for details");
            }
        });
    }

    private void renderResult(String json) {
        Platform.runLater(() -> {
            var webEngine = clusterWebView.getEngine();
            webEngine.loadContent(getHtml());
            webEngine.getLoadWorker()
                    .stateProperty()
                    .addListener(getStateChangeListener(json, webEngine));
        });
    }

    private ChangeListener<Worker.State> getStateChangeListener(String json, WebEngine webEngine) {
        return (ov, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                webEngine.executeScript("update(" + json + ")");
            }
        };
    }

    private String getClusterInfoJson(IAerospikeClient client) {
        var stringBuilder = new StringBuilder("[");
        for (Node node : client.getNodes()) {
            var nodeInfo = nodeInfoMapper.getNodeInfo(node);
            var nodeDump = json(nodeInfo);
            stringBuilder.append(nodeDump);
            stringBuilder.append("\n");
        }
        return stringBuilder.append("]").toString();
    }

    private String getHtml() {
        try (var inputStream = getClass().getClassLoader().getResourceAsStream("html/cluster.html")) {
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
