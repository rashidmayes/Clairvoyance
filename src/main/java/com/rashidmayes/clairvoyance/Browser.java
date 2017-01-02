package com.rashidmayes.clairvoyance;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

import javax.swing.text.TableView;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.cluster.Node;
import com.rashidmayes.clairvoyance.util.FileUtil;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

public class Browser implements Runnable, ChangeListener<TreeItem<SimpleTreeNode>>, EventHandler<MouseEvent> {
	
	private static final String NAMESPACE_LINE_FORMAT = "%-10s %10s %2d %16d %16d %16d %16d %8s %8s %8d%% %8d%%\n";
	private static final String NAMESPACE_HEADINGS = String.format("%-10s %10s %2s %16s %16s %16s %16s %8s %8s %9s %9s\n"
			,"Name", "Type", "RF", "Master", "Prole", "Used Memory", "Used Disk", "Memory", "Disk", "Free Mem", "Free Disk");
	private static final String SET_LINE_FORMAT = "%-16s %-16s %16d %16s\n";
	private static final String SET_HEADINGS = String.format("%-16s %-16s %16s %16s\n"
			,"Set", "Namespace", "Objects", "Bytes Memory");
	private static final String NAME_FORMAT = "%s (%s)";
	
	
    private final ImageView rootIcon = new ImageView(new Image(getClass().getResourceAsStream("ic_cluster.png")));
    private final Image namespaceIcon = new Image(getClass().getResourceAsStream("ic_storage.png"));
    private final Image setIcon = new Image(getClass().getResourceAsStream("ic_set.png"));
	
    private boolean tick = false;
    private NumberFormat mNumberFormat = NumberFormat.getNumberInstance();
    
    @FXML private TextArea console;
    @FXML private TreeView<SimpleTreeNode> namespacesTree;
    @FXML private TableView dataTable;
    @FXML private TabPane tabs;
    
    private TextAreaLogHandler mTextAreaLogHandler;
    
    private Thread mThread;
    private boolean cancel = false;

    public Browser() {
    }

    @FXML
    public void initialize() {
    	//set up console
    	mTextAreaLogHandler = new TextAreaLogHandler(console,5*1024);
    	App.APP_LOGGER.addHandler(mTextAreaLogHandler);
    	        
        namespacesTree.setOnMouseClicked(this);
        namespacesTree.getSelectionModel().selectedItemProperty().addListener(this);

    	mThread = new Thread(this);
    	mThread.setDaemon(true);
    	mThread.start();
     }
    
    @Override
    public void handle(MouseEvent mouseEvent) {            
        if ( mouseEvent.getClickCount() == 2 ) {
            //TreeItem<SimpleTreeNode> item = namespacesTree.getSelectionModel().getSelectedItem();
        }
    }
    

