package com.rashidmayes.clairvoyance;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.ScanCallback;
import com.aerospike.client.async.AsyncClient;
import com.aerospike.client.cluster.Node;
import com.aerospike.client.policy.Priority;
import com.aerospike.client.policy.ScanPolicy;
import com.aerospike.client.query.RecordSet;
import com.aerospike.client.query.Statement;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.rashidmayes.clairvoyance.util.FileUtil;

import gnu.crypto.util.Base64;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;

public class SetController implements ScanCallback {
	
	public static final int DIGEST_LEN = 20;
	
	@FXML private GridPane rootPane;
	@FXML private ListView<Integer> pages;
	@FXML private TextArea recordDetails;
    @FXML private TableView<RecordRow> dataTable;
    @FXML private TabPane tabs;
    
	private ObjectMapper mObjectMapper = new ObjectMapper();
	private ObjectWriter mObjectWriter;

    private SetInfo mSetInfo;
    private Thread mScanThread;
    private int mRecordCount = 1;
    private int mPageCount = 0;
    private Thread mCurrentLoader;
    private Set<String> mColumns = new HashSet<String>();
    private Set<String> mKnownColumns = new HashSet<String>();
    
    
    private int mMaxBufferSize = 500;
    private int mMaxKeyBufferSize = 50000;
    private int mMaxPageZeroSize = 200000;
    
    private ArrayList<RecordRow> mRowBuffer = new ArrayList<RecordRow>(mMaxBufferSize);
    private ArrayList<byte[]> mKeyBuffer = new ArrayList<byte[]>(mMaxKeyBufferSize);
    
    private File mRootDir;
    private boolean cancelled;
    

    public SetController() {
    }

