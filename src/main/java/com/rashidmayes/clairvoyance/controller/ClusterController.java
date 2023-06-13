package com.rashidmayes.clairvoyance.controller;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.cluster.Node;
import com.rashidmayes.clairvoyance.ClairvoyanceFxApplication;
import com.rashidmayes.clairvoyance.model.ApplicationModel;
import com.rashidmayes.clairvoyance.model.NodeInfoMapper;
import com.rashidmayes.clairvoyance.util.ClairvoyanceLogger;
import com.rashidmayes.clairvoyance.util.ClairvoyanceObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;

public class ClusterController {

    @FXML
    private TextArea dumpTextArea;

    private final NodeInfoMapper nodeInfoMapper;

    public ClusterController() {
        this.nodeInfoMapper = new NodeInfoMapper();
    }

    @FXML
    public void initialize() {
        ApplicationModel.INSTANCE.runInBackground(() -> {
            ClairvoyanceLogger.logger.info("starting cluster dump");
            try {
                AerospikeClient client = ClairvoyanceFxApplication.getClient();
                var stringBuilder = new StringBuilder();
                for (Node node : client.getNodes()) {
                    var nodeInfo = nodeInfoMapper.getNodeInfo(node);
                    var nodeDump = ClairvoyanceObjectMapper.objectWriter.writeValueAsString(nodeInfo);
                    stringBuilder.append(nodeDump);
                    stringBuilder.append("\n");
                }
                var dump = stringBuilder.toString();
                Platform.runLater(() -> {
                    dumpTextArea.appendText(dump);
                });
                ClairvoyanceLogger.logger.info("cluster dump completed");
            } catch (Exception e) {
                ClairvoyanceLogger.logger.error(e.getMessage(), e);
                new Alert(Alert.AlertType.ERROR, "there was an error while performing cluster dump - see logs for details")
                        .showAndWait();
            }
        });
    }

}
