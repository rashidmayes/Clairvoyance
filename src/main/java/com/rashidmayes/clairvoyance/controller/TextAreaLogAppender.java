package com.rashidmayes.clairvoyance.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import javafx.scene.control.TextArea;

public class TextAreaLogAppender extends AppenderBase<ILoggingEvent> {

    private final TextArea textArea;

    TextAreaLogAppender(TextArea textArea) {
        this.textArea = textArea;
    }

    @Override
    protected void append(ILoggingEvent loggingEvent) {
        if (loggingEvent.getLevel().isGreaterOrEqual(Level.INFO)) {
            var callerData = loggingEvent.getCallerData()[0];
            var debug = callerData.getClassName() + "$" + callerData.getMethodName();
            String text = debug + " " + "$> " + loggingEvent.getMessage() + "\n";
            if (this.textArea != null) {
                textArea.appendText(text);
                // todo: if there is no space for new logs - remove oldest part of text in the console
            }
        }
    }

}
