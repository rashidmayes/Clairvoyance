package com.rashidmayes.clairvoyance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.text.TableView;

import org.apache.commons.lang.StringUtils;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Info;
import com.aerospike.client.cluster.Node;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.rashidmayes.clairvoyance.util.FileUtil;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class Browser implements Runnable {
	
	private static final String NAMESPACE_LINE_FORMAT = "%-10s %10s %2d %16d %16d %16d %16d %8s %8s %8d%% %8d%%\n";
	private static final String NAMESPACE_HEADINGS = String.format("%-10s %10s %2s %16s %16s %16s %16s %8s %8s %9s %9s\n"
			,"Name", "Type", "RF", "Master", "Prole", "Used Memory", "Used Disk", "Memory", "Disk", "Free Mem", "Free Disk");
	private static final String SET_LINE_FORMAT = "%-16s %-16s %16d %16s\n";
	private static final String SET_HEADINGS = String.format("%-16s %-16s %16s %16s\n"
			,"Set", "Namespace", "Objects", "Bytes Memory");
	
	
    private final ImageView rootIcon = new ImageView(new Image(getClass().getResourceAsStream("ic_cluster.png")));
    private final Image namespaceIcon = new Image(getClass().getResourceAsStream("ic_namespace.png"));
    private final Image setIcon = new Image(getClass().getResourceAsStream("ic_set.png"));
	
    @FXML private TextArea console;
    @FXML private TreeView<SimpleTreeNode> namespacesTree;
    @FXML private TableView dataTable;
    
    private TextAreaLogHandler mTextAreaLogHandler;
	private ObjectMapper mObjectMapper = new ObjectMapper();
	private ObjectWriter mObjectWriter = mObjectMapper.writerWithDefaultPrettyPrinter();
    
    private Thread mThread;
    private boolean cancel = false;

    public Browser() {
    }

    @FXML
    public void initialize() {
    	//set up console
    	mTextAreaLogHandler = new TextAreaLogHandler(console,5*1024);
    	App.APP_LOGGER.addHandler(mTextAreaLogHandler);
    	
    	AerospikeClient client = App.getClient();
    	SimpleTreeNode root = new SimpleTreeNode();
    	root.displayName = App.host + ":" + App.port;
    	root.value = client;
        TreeItem<SimpleTreeNode> rootItem = new TreeItem<SimpleTreeNode> (root, rootIcon);
        rootItem.setExpanded(true);
        
        namespacesTree.setRoot(rootItem);
    	
    	mThread = new Thread(this);
    	mThread.setDaemon(true);
    	mThread.start();
     }
    
    @FXML protected void handleAction(ActionEvent event) {
    	
    	
    }
    
    public void run() {
    	
    	while (!cancel) {
    	
    	   	try {    	   		
        		AerospikeClient client = App.getClient();
        		
				try {
					// tree update
					TreeItem<SimpleTreeNode> root = namespacesTree.getRoot();

					// list namespaces
					Node node = client.getNodes()[0];
					List<NamespaceInfo> namespaces = getNamespaceInfo(node);

					List<SetInfo> sets;
					TreeItem<SimpleTreeNode> namespaceNode;
					TreeItem<SimpleTreeNode> setNode;
					for (NamespaceInfo namespace : namespaces) {

						namespaceNode = null;
						for (TreeItem<SimpleTreeNode> tempNode : root.getChildren()) {
							if (tempNode.getValue().displayName.equals(namespace.name)) {
								namespaceNode = tempNode;
								namespaceNode.getValue().value = namespace;
								break;
							}
						}

						if (namespaceNode == null) {
							SimpleTreeNode s = new SimpleTreeNode();
							s.displayName = namespace.name;
							s.value = namespace;
							namespaceNode = new TreeItem<SimpleTreeNode>(s, new ImageView(namespaceIcon));
							root.getChildren().add(namespaceNode);
						}

						sets = getSetInfo(node, namespace.name);
						for ( SetInfo setInfo : sets ) {
							
							setNode = null;
							for (TreeItem<SimpleTreeNode> tempNode : namespaceNode.getChildren()) {
								if (tempNode.getValue().displayName.equals(setInfo.name)) {
									setNode = tempNode;
									setNode.getValue().value = setInfo;
									break;
								}
							}
							
							if (setNode == null) {
								SimpleTreeNode s = new SimpleTreeNode();
								s.displayName = setInfo.name;
								s.value = setInfo;
								setNode = new TreeItem<SimpleTreeNode>(s, new ImageView(setIcon));
								namespaceNode.getChildren().add(setNode);
							}	
						}
					}

				} catch (Exception e) {
					App.APP_LOGGER.severe(e.getMessage());
				}


    	   		
    	   		///debugout

    			try {
            		StringBuffer buffer = new StringBuffer();
            		//list namespaces
            		List<NamespaceInfo> namespaces;
        			for (Node node : client.getNodes() ) {

        				namespaces = getNamespaceInfo(node);
        				buffer.append("\n").append(node.getHost())
        				.append("\n\n");
        				
        				buffer.append(NAMESPACE_HEADINGS);
        				
        				for (NamespaceInfo namespace : namespaces) {
        					    					
        					buffer.append(String.format(
        							NAMESPACE_LINE_FORMAT, 
        							namespace.name
        							,namespace.getType()
        							,namespace.getReplicationFactor()
        							
        							,namespace.getMasterObjects()
        							,namespace.getProleObjects()
        							
        							,namespace.getUsedBytesMemory()
        							,namespace.getUsedBytesDisk()
        							
        							,FileUtil.getSizeString(namespace.getTotalBytesMemory(), Locale.US)
        							,FileUtil.getSizeString(namespace.getTotalBytesDisk(), Locale.US)

        							,namespace.getFreeMemoryPercent()
        							,namespace.getFreeDiskPercent()
        							));
        				}
        				buffer.append("\n");
        			}	
        			App.APP_LOGGER.info(buffer.toString());
        			buffer.setLength(0);
        			
        			List<SetInfo> sets;
        			for (Node node : client.getNodes() ) {

        				namespaces = getNamespaceInfo(node);
        				buffer.append("\n").append(node.getHost())
        				.append("\n\n");
        				
        				buffer.append(SET_HEADINGS);
        						
        				for (NamespaceInfo namespace : namespaces) {

        					
        					sets = getSetInfo(node,namespace.name);
        					for (SetInfo setInfo : sets) {

        						buffer.append(String.format(
        								SET_LINE_FORMAT, 
        								setInfo.name
        								,namespace.name
        								,setInfo.objectCount
        								,FileUtil.getSizeString(setInfo.bytesMemory,Locale.US)
        								));
        					}
        					
        				}
        				buffer.append("\n");
        			}
        			App.APP_LOGGER.info(buffer.toString());
        			buffer.setLength(0);

    			} catch (Exception e) {
    				App.APP_LOGGER.warning(e.toString());
    			}
        		
        	} catch (Exception e) {
        		App.APP_LOGGER.severe(e.getMessage());
        	}
    		
    		try {
    			Thread.sleep(60*1000);
    		} catch (Exception e) {
    			
    		}
    	}
    }
    
    
    
    
    
    
    
    
    
    
	public Map<String, String> map(String in, String delim) {
		Map<String, String> map = new HashMap<String, String>();
		String[] kvPair;
		for ( String pair : StringUtils.split(in,delim) ) {
			kvPair = pair.split("=");
			map.put(kvPair[0], kvPair[1]);
		}
		
		return map;
	}	
	
	public String getValue(Map<?,?> map, String... keys) {
		if ( map == null ) {
			return null;
		} else {
			Object value = null;
			for (String key : keys) {
				value = map.get(key);
				if ( value != null ) {
					break;
				}
			}
			
			return (value == null) ? null : value.toString();
		}
	}
	
	public NodeInfo getNodeInfo(Node node) {
		NodeInfo nodeInfo = new NodeInfo();
		
		Map<String,String> details =  Info.request(null,node);
		nodeInfo.id = details.get("node");
		nodeInfo.build = details.get("build");
		nodeInfo.edition = details.get("edition");
		nodeInfo.version = details.get("version");
		nodeInfo.statistics = map(details.get("statistics"), ";");
		
		return nodeInfo;
	}
		
		
	public List<NamespaceInfo> getNamespaceInfo(Node node) {	

		List<NamespaceInfo> namespaces = new ArrayList<NamespaceInfo>();
		NamespaceInfo namespaceInfo;

		for ( String namespace : StringUtils.split(Info.request(null, node, "namespaces"), ";") ) {			
			namespaceInfo = new NamespaceInfo();
			namespaceInfo.name = namespace;
			namespaceInfo.properties = map(Info.request(null, node, "namespace/" + namespace), ";");	
			namespaces.add(namespaceInfo);
		}
		
		return namespaces;
	}
	
	
	public NamespaceInfo getNamespaceInfo(Node node, String namespace) {	

		List<NamespaceInfo> namespaces = new ArrayList<NamespaceInfo>();
		NamespaceInfo namespaceInfo = new NamespaceInfo();
		namespaceInfo.name = namespace;
		namespaceInfo.properties = map(Info.request(null, node, "namespace/" + namespace), ";");	
		namespaces.add(namespaceInfo);
		
		return namespaceInfo;
	}
	
	public List<SetInfo> getSetInfo(Node node, String namespace) {
		
		List<SetInfo> sets = new ArrayList<SetInfo>();
		SetInfo setInfo;
		
		Map<String, String> map;
		for ( String set : StringUtils.split(Info.request(null, node, "sets/" + namespace), ";") ) {
			map = map(set, ":");
			setInfo = new SetInfo();
			setInfo.properties = map;
			setInfo.bytesMemory = getLong(map,0,"n-bytes-memory");
			setInfo.name = map.get("set_name");
			setInfo.namespace = map.get("ns_name");
			setInfo.objectCount = getLong(map,0,"n_objects");

			sets.add(setInfo);
		}
		
		return sets;
	}
	
	
	static class SimpleTreeNode {
		String displayName;
		Object value;
		
		public String toString() {
			return displayName;
		}
		
		public boolean equals(Object v) {
			return value.equals(v);
		}
	}
	
	static class NodeInfo {
		public String id;
		public String build;
		public String edition;
		public String version;
		public Map<String,String> statistics;			
	}
	
	static class NamespaceInfo {
		public String name;
		public Map<String, String> properties  = new HashMap<String, String>();		
		public String[] bins;
		public SetInfo[] sets;
		
		public long getObjects() {
			return getLong(properties, 0, "objects");
		}
		
		public String getType() {
			return getString(properties, "", "type");
		}
		
		public long getProleObjects() {
			return getLong(properties, 0, "prole-objects");
		}
		
		public long getUsedBytesMemory() {
			return getLong(properties, 0, "used-bytes-memory");
		}
		
		public long getReplicationFactor() {
			return getLong(properties, 0, "repl-factor");
		}
		
		public long getUsedBytesDisk() {
			return getLong(properties, 0, "used-bytes-disk");
		}
		
		public long getMasterObjects() {
			return getLong(properties, 0, "master-objects");
		}
		
		public long getTotalBytesMemory() {
			return getLong(properties, 0, "total-bytes-memory");
		}
		
		public long getTotalBytesDisk() {
			return getLong(properties, 0, "total-bytes-disk");
		}
		
		public long getFreeMemoryPercent() {
			return getLong(properties, 0, "free-pct-memory");
		}
		
		public long getFreeDiskPercent() {
			return getLong(properties, 0, "free-pct-disk");
		}		
	}
	
	static class SetInfo {
		public String namespace;
		public String name;
		public long objectCount;
		public long bytesMemory;
		public Map<String, String> properties;
	}
	
	
	
	public static String getProperty(Map<String, String> properties, String... keys) {
		if ( properties != null ) {
			String value;
			for (String key : keys) {
				value = properties.get(key);
				if ( value != null) {
					return value;
				}
			}
		}

		return null;
	}
	
	
	public static String getString(Map<String, String> properties, String def, String... keys) {
		String value = getProperty(properties, keys);
		return ( value == null ) ? def : value;
	}
	
	public static long getLong(Map<String, String> properties, long def, String... keys) {
		String value = getProperty(properties, keys);
		if ( value != null ) {
			try {
				return Long.parseLong(value);
			} catch (NumberFormatException nfe) {
				
			}
		}
		
		return def;
	}
}
