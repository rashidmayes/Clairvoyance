package com.rashidmayes.clairvoyance;

import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.cluster.Node;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.rashidmayes.clairvoyance.util.FileUtil;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;

public class ClusterDumpController {
	
	private static final String NAMESPACE_LINE_FORMAT = "%-10s %10s %2d %16d %16d %16d %16d %8s %8s %8d%% %8d%%\n";
	private static final String NAMESPACE_HEADINGS = String.format("%-10s %10s %2s %16s %16s %16s %16s %8s %8s %9s %9s\n"
			,"Name", "Type", "RF", "Master", "Prole", "Used Memory", "Used Disk", "Memory", "Disk", "Free Mem", "Free Disk");
	private static final String SET_LINE_FORMAT = "%-16s %-16s %16d %16s\n";
	private static final String SET_HEADINGS = String.format("%-16s %-16s %16s %16s\n"
			,"Set", "Namespace", "Objects", "Bytes Memory");
	
		
    @FXML private TextArea dumpOutput;
    @FXML private TabPane tabs;
    
	private ObjectMapper mObjectMapper = new ObjectMapper();
	private ObjectWriter mObjectWriter;

    public ClusterDumpController() {
    	mObjectMapper.setSerializationInclusion(Include.NON_NULL);
    	mObjectWriter = mObjectMapper.writerWithDefaultPrettyPrinter();
    }

    @FXML
    public void initialize() {
    	
		App.EXECUTOR.execute(new Runnable() {
			
			public void run() {
		    	try {
		    		
		    		AerospikeClient client = App.getClient();
		    		
	    	   		///debugout
            		final StringBuffer buffer = new StringBuffer();
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
        
        			buffer.append("\n\n").append(mObjectWriter.writeValueAsString(client)).append("\n\n");
					NodeInfo nodeInfo;
					for ( Node node : client.getNodes() ) {
						nodeInfo = App.getNodeInfo(node);
						nodeInfo.namespaces = App.getNamespaceInfo(node).toArray(new NamespaceInfo[0]);
						for ( NamespaceInfo ni : nodeInfo.namespaces ) {
							ni.sets = App.getSetInfo(node, ni.name).toArray(new SetInfo[0]);
						}
						
						buffer.append(mObjectWriter.writeValueAsString(nodeInfo)).append('\n');;
					}
								
					Platform.runLater(new Runnable() {
						public void run() {
							dumpOutput.setText(buffer.toString());
						}
					});
				} catch (Exception e) {
					App.APP_LOGGER.log(Level.SEVERE, e.getMessage(), e);
				}		
			}

		});

     }

    @FXML protected void handleAction(ActionEvent event) {
    	App.APP_LOGGER.info(event.toString());
    }
    
}
