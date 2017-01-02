package com.rashidmayes.clairvoyance;

import java.util.logging.Level;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.cluster.Node;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.rashidmayes.clairvoyance.util.Template;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class NamespaceController extends Service<String> implements EventHandler<WorkerStateEvent> {
		
    @FXML private WebView webView;
    @FXML private TabPane tabs;
    
	private ObjectMapper mObjectMapper = new ObjectMapper();
	private ObjectWriter mObjectWriter;

    private NamespaceInfo mNamespaceInfo;
    private Thread mThread;

    public NamespaceController() {
    }

    @FXML
    public void initialize() {
    	mObjectMapper.setSerializationInclusion(Include.NON_NULL);
    	mObjectWriter = mObjectMapper.writerWithDefaultPrettyPrinter();
    	
    	setOnSucceeded(this);
    	
    	mNamespaceInfo = (NamespaceInfo) webView.getUserData();
    	
    	try {
			String html = Template.getText("/namespace.html");
			WebEngine engine = webView.getEngine();
			
			engine.getLoadWorker().stateProperty().addListener(
					new ChangeListener<State>() {
						@Override public void changed(ObservableValue<? extends State> ov, State oldState, State newState) {
							if (newState == Worker.State.SUCCEEDED) {
				    	    	mThread = new Thread(new Runnable() {
				    	    		
									public void run() {
										while ( mThread != null && webView.getScene() != null ) {
											Platform.runLater(new Runnable() {
												public void run() {
													if ( !NamespaceController.this.isRunning() ) {
														NamespaceController.this.restart();
													}
												}
											});
											
											try {
												Thread.sleep(30*1000);
											} catch (Exception e) {
												
											}
										}
									}
				    	    		
				    	    	}); 	
								
		            	    	mThread.setDaemon(true);
		            	    	mThread.start();
							}
	                    }
	                });
			
			
			engine.loadContent(html);
		} catch (Exception e) {
			App.APP_LOGGER.severe(e.getMessage());
		}
    	
    	webView.sceneProperty().addListener((obs, oldScene, newScene) -> {
    	    if (newScene == null) {

    	    } else {

    	    }
    	});
     }

    
    @FXML protected void handleAction(ActionEvent event) {
    	App.APP_LOGGER.info(event.toString());
    }
    
    
    @Override
    public void handle(WorkerStateEvent event) {
        String json = String.valueOf(event.getSource().getValue());
        if ( json != null ) {
    		WebEngine engine = webView.getEngine();
    		if ( engine.getDocument() != null ) {
        		try {
        			engine.executeScript("update(" + json + ")");
        		} catch (Exception e) {
        			App.APP_LOGGER.log(Level.WARNING, e.getMessage(), e);
        		}	
    		}
        }
    }
    
    protected Task<String> createTask() {

        return new Task<String>() {
            protected String call() {
            	
        		mNamespaceInfo = (NamespaceInfo) webView.getUserData();
        		if ( mNamespaceInfo != null ) {
        			
            	   	try {    	   		
                		AerospikeClient client = App.getClient();
                		Node node = client.getNodes()[0];
                		
                        //get latest from server
                        mNamespaceInfo = App.getNamespaceInfo(node, mNamespaceInfo.name);
                        return mObjectWriter.writeValueAsString(mNamespaceInfo);
                		
                	} catch (Exception e) {
                		App.APP_LOGGER.severe(e.getMessage());
                	}
        		}
            	
        		return null;
            }
        };
    }
}
