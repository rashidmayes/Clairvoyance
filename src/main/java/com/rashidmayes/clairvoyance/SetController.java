package com.rashidmayes.clairvoyance;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.ScanCallback;
import com.aerospike.client.cluster.Node;
import com.aerospike.client.policy.Priority;
import com.aerospike.client.policy.ScanPolicy;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;

public class SetController extends Service<String> implements EventHandler<WorkerStateEvent>, ScanCallback {
	
	public static final int DIGEST_LEN = 20;
	
	@FXML private SplitPane rootPane;
	@FXML private ListView<Integer> pages;
	@FXML private TextArea recordDetails;
    @FXML private TableView<RecordRow> dataTable;
    @FXML private TabPane tabs;
    
	private ObjectMapper mObjectMapper = new ObjectMapper();
	private ObjectWriter mObjectWriter;

    private SetInfo mSetInfo;
    private Thread mThread;
    private int mRecordCount = 1;
    private int pageCount = 0;
    private Set<String> mColumns = new HashSet<String>();
    private Set<String> mKnownColumns = new HashSet<String>();
    
    
    private int mMaxBufferSize = 300;
    private int mMaxKeyBufferSize = 1000;
    private ArrayList<RecordRow> mRowBuffer = new ArrayList<RecordRow>(mMaxBufferSize);
    private ArrayList<byte[]> mKeyBuffer = new ArrayList<byte[]>(mMaxKeyBufferSize);
    
    private Path mPath;
    private File mDir;
    

    public SetController() {
    }

