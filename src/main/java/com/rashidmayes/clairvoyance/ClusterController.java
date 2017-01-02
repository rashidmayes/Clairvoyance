package com.rashidmayes.clairvoyance;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

public class ClusterController extends Service<String> implements EventHandler<WorkerStateEvent> {
		
    @FXML private WebView webView;
    @FXML private TabPane tabs;
    
	private ObjectMapper mObjectMapper = new ObjectMapper();
	private ObjectWriter mObjectWriter;

    private NodeInfo mNodeInfo;
    private Thread mThread;

    public ClusterController() {
    }

    @FXML
    public void initialize() {
    	
    	mObjectMapper.setSerializationInclusion(Include.NON_NULL);
    	mObjectWriter = mObjectMapper.writerWithDefaultPrettyPrinter();
    	setOnSucceeded(this);
    	
    	mNodeInfo = (NodeInfo) webView.getUserData();
    	
    	try {
			String html = Template.getText("/cluster.html");
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
		        									if ( !ClusterController.this.isRunning() ) {
		        										ClusterController.this.restart();
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
    	    	//hide
    	    } else {
    	    	//show
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
        			//System.out.println(json);
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
            	mNodeInfo = (NodeInfo) webView.getUserData();
        		if ( mNodeInfo != null ) {
        			
            	   	try {    	   		
                		AerospikeClient client = App.getClient();

                		List<NodeInfo> list = new ArrayList<NodeInfo>();
                		List<NamespaceInfo> namespaces;
                		NodeInfo nodeInfo;
                		for ( Node node : client.getNodes() ) {
                			nodeInfo = App.getNodeInfo(node);
                			list.add(nodeInfo);
                			
                			namespaces = App.getNamespaceInfo(node);
                			nodeInfo.namespaces = namespaces.toArray(new NamespaceInfo[namespaces.size()]);
                		}
                        return mObjectWriter.writeValueAsString(list);
                		
                	} catch (Exception e) {
                		App.APP_LOGGER.severe(e.getMessage());
                	}
        		}
            	
        		return null;
            }
        };
    }
}
