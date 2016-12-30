package com.rashidmayes.clairvoyance;

import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.policy.ClientPolicy;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class App extends Application
{
	public static final Logger APP_LOGGER = Logger.getLogger("app");
	private static AerospikeClient client = null;
	protected static String host = null;
	protected static int port;
	private static String username = null;
	private static String password = null;
	
	public static void setConnectionInfo(String host, int port, String username, String password) {
		App.host = host;
		App.password = password;
		App.username = username;
		App.port = port;
	}
	
	public static AerospikeClient getClient() throws AerospikeException {
		if ( client == null || !client.isConnected() ) {
			
        	if ( StringUtils.isBlank(username) || StringUtils.isBlank(password) ) {
        		
        		ClientPolicy policy = new ClientPolicy();
        		policy.user = username;
        		policy.password = password;
        		client = new AerospikeClient(policy, host, port);
        	} else {
        		client = new AerospikeClient(host, port);
        	}
			
			client.writePolicyDefault.timeout = 4000;
			client.readPolicyDefault.timeout = 4000;
			client.queryPolicyDefault.timeout = 4000;
		}
		
		return client;
	}
	
    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
    
    @Override
    public void start(Stage stage) throws Exception {
    	/*
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent e) {
               Platform.exit();
               System.exit(0);
            }
         });*/
    	
    	
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
        stage.setTitle("Clairvoyance");
        stage.setX(primaryScreenBounds.getMinX());
        stage.setY(primaryScreenBounds.getMinY());
        stage.setWidth(primaryScreenBounds.getWidth());
        stage.setHeight(primaryScreenBounds.getHeight());
        
        Parent root = FXMLLoader.load(getClass().getResource("connect.fxml"));
        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.show();
    }
}
