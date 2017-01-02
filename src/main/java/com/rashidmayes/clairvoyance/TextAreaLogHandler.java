package com.rashidmayes.clairvoyance;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import javafx.scene.control.TextArea;

public class TextAreaLogHandler extends Handler {
	
	private TextArea mTextArea;
	private int mMaxLength;
	
	TextAreaLogHandler(TextArea textArea, int maxLength) {
		this.mMaxLength = maxLength;
		this.mTextArea = textArea;
		
		this.setFormatter(new SimpleFormatter());
	}
	
	String messageFormat = "%s: %s";
	@Override
	public void publish(LogRecord record) {
		if ( this.isLoggable(record) ) {
			String text = record.getMessage() +"\n";
			mTextArea.appendText(text);
			int currentLength = mTextArea.getLength();
			if ( currentLength > mMaxLength ) {
				int end = (mTextArea.getLength() - mMaxLength);
				if ( end > 0 ) {
					//mTextArea.replaceText(0, end , "");
				}
			}
			/*
			if ( !mTextArea.isFocused() && !mTextArea.isHover() && mTextArea.getSelection().getLength() == 0 ) {
				mTextArea.end();
			}*/
		}
	}

	@Override
	public void flush() {
		
	}

	@Override
	public void close() throws SecurityException {
		
	}
}