    @FXML
    public void initialize() {
    	
    	mObjectMapper.setSerializationInclusion(Include.NON_NULL);
    	mObjectWriter = mObjectMapper.writerWithDefaultPrettyPrinter();

    	rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {

    	    if (newScene == null) {
    	    	//stop scan
    	    	//clean files
    	    	cancelled = true;
    	    	if ( mRootDir != null ) {
    	    		App.EXECUTOR.execute(new Runnable() {
    	    			public void run() {
    	    				if ( FileUtils.deleteQuietly(mRootDir) ) {
    	    					App.APP_LOGGER.info("Removed " + mRootDir.getAbsolutePath());
    	    				}
    	    			}
    	    		});
    	    	}
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
		
				TableColumn<RecordRow, String> StringColumn = new TableColumn<RecordRow, String>("Digest");
				StringColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<RecordRow, String>, ObservableValue<String>>() {

				    @Override
				    public ObservableValue<String> call(TableColumn.CellDataFeatures<RecordRow, String> param) {
				    	RecordRow recordRow = param.getValue();
				    	if ( recordRow != null ) {
				    		return new SimpleStringProperty(Base64.encode(recordRow.getKey().digest));
				    	}  
				    	return new SimpleStringProperty("");	
				    }
				});
				dataTable.getColumns().add(StringColumn);
				
				pages.setCellFactory(new Callback<ListView<Integer>, ListCell<Integer>>()
		        {
		            @Override
		            public ListCell<Integer> call(ListView<Integer> listView)
		            {
		                return new PageRootCell();
		            }
		        });
				
				
    	    	mScanThread = new Thread(App.SCANS, new Runnable() {
					public void run() {
						if ( mScanThread != null && rootPane.getScene() != null ) {
							try {
						        mRootDir = new File(System.getProperty("java.io.tmpdir"));
						        mRootDir = new File(mRootDir,"clairvoyance");
						        mRootDir = new File(mRootDir,FileUtil.prettyFileName(mSetInfo.namespace, null, false));
						        mRootDir = new File(mRootDir,FileUtil.prettyFileName(mSetInfo.name, null, false));
						        mRootDir.mkdirs();
						        
						        App.APP_LOGGER.info(mRootDir.getPath());
								
				    	    	ScanPolicy scanPolicy = new ScanPolicy();
				    	    	scanPolicy.concurrentNodes = true;
				    	    	scanPolicy.priority = Priority.LOW;
				    	    	scanPolicy.scanPercent = 100;
				    	    	//scanPolicy.maxConcurrentNodes = 1;
				    	    	
				    	    	AsyncClient client = App.getClient();
				    	    	
				    	    	
				    	    	Statement statement = new Statement();
				    	    	statement.setNamespace(mSetInfo.namespace);
				    	    	statement.setSetName(mSetInfo.name);


				    	    	for ( Node node : client.getNodes() ) {
				    	    		if ( mScanThread != null && rootPane.getScene() != null && !cancelled ) {

						    	    	App.APP_LOGGER.info(mSetInfo.name + " start scan on " + node.getHost());
						    	    	RecordSet rs = client.queryNode(null, statement, node);
						    	    	try {
						    	    	    while (!cancelled && mScanThread != null && rootPane.getScene() != null && rs.next()) {
						    	    	        Key key = rs.getKey();
						    	    	        Record record = rs.getRecord();
						    	    	        //System.out.println(key + " " + record);
						    	    	        SetController.this.scanCallback(key, record);
						    	    	    }
						    	    	}
						    	    	finally {
						    	    	    rs.close();
						    	    	    App.APP_LOGGER.info(mSetInfo.name + " scan complete on " + node.getHost());	
						    	    	}
				    	    		}
				    	    	}
				    	    	

				    	    	/*
				    	    	for ( Node node : client.getNodes() ) {
				    	    		if ( mScanThread != null && rootPane.getScene() != null && !cancelled ) {
						    	    	App.APP_LOGGER.info(mSetInfo.name + " start scan on " + node.getHost());	
						    	    	client.scanNode(null, node, mSetInfo.namespace, mSetInfo.name, SetController.this);
						    	    	App.APP_LOGGER.info(mSetInfo.name + " scan complete on " + node.getHost());	
				    	    		}
				    	    	}*/

							} catch (AerospikeException.ScanTerminated e) {
								App.APP_LOGGER.info(e.getMessage());
								return;
							} catch (Exception e) {
								App.APP_LOGGER.log(Level.SEVERE, e.getMessage(), e);
							} finally {
								App.APP_LOGGER.info(mSetInfo.name + " scans complete");	
							}
							
							flushColumns();
							flush(Thread.currentThread());
							flushKeys();
						}
					}
    	    		
    	    	});
    	    	mScanThread.setDaemon(true);

    	    	mCurrentLoader = mScanThread;
    	    	mScanThread.start();
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
    			loadPage(newValue);
    		}
    	});
     }
    
    
    private void loadPage(final int pageNumber) {
    	App.APP_LOGGER.info("Load requested for " + pageNumber);
    	
    	//mRowBuffer = new ArrayList<RecordRow>();
    	//final List<RecordRow> list = mRowBuffer;
    	final File file = new File(mRootDir,pageNumber + ".data");
    	if ( file.exists() ) {
    		dataTable.getItems().clear();
    		dataTable.getItems().removeAll(dataTable.getItems());
    		
        	Thread thread = new Thread(App.LOADS, new Runnable() {
    			public void run() {
    				if ( Thread.currentThread() == mCurrentLoader && rootPane.getScene() != null ) {
    					
    			    	List<byte[]> keys = new ArrayList<byte[]>();

    			    	FileInputStream fis = null;
    			    	DataInputStream dis = null;
    			    	byte[] digest;
    			    	try {
    			    		//change to batch request
    			    		//AerospikeClient client = App.getClient();
    			    		
    			    		fis = new FileInputStream(file);
    			    		dis = new DataInputStream(fis);
    			    		
    			    		int recordIndex = 1 + (pageNumber * mMaxKeyBufferSize);
    			    		RecordRow recordRow;	
    			    		Key key;
    			    		Record record = null;
    			    		//list.clear();
    			    		mRowBuffer = new ArrayList<RecordRow>();
    			    		do {
    			    			digest = new byte[DIGEST_LEN];
    			    			dis.readFully(digest);
    			    			keys.add(digest);
    			    			
    			    			key = new Key(mSetInfo.namespace, digest, mSetInfo.name, null);
    			    			//record = client.get(null, key);
    			    			
    			    			recordRow = new RecordRow(key, record);	
    			    			recordRow.index = recordIndex++;
    			    			if ( Thread.currentThread() == mCurrentLoader) {
    			    				mRowBuffer.add(recordRow);
    			    			}
    			    			
    			    			/*
    			    			if ( record != null ) {
    			    				mColumns.addAll(record.bins.keySet());
    			    			}*/
    			    
    			    			if ( mRowBuffer.size() >= mMaxBufferSize ) {
    			    				flush(Thread.currentThread());
    			    			}			
    			    		} while (Thread.currentThread() == mCurrentLoader);
    			    		
    					} catch (EOFException eof) { 
    						App.APP_LOGGER.info("Reached end of " + file.getAbsolutePath());
    					} catch (IOException e) {
    						App.APP_LOGGER.log(Level.SEVERE, e.getMessage(), e);
    					} finally {
    						try { if ( dis != null ) dis.close(); } catch (Exception e) { }
    						try { if ( fis != null ) fis.close(); } catch (Exception e) { }
    					}
    					
    					flush(Thread.currentThread());
    				}
    			}
        		
        	}, file.getPath());
        	thread.setDaemon(true);

        	mCurrentLoader = thread;
        	thread.start();
    	}
    }
    
    @Override
	public void scanCallback(Key key, Record record) throws AerospikeException {

		if ( mScanThread == null || rootPane.getScene() == null || cancelled ) {
			throw new AerospikeException.ScanTerminated();
		}
		
		mColumns.addAll(record.bins.keySet());
		
		RecordRow recordRow = new RecordRow(key, record);	
		recordRow.index = mRecordCount++;
		
		if (mScanThread == mCurrentLoader && recordRow.index <= mMaxPageZeroSize) {
			mRowBuffer.add(recordRow);
		}
		
		//System.err.println(mRowBuffer.size() + " " + mMaxBufferSize + " budder " + ( mRowBuffer.size() >= mMaxBufferSize));
		if ( mRowBuffer.size() >= mMaxBufferSize ) {
			flushColumns();
			flush(mScanThread);
		}
		
		mKeyBuffer.add(key.digest);
		if ( mKeyBuffer.size() >= mMaxKeyBufferSize ) {
			flushKeys();
		}
    }
    
    private void flushKeys() {
 
    	final int pageNumber = mPageCount++;
    	final File file = new File(mRootDir,pageNumber + ".data");

    	final List<byte[]> keys = mKeyBuffer;
    	mKeyBuffer = new ArrayList<byte[]>();
    	
    	FileOutputStream fos = null;
    	DataOutputStream dos = null;
    	try {        	
    		fos = new FileOutputStream(file);
    		dos = new DataOutputStream(fos);
    		
    		for ( byte[] digest : keys ) {
    			if ( cancelled ) {
    				return;
    			}
    			dos.write(digest);
    		}
    		
    		dos.flush();
    		
    		Platform.runLater(new Runnable() {
    			public void run() {
    				try {
    					pages.getItems().add(pageNumber);
    				} catch (Exception e) {
    					
    				}
    			}
    		});
    		
		} catch (Exception e) {
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
			
			try {
				FileUtils.forceDeleteOnExit(file);
			} catch (IOException e) {
			}
		}
    }
    

    private void flushColumns() {
    	
    	final Set<String> columns = mColumns;
    	mColumns = new HashSet<String>();
    	
		Platform.runLater(new Runnable() {
			public void run() {
				try {
					TableColumn<RecordRow, String> column;
					
					for ( String s : columns ) {
						if ( !mKnownColumns.contains(s) ) {
							
							column = new TableColumn<RecordRow, String>(s);
							column.setMinWidth(50);
							column.setCellValueFactory(new NoSQLCellFactory(s));
							
							mKnownColumns.add(s);
							dataTable.getColumns().add(column);
						}	
					}	
				} catch (Exception e) {
					App.APP_LOGGER.log(Level.SEVERE,e.getMessage(),e);
				}
			}
		});
	}
    
    
    private void flush(final Thread thread) {
		    	
		final List<RecordRow> list = mRowBuffer;
		mRowBuffer = new ArrayList<RecordRow>(1024);

		Platform.runLater(new Runnable() {
			public void run() {
				
				try {
					
					if ( mCurrentLoader == thread ) {
					
						for ( RecordRow rr : list ) {
							if ( mCurrentLoader == thread ) {
								dataTable.getItems().add(rr);
							} else {
								break;
							}
						}	
					}
				} catch (Exception e) {
					App.APP_LOGGER.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		});
	}

	@FXML protected void handleAction(ActionEvent event) {
    	App.APP_LOGGER.info(event.toString());
    }
    
    static class PageRootCell extends ListCell<Integer> {
        @Override
        public void updateItem(Integer item, boolean empty) {
            super.updateItem(item, empty);
            if ( item == null ) {
            	this.setText(null);
            } else {
            	this.setText(StringUtils.leftPad(Integer.toString(item, Character.MAX_RADIX).toUpperCase(), 4, "0"));
            }
        }
    }
}