	@Override
	public void changed(ObservableValue<? extends TreeItem<SimpleTreeNode>> observable, TreeItem<SimpleTreeNode> oldValue, TreeItem<SimpleTreeNode> newValue) {

		try {
			Tab tab = null;
			String id = newValue.getValue().value.getId().toString();
			for ( Tab t : tabs.getTabs() ) {
				if ( t.getId().equals(id) ) {
					tab = t;
					break;
				}
			}
			
			if ( tab == null ) {
		        tab = new Tab();
		        tab.setId(id);
		        tab.setText(newValue.getValue().displayName);
		        
		        
		        Identifiable identifiable = newValue.getValue().value;
		        
		        if ( identifiable instanceof NamespaceInfo ) {
		        	tab.setContent( (javafx.scene.Node) FXMLLoader.load(this.getClass().getResource("tab_namespace.fxml")) );
		        } else if ( identifiable instanceof SetInfo ) {
		        	tab.setContent( (javafx.scene.Node) FXMLLoader.load(this.getClass().getResource("tab_set.fxml")) );
		        } else {
		        	tab.setContent( (javafx.scene.Node) FXMLLoader.load(this.getClass().getResource("tab_cluster.fxml")) );
		        }
		        tab.getContent().setUserData(identifiable);
		        tabs.getTabs().add(tab);
			}
			
			tabs.getSelectionModel().select(tab);			
		} catch (Exception e) {
			App.APP_LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
	}    

    
    @FXML protected void handleAction(ActionEvent event) {
    	
    	App.APP_LOGGER.info(event.toString());
    	
    }
    
    public void run() {
    	
    	while (!cancel) {
    	
    	   	try {    	   		
        		AerospikeClient client = App.getClient();
        		
				try {
					Node node = client.getNodes()[0];
					
					//tree update
					TreeItem<SimpleTreeNode> root = namespacesTree.getRoot();
					if ( root == null ) {
						NodeInfo nodeInfo = App.getNodeInfo(node);
				    	SimpleTreeNode rootNode = new SimpleTreeNode();
				    	rootNode.displayName = App.host + ":" + App.port;
				    	rootNode.value = nodeInfo;
				    	
				        root = new TreeItem<SimpleTreeNode> (rootNode, rootIcon);
				        //rootItem.setExpanded(true);
				        
				        namespacesTree.setRoot(root);
					}

					//list namespaces
					List<NamespaceInfo> namespaces = App.getNamespaceInfo(node);

					List<SetInfo> sets;
					TreeItem<SimpleTreeNode> namespaceNode;
					TreeItem<SimpleTreeNode> setNode;
					for (NamespaceInfo namespace : namespaces) {

						namespaceNode = null;
						for (TreeItem<SimpleTreeNode> tempNode : root.getChildren()) {
	
							if (tempNode.getValue().value.getId().equals(namespace.getId())) {
								namespaceNode = tempNode;
								namespaceNode.getValue().value = namespace;
								
								if ( namespace.getMasterObjects() == 0 ) {
									namespaceNode.getValue().displayName = namespace.name;
								} else {
									if ( tick ) {
										namespaceNode.getValue().displayName = String.format(NAME_FORMAT, namespace.name, mNumberFormat.format(namespace.getMasterObjects()));
									} else {
										if ( "device".equals(namespace.getType()) ) {
											namespaceNode.getValue().displayName = String.format(NAME_FORMAT, namespace.name, FileUtil.getSizeString(namespace.getUsedBytesDisk(), Locale.US));
											
										} else {
											namespaceNode.getValue().displayName = String.format(NAME_FORMAT, namespace.name, FileUtil.getSizeString(namespace.getUsedBytesMemory(), Locale.US));
										}
									}
								}

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

						sets = App.getSetInfo(node, namespace.name);
						for ( SetInfo setInfo : sets ) {
							
							setNode = null;
							for (TreeItem<SimpleTreeNode> tempNode : namespaceNode.getChildren()) {

								if (tempNode.getValue().value.getId().equals(setInfo.getId())) {
									setNode = tempNode;
									setNode.getValue().value = setInfo;
									
									if ( setInfo.objectCount == 0) {
										setNode.getValue().displayName = setInfo.name;
									} else {
										if ( tick ) {
											setNode.getValue().displayName = String.format(NAME_FORMAT, setInfo.name, mNumberFormat.format(setInfo.objectCount));
										} else {
											setNode.getValue().displayName = String.format(NAME_FORMAT, setInfo.name, FileUtil.getSizeString(setInfo.bytesMemory, Locale.US));
										}
									}

									
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

        				namespaces = App.getNamespaceInfo(node);
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

        				namespaces = App.getNamespaceInfo(node);
        				buffer.append("\n").append(node.getHost())
        				.append("\n\n");
        				
        				buffer.append(SET_HEADINGS);
        						
        				for (NamespaceInfo namespace : namespaces) {

        					
        					sets = App.getSetInfo(node,namespace.name);
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
    			Thread.sleep(5*60*1000);
    		} catch (Exception e) {
    			
    		}
    		
    		tick = !tick;
    	}
    }
}
