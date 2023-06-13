package com.rashidmayes.clairvoyance.controller;

import com.rashidmayes.clairvoyance.model.ApplicationModel;
import com.rashidmayes.clairvoyance.ClairvoyanceFxApplication;
import com.rashidmayes.clairvoyance.util.ClairvoyanceLogger;
import com.rashidmayes.clairvoyance.model.ConnectionInfo;
import com.rashidmayes.clairvoyance.util.Result;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Objects;

public class ConnectController {

    @FXML
    private TextField hostField;
    @FXML
    private TextField portField;
    @FXML
    private TextField usernameField;
    @FXML
    private TextField passwordField;
    @FXML
    private Button connectButton;
    @FXML
    private Button connectAlternateButton;

    @FXML
    public void initialize() {
        hostField.setText(ClairvoyanceFxApplication.PREFERENCES.get("last.host", null));
        portField.setText(ClairvoyanceFxApplication.PREFERENCES.get("last.port", "3000"));
    }

    @FXML
    protected void handleConnectAction(ActionEvent event) {
        var connectionInfoResult = getConnectionInfo(event.getSource() == connectAlternateButton);
        if (connectionInfoResult.hasError()) {
            new Alert(Alert.AlertType.ERROR, connectionInfoResult.getError())
                    .showAndWait();
            return;
        }
        ApplicationModel.INSTANCE.setConnectionInfo(connectionInfoResult.getData());
        var aerospikeClientResult = ApplicationModel.INSTANCE.createNewAerospikeClient();
        if (aerospikeClientResult.hasError()) {
            ClairvoyanceLogger.logger.warn("could not connect to cluster: {}", aerospikeClientResult.getError());
            new Alert(Alert.AlertType.ERROR, "could not connect to cluster")
                    .showAndWait();
            return;
        }
        try {
            URL resource = getClass().getClassLoader().getResource("browser.fxml");
            Objects.requireNonNull(resource, "browser.fxml is null");
            Parent root = FXMLLoader.load(resource);
            Scene scene = new Scene(root);

            Stage stage = (Stage) (this.hostField.getScene().getWindow());
            stage.setScene(scene);
            //stage.centerOnScreen();

            ClairvoyanceFxApplication.PREFERENCES.put("last.host", connectionInfoResult.getData().host());
            ClairvoyanceFxApplication.PREFERENCES.putInt("last.port", connectionInfoResult.getData().port());

        } catch (Exception e) {
            ClairvoyanceLogger.logger.error(e.getMessage(), e);
            Alert alert = new Alert(AlertType.ERROR, String.format("Error connecting: %s", e.getMessage()));
            alert.showAndWait();
        }
    }

    private Result<ConnectionInfo, String> getConnectionInfo(boolean useServiceAlternate) {
        var hostResult = getHost();
        var portResult = getPort();
        var username = this.usernameField.getText();
        var password = this.passwordField.getText();

        if (hostResult.hasError()) {
            return Result.error(hostResult.getError());
        }
        if (portResult.hasError()) {
            return Result.error(portResult.getError());
        }
        return Result.of(new ConnectionInfo(
                hostResult.getData(),
                portResult.getData(),
                username,
                password,
                useServiceAlternate
        ));
    }

    private Result<String, String> getHost() {
        var host = this.hostField.getText();
        if (host == null || host.isBlank()) {
            return Result.error("host cannot be empty");
        }
        return Result.of(host);
    }

    private Result<Integer, String> getPort() {
        var portValue = this.portField.getText();
        if (portValue == null || portValue.isBlank()) {
            return Result.error("port cannot be empty");
        }
        try {
            var port = Integer.parseInt(portValue);
            return Result.of(port);
        } catch (NumberFormatException exception) {
            return Result.error("invalid port");
        }
    }

}