    @FXML
    public void initialize() {
    	
    	mObjectMapper.setSerializationInclusion(Include.NON_NULL);
    	mObjectWriter = mObjectMapper.writerWithDefaultPrettyPrinter();
    	
    	setOnSucceeded(this);
    	
    	rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {

    	    if (newScene == null) {
    	    	//stop scan
    	    } else {
    	    	//start scan
    	    	mSetInfo = (SetInfo) rootPane.getUserData();
    	    	
				TableColumn<RecordRow, Number> column = new TableColumn<RecordRow, Number>("#");

				column.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<RecordRow, Number>, ObservableValue<Number>>() {

				    @Override
				    public ObservableValue<Number> call(TableColumn.CellDataFeatures<RecordRow, Number> param) {
				    	RecordRow recordRow = param.getValue();
				    	if ( recordRow != null ) {
				    		return new SimpleIntegerProperty(recordRow.index);
				    	}  
				    	return new SimpleIntegerProperty(0);	
				    }
				});
				
				dataTable.getColumns().add(column);

				pages.setCellFactory(new Callback<ListView<Integer>, ListCell<Integer>>()
		        {
		            @Override
		            public ListCell<Integer> call(ListView<Integer> listView)
		            {
		                return new ColorRectCell();
		            }
		        });
				
				
    	    	mThread = new Thread(App.SCANS, new Runnable() {
					public void run() {
						if ( mThread != null && rootPane.getScene() != null ) {
							try {
								mPath = Files.createTempDirectory("clairvoyance");
								mDir = new File(mPath.toFile(), Integer.toHexString(mSetInfo.getId().toString().hashCode()));
								mDir.mkdirs();
								
				    	    	ScanPolicy scanPolicy = new ScanPolicy();
				    	    	scanPolicy.concurrentNodes = false;
				    	    	scanPolicy.priority = Priority.LOW;
				    	    	scanPolicy.scanPercent = 100;
				    	    	
				    	    	AerospikeClient client = App.getClient();
				    	    	client.scanAll(scanPolicy, mSetInfo.namespace, mSetInfo.name, SetController.this);	
							} catch (Exception e) {
								App.APP_LOGGER.log(Level.SEVERE, e.getMessage(), e);
							} finally {
								App.APP_LOGGER.info(mSetInfo.name + " scan complete");	
							}
							
							flush();
							flushKeys();
						}
					}
    	    		
    	    	});
    	    	mThread.setDaemon(true);
    	    	mThread.start();
    	    }
    	});
    	
    	dataTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
    		if ( newValue != null ) {
        		try {
    				recordDetails.setText(mObjectWriter.writeValueAsString(newValue));
    			} catch (Exception e) {
    				App.APP_LOGGER.warning(e.getMessage());
    			}	
    		}
    	});
    	
    	
    	pages.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
    		if ( newValue != null ) {
        		try {
    				recordDetails.setText(mObjectWriter.writeValueAsString(newValue));
    			} catch (Exception e) {
    				App.APP_LOGGER.warning(e.getMessage());
    			}	
    		}
    	});
     }
    
    static class ColorRectCell extends ListCell<Integer> {
        @Override
        public void updateItem(Integer item, boolean empty) {
            super.updateItem(item, empty);
            if ( item == null ) {
            	this.setText(null);
            } else {
            	this.setText(String.valueOf(item));
            }
        }
    }
     

    
    @Override
	public void scanCallback(Key key, Record record) throws AerospikeException {
		if ( mThread == null || rootPane.getScene() == null ) {
			throw new AerospikeException.ScanTerminated();
		}
		
		RecordRow recordRow = new RecordRow(key, record);	
		recordRow.index = mRecordCount++;
		mRowBuffer.add(recordRow);
		mKeyBuffer.add(key.digest);
		
		mColumns.addAll(record.bins.keySet());
		
		if ( mRowBuffer.size() >= mMaxBufferSize ) {
			flush();
		}
		
		if ( mKeyBuffer.size() >= mMaxKeyBufferSize ) {
			flushKeys();
		}
    }
    
    private void flushKeys() {
    	
    	final int pageNumber = pageCount++;
    	final File file = new File(mDir,pageNumber + ".data");
    	final List<byte[]> keys = mKeyBuffer;
    	mKeyBuffer = new ArrayList<byte[]>();
    	
    	FileOutputStream fos = null;
    	DataOutputStream dos = null;
    	try {
    		fos = new FileOutputStream(file);
    		dos = new DataOutputStream(fos);
    		
    		for ( byte[] digest : keys ) {
    			dos.write(digest);
    		}
    		
    		dos.flush();
    		
    		Platform.runLater(new Runnable() {
    			public void run() {
    				pages.getItems().add(pageNumber);
    			}
    		});
    		
		} catch (IOException e) {
			App.APP_LOGGER.log(Level.SEVERE, e.getMessage(), e);
		} finally {
			try {
				if ( dos != null ) dos.close();
			} catch (Exception e) {
				
			}
			
			try {
				if ( fos != null ) fos.close();
			} catch (Exception e) {
				
			}
		}
    	
    	System.out.println(file.getPath());
    }
    
    
    private void flush() {
		    	
		final List<RecordRow> list = mRowBuffer;
		mRowBuffer = new ArrayList<RecordRow>(1024);
		
		Platform.runLater(new Runnable() {
			public void run() {
				
				TableColumn<RecordRow, String> column;
				for ( String s : mColumns ) {
					if ( !mKnownColumns.contains(s) ) {
						mKnownColumns.add(s);
						column = new TableColumn<RecordRow, String>(s);
						column.setMinWidth(100);
						column.setCellValueFactory(new NoSQLCellFactory(s));
						
						dataTable.getColumns().add(column);
					}
				}
				mColumns.clear();
	
				for ( RecordRow rr : list ) {
					dataTable.getItems().add(rr);
				}
			}
		});
    	
		//App.APP_LOGGER.info(key + " " + record);
	}

	@FXML protected void handleAction(ActionEvent event) {
    	App.APP_LOGGER.info(event.toString());
    }
    
    
    @Override
    public void handle(WorkerStateEvent event) {
        String json = String.valueOf(event.getSource().getValue());
        if ( json != null ) {

        }
    }
    
    protected Task<String> createTask() {

        return new Task<String>() {
            protected String call() {
            	
            	mSetInfo = (SetInfo) rootPane.getUserData();
        		if ( mSetInfo != null ) {
        			
            	   	try {    	   		
                		AerospikeClient client = App.getClient();
                		Node node = client.getNodes()[0];

                        return mObjectWriter.writeValueAsString(mSetInfo);
                		
                	} catch (Exception e) {
                		App.APP_LOGGER.severe(e.getMessage());
                		e.printStackTrace();
                	}
        		}
            	
        		return null;
            }
        };
    }
}
