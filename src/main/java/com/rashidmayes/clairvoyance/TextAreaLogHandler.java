package com.rashidmayes.clairvoyance;

import javafx.scene.control.TextArea;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class TextAreaLogHandler extends Handler {

    private final TextArea textArea;

    TextAreaLogHandler(TextArea textArea) {
        this.textArea = textArea;
        this.setFormatter(new SimpleFormatter());
    }

    @Override
    public void publish(LogRecord record) {
        if (this.isLoggable(record)) {
            var debug = record.getSourceClassName() + "$" + record.getSourceMethodName();
            String text = debug + " " + "$> " + record.getMessage() + "\n";
            if(this.textArea != null) {
                textArea.appendText(text);
                // todo: if there is no space for new logs - remove oldest part of text in the console
            }
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }

}
