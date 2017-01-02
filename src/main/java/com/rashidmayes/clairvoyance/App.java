package com.rashidmayes.clairvoyance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.Info;
import com.aerospike.client.cluster.Node;
import com.aerospike.client.policy.ClientPolicy;
import com.rashidmayes.clairvoyance.util.MapHelper;

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
	
	
	public static NodeInfo getNodeInfo(Node node) {
		NodeInfo nodeInfo = new NodeInfo();
		
		Map<String,String> details =  Info.request(null,node);
		nodeInfo.nodeId = details.get("node");
		nodeInfo.build = details.get("build");
		nodeInfo.edition = details.get("edition");
		nodeInfo.version = details.get("version");
		nodeInfo.statistics = MapHelper.map(details.get("statistics"), ";");
		nodeInfo.address = node.getAddress().toString();
		nodeInfo.host = node.getHost().toString();
		nodeInfo.name = node.getName();
		
		
		return nodeInfo;
	}
		
		
	public static List<NamespaceInfo> getNamespaceInfo(Node node) {	

		List<NamespaceInfo> namespaces = new ArrayList<NamespaceInfo>();
		NamespaceInfo namespaceInfo;

		for ( String namespace : StringUtils.split(Info.request(null, node, "namespaces"), ";") ) {			
			namespaceInfo = new NamespaceInfo();
			namespaceInfo.name = namespace;
			namespaceInfo.properties = MapHelper.map(Info.request(null, node, "namespace/" + namespace), ";");	
			namespaces.add(namespaceInfo);
		}
		
		return namespaces;
	}
	
	
	public static NamespaceInfo getNamespaceInfo(Node node, String namespace) {	

		List<NamespaceInfo> namespaces = new ArrayList<NamespaceInfo>();
		NamespaceInfo namespaceInfo = new NamespaceInfo();
		namespaceInfo.name = namespace;
		namespaceInfo.properties = MapHelper.map(Info.request(null, node, "namespace/" + namespace), ";");	
		namespaces.add(namespaceInfo);
		
		return namespaceInfo;
	}
	
	public static List<SetInfo> getSetInfo(Node node, String namespace) {
		
		List<SetInfo> sets = new ArrayList<SetInfo>();
		SetInfo setInfo;
		
		Map<String, String> map;
		for ( String set : StringUtils.split(Info.request(null, node, "sets/" + namespace), ";") ) {
			map = MapHelper.map(set, ":");
			setInfo = new SetInfo();
			setInfo.properties = map;
			setInfo.bytesMemory = MapHelper.getLong(map,0,"n-bytes-memory");
			setInfo.name = map.get("set_name");
			setInfo.namespace = map.get("ns_name");
			setInfo.objectCount = MapHelper.getLong(map,0,"n_objects");

			sets.add(setInfo);
		}
		
		return sets;
	}
	
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
