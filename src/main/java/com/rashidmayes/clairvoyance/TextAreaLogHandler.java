package com.rashidmayes.clairvoyance;

import javafx.scene.control.TextArea;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class TextAreaLogHandler extends Handler {

    private final TextArea textArea;
    private final int maxLength;

    TextAreaLogHandler(TextArea textArea, int maxLength) {
        this.maxLength = maxLength;
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
                int currentLength = textArea.getLength();
                if (currentLength > maxLength) {
                    int end = (textArea.getLength() - maxLength);
                    if (end > 0) {
                        //mTextArea.replaceText(0, end , "");
                    }
                }
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
