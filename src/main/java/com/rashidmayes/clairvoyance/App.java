package com.rashidmayes.clairvoyance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.apache.commons.lang.StringUtils;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Info;
import com.aerospike.client.async.AsyncClient;
import com.aerospike.client.async.AsyncClientPolicy;
import com.aerospike.client.cluster.Node;
import com.rashidmayes.clairvoyance.util.MapHelper;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class App extends Application
{
	public static final Logger APP_LOGGER = Logger.getLogger("app");
	static final ThreadGroup SCANS = new ThreadGroup("scans");
	static final ThreadGroup LOADS = new ThreadGroup("loads");
	static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3, new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        }
    });
	static final Preferences Config =  Preferences.userNodeForPackage(App.class);
	
	private static AsyncClient client = null;
	protected static String host = null;
	protected static int port;
	protected static boolean useServicesAlternate;
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
			setInfo.bytesMemory = MapHelper.getLong(map,0,"n-bytes-memory", "memory_data_bytes");
			setInfo.name = MapHelper.getString(map,"","set_name","set");
			setInfo.namespace =  MapHelper.getString(map, "", "ns_name", "ns");
			setInfo.objectCount = MapHelper.getLong(map,0,"n_objects", "objects");

			sets.add(setInfo);
		}
		
		return sets;
	}
	
	public static void setConnectionInfo(String host, int port, String username, String password, boolean useServicesAlternate) {
		App.host = host;
		App.password = password;
		App.username = username;
		App.port = port;
		App.useServicesAlternate = useServicesAlternate;
	}
	
	public static AsyncClient getClient() throws AerospikeException {
		if ( client == null || !client.isConnected() ) {
			
    		AsyncClientPolicy policy = new AsyncClientPolicy();
    		policy.useServicesAlternate = App.useServicesAlternate;
			
        	if ( StringUtils.isBlank(username) || StringUtils.isBlank(password) ) {
        		policy.user = username;
        		policy.password = password;
        		client = new AsyncClient(policy, host, port);
        	} else {
        		client = new AsyncClient(host, port);
        	}
        	
        	
        	client = new AsyncClient(policy, host, port);
        	/*String[] commands =  new String[] {"services", "services-alternate"};
        	HashMap<String,String> infoMap = Info.request(client.getNodes()[0].getConnection(1000), commands );
        	System.out.println(infoMap);*/
        	
			
			client.writePolicyDefault.timeout = 4000;
			client.readPolicyDefault.timeout = 4000;
			client.queryPolicyDefault.timeout = Integer.MAX_VALUE;
		}
		
		return client;
	}
	
    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
    
    @Override
    public void start(Stage stage) throws Exception {
    	
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
               try {
            	   Config.sync();
				} catch (BackingStoreException e) {
					e.printStackTrace();
				}
               System.exit(0);
            }
         });
    	
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
        stage.setTitle("Clairvoyance");
        stage.setX(primaryScreenBounds.getMinX());
        stage.setY(primaryScreenBounds.getMinY());
        stage.setWidth(primaryScreenBounds.getWidth());
        stage.setHeight(primaryScreenBounds.getHeight());
        stage.getIcons().add(new Image(App.class.getResourceAsStream("icon.png"))); 
        
        Parent root = FXMLLoader.load(getClass().getResource("connect.fxml"));
        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.show();
    }
}
