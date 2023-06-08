package com.rashidmayes.clairvoyance.controller;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.rashidmayes.clairvoyance.App;
import com.rashidmayes.clairvoyance.util.Template;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class AboutController {

    @FXML
    private WebView webView;
    @FXML
    private TabPane tabs;

    private ObjectMapper mObjectMapper = new ObjectMapper();
    private ObjectWriter mObjectWriter;

    public AboutController() {
        mObjectMapper.setSerializationInclusion(Include.NON_NULL);
        mObjectWriter = mObjectMapper.writerWithDefaultPrettyPrinter();
    }

    @FXML
    public void initialize() {

        try {
            String html = Template.getText("/about.html");
            WebEngine engine = webView.getEngine();

            engine.getLoadWorker().stateProperty().addListener(
                    new ChangeListener<State>() {
                        @Override
                        public void changed(ObservableValue<? extends State> ov, State oldState, State newState) {
                            if (newState == Worker.State.SUCCEEDED) {
                                Platform.runLater(new Runnable() {
                                    public void run() {

                                    }
                                });
                            }
                        }
                    });

            engine.loadContent(html);
        } catch (Exception e) {
            App.APP_LOGGER.severe(e.getMessage());
        }
    }
}
