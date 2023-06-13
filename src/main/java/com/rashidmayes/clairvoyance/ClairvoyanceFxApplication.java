package com.rashidmayes.clairvoyance;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.async.AsyncClient;
import com.rashidmayes.clairvoyance.model.ApplicationModel;
import com.rashidmayes.clairvoyance.util.ClairvoyanceLogger;
import com.rashidmayes.clairvoyance.util.FileUtil;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class ClairvoyanceFxApplication extends Application {

    public static final Preferences PREFERENCES = Preferences.userNodeForPackage(ClairvoyanceFxApplication.class);

    public static AsyncClient getClient() throws AerospikeException {
        var aerospikeClientResult = ApplicationModel.INSTANCE.getAerospikeClient();
        if (aerospikeClientResult.hasError()) {
            // TODO: 12/06/2023 not sure if this can be run from any thread - probably only from FX thread
            new Alert(Alert.AlertType.ERROR, aerospikeClientResult.getError())
                    .showAndWait();
            throw new AerospikeException(aerospikeClientResult.getError());
        }
        return aerospikeClientResult.getData();
    }

    public static void main(String[] args) {
        Application.launch(ClairvoyanceFxApplication.class, args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        ClairvoyanceLogger.logger.info("starting clairvoyance...");

        stage.setOnCloseRequest(onCloseEventHandler());

        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
        stage.setTitle("Clairvoyance");
        stage.setX(primaryScreenBounds.getMinX());
        stage.setY(primaryScreenBounds.getMinY());
        stage.setWidth(primaryScreenBounds.getWidth());
        stage.setHeight(primaryScreenBounds.getHeight());
        stage.getIcons().add(new Image(getClass().getClassLoader().getResourceAsStream("icon.png")));

        Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("connect.fxml"));
        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.show();
    }

    private EventHandler<WindowEvent> onCloseEventHandler() {
        return event -> {
            try {
                PREFERENCES.sync();
                FileUtil.clearCache();
            } catch (BackingStoreException e) {
                ClairvoyanceLogger.logger.error(e.getMessage(), e);
            }
            System.exit(0);
        };
    }

}
