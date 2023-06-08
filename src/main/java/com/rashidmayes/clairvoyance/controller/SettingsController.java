package com.rashidmayes.clairvoyance.controller;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.rashidmayes.clairvoyance.App;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import javafx.scene.web.WebView;

public class SettingsController {

    @FXML
    private WebView webView;
    @FXML
    private TabPane tabs;

    private ObjectMapper mObjectMapper = new ObjectMapper();
    private ObjectWriter mObjectWriter;

    public SettingsController() {
        mObjectMapper.setSerializationInclusion(Include.NON_NULL);
        mObjectWriter = mObjectMapper.writerWithDefaultPrettyPrinter();
    }

    @FXML
    public void initialize() {
    }

    @FXML
    protected void handleAction(ActionEvent event) {
        App.APP_LOGGER.info(event.toString());
    }

}
