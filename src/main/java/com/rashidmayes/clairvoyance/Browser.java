package com.rashidmayes.clairvoyance;

import java.io.File;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

import javax.swing.text.TableView;

import org.apache.commons.io.FileUtils;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.cluster.Node;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.rashidmayes.clairvoyance.util.FileUtil;
import com.rashidmayes.clairvoyance.util.Platform;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class Browser implements Runnable, ChangeListener<TreeItem<SimpleTreeNode>>, EventHandler<MouseEvent> {
	
	private static final String NAME_FORMAT = "%s (%s)";
	
    private final ImageView rootIcon = new ImageView(new Image(getClass().getClassLoader().getResourceAsStream("ic_cluster.png")));
    private final Image namespaceIcon = new Image(getClass().getClassLoader().getResourceAsStream("ic_storage.png"));
    private final Image setIcon = new Image(getClass().getClassLoader().getResourceAsStream("ic_set.png"));
	
    private boolean tick = false;
    private NumberFormat mNumberFormat = NumberFormat.getNumberInstance();
    
    @FXML private TextArea console;
    @FXML private TreeView<SimpleTreeNode> namespacesTree;
    @FXML private TableView dataTable;
    @FXML private TabPane tabs;
    
    private TextAreaLogHandler mTextAreaLogHandler;
    
    private Thread mThread;
    private boolean cancel = false;
    
	private ObjectMapper mObjectMapper = new ObjectMapper();
	private ObjectWriter mObjectWriter;


    public Browser() {
    	mObjectMapper.setSerializationInclusion(Include.NON_NULL);
    	mObjectWriter = mObjectMapper.writerWithDefaultPrettyPrinter();
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
		        	tab.setContent( (javafx.scene.Node) FXMLLoader.load(getClass().getClassLoader().getResource("tab_namespace.fxml")) );
		        } else if ( identifiable instanceof SetInfo ) {
		        	tab.setContent( (javafx.scene.Node) FXMLLoader.load(getClass().getClassLoader().getResource("tab_set.fxml")) );
		        } else {
		        	tab.setContent( (javafx.scene.Node) FXMLLoader.load(getClass().getClassLoader().getResource("tab_cluster.fxml")) );
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
    	App.APP_LOGGER.info(event.getSource().toString());
    }
    
    @FXML protected void handleClusterDump(ActionEvent event) {
    	
    	
    	try {
    		MenuItem item = (MenuItem) event.getSource();
	        Tab tab = new Tab();
	        tab.setId(item.getId());
	        tab.setText(item.getText());
	            	        
	        Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("tab_cluster.fxml"));
	        tab.setContent(root);
    		
	        tabs.getTabs().add(tab);
	        tabs.getSelectionModel().select(tab);	
	        
		} catch (Exception e) {
			App.APP_LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
    	
		App.EXECUTOR.execute(new Runnable() {
			
			public void run() {
		    	try {
		    		AerospikeClient client = App.getClient();
					App.APP_LOGGER.info(mObjectWriter.writeValueAsString(client));
					NodeInfo nodeInfo;
					for ( Node node : client.getNodes() ) {
						nodeInfo = App.getNodeInfo(node);
						nodeInfo.namespaces = App.getNamespaceInfo(node).toArray(new NamespaceInfo[0]);
						for ( NamespaceInfo ni : nodeInfo.namespaces ) {
							ni.sets = App.getSetInfo(node, ni.name).toArray(new SetInfo[0]);
						}
						
						
						App.APP_LOGGER.info(mObjectWriter.writeValueAsString(nodeInfo));
					}
				} catch (Exception e) {
					App.APP_LOGGER.log(Level.SEVERE, e.getMessage(), e);
				}		
			}

		});
    	
    }
    
    @FXML protected void handleReconnect(ActionEvent event) {
    	try {
			App.APP_LOGGER.info(mObjectWriter.writeValueAsString(App.getClient(true)));
		} catch (Exception e) {
			App.APP_LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
    }
    
    @FXML protected void handleClearCache(ActionEvent event) {
    	//move off UI thread
    	try {
        	File mRootDir = new File(System.getProperty("java.io.tmpdir"));
            mRootDir = new File(mRootDir,"clairvoyance");
            FileUtils.deleteDirectory(mRootDir);
		} catch (Exception e) {
			App.APP_LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
    }
    
    @FXML protected void handleExit(ActionEvent event) {
    	System.exit(0);
    }
    
    @FXML protected void handleSettings(ActionEvent event) {
    	try {
    		MenuItem item = (MenuItem) event.getSource();
	        Tab tab = new Tab();
	        tab.setId(item.getId());
	        tab.setText(item.getText());
	            	        
	        Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("tab_settings.fxml"));
	        tab.setContent(root);
    		
	        tabs.getTabs().add(tab);
	        tabs.getSelectionModel().select(tab);	
	        
		} catch (Exception e) {
			App.APP_LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
    }
    
    
    @FXML protected void handleCreateData(ActionEvent event) {
    	try {
    		Tab tab = null;
    		MenuItem item = (MenuItem) event.getSource();
    		for (Tab temp : tabs.getTabs()) {
    			if ( temp.getId().equals(item.getId()) ) {
    				tab = temp;
    				break;
    			}
    		}
    		
    		if ( tab == null ) {
    			tab = new Tab();
				tab.setId(item.getId());
				tab.setText(item.getText());
				
		        Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("tab_data_creator.fxml"));
		        tab.setContent(root);
		        
		        tabs.getTabs().add(tab);
    		}

	        tabs.getSelectionModel().select(tab);	
	        
		} catch (Exception e) {
			App.APP_LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
    }
    
    @FXML protected void handleAbout(ActionEvent event) {
    	try {
    		MenuItem item = (MenuItem) event.getSource();
    		
	        Tab tab = new Tab();
	        tab.setId(item.getId());
	        tab.setText(item.getText());
	            	        
	        Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("tab_about.fxml"));
	        tab.setContent(root);

	        tabs.getTabs().add(tab);
	        tabs.getSelectionModel().select(tab);	
	        
		} catch (Exception e) {
			App.APP_LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
    }
    
    
    @FXML protected void handleWeb(ActionEvent event) {
    	
    	if ( event.getSource() instanceof MenuItem ) {
    		
        	try {
        		MenuItem item = (MenuItem) event.getSource();
        		App.APP_LOGGER.info("opening " + item.getId());
        		
    	        Tab tab = new Tab();
    	        tab.setId(item.getId());
    	        tab.setText(item.getText());
    	            	        
    	        Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("tab_web.fxml"));
    	        tab.setContent(root);

        		WebEngine engine = ((WebView)root).getEngine();
        		engine.load(item.getId());	
        		
    	        tabs.getTabs().add(tab);
    	        tabs.getSelectionModel().select(tab);	
    	        
    		} catch (Exception e) {
    			App.APP_LOGGER.log(Level.SEVERE, e.getMessage(), e);
    		}
    	}
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
				
				Runtime runtime = Runtime.getRuntime();
				App.APP_LOGGER.info("Free memory: " + FileUtils.byteCountToDisplaySize(runtime.freeMemory()));
				App.APP_LOGGER.info(mObjectWriter.writeValueAsString(new Platform()));
				
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
