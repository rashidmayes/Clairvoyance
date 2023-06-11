package com.rashidmayes.clairvoyance;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.async.AsyncClient;
import com.aerospike.client.cluster.Node;
import com.rashidmayes.clairvoyance.model.*;
import com.rashidmayes.clairvoyance.util.ClairvoyanceLogger;
import com.rashidmayes.clairvoyance.util.FileUtil;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class Browser implements ChangeListener<TreeItem<SimpleTreeNode>> {

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(
            1,
            runnable -> {
                var thread = new Thread(runnable);
                thread.setName("update-cluster-info-scheduled-thread");
                thread.setDaemon(true);
                return thread;
            }
    );

    private final ImageView rootIcon;
    private final Image nodeIcon;
    private final Image namespaceIcon;
    private final Image setIcon;

    private final NumberFormat numberFormat = NumberFormat.getNumberInstance();

    @FXML
    private TextArea console;
    @FXML
    private TreeView<SimpleTreeNode> namespacesTree;
    @FXML
    private TabPane tabs;

    private final NodeInfoMapper nodeInfoMapper = new NodeInfoMapper();

    public Browser() {
        var classLoader = getClass().getClassLoader();

        var rootIconImage = classLoader.getResourceAsStream("ic_cluster.png");
        Objects.requireNonNull(rootIconImage, "ic_cluster.png is missing");
        this.rootIcon = new ImageView(new Image(rootIconImage));

        var nodeIconImage = classLoader.getResourceAsStream("node.png");
        Objects.requireNonNull(nodeIconImage, "node.png is missing");
        this.nodeIcon = new Image(nodeIconImage);

        var namespaceIconImage = classLoader.getResourceAsStream("ic_storage.png");
        Objects.requireNonNull(namespaceIconImage, "ic_storage.png is missing");
        this.namespaceIcon = new Image(namespaceIconImage);

        var setIconImage = classLoader.getResourceAsStream("ic_set.png");
        Objects.requireNonNull(setIconImage, "ic_set.png is missing");
        this.setIcon = new Image(setIconImage);
    }

    @FXML
    public void initialize() {
        TextAreaLogHandler textAreaLogHandler = new TextAreaLogHandler(console);
        ClairvoyanceLogger.logger.addHandler(textAreaLogHandler);

        namespacesTree.getSelectionModel()
                .selectedItemProperty()
                .addListener(this);

        executor.scheduleAtFixedRate(updateClusterTreeView(), 0, 2, TimeUnit.MINUTES);
    }

    @Override
    public void changed(ObservableValue<? extends TreeItem<SimpleTreeNode>> observable, TreeItem<SimpleTreeNode> oldValue, TreeItem<SimpleTreeNode> newValue) {
        try {
            var id = newValue.getValue().value.getId();
            var optionalTab = getTab(id);

            if (optionalTab.isEmpty()) {
                var tab = new Tab();
                tab.setId(id);
                tab.setText(newValue.getValue().displayName);

                var identifiable = newValue.getValue().value;

                if (identifiable instanceof NamespaceInfo) {
                    tab.setContent(FXMLLoader.load(getClass().getClassLoader().getResource("tab_namespace.fxml")));
                } else if (identifiable instanceof SetInfo) {
                    tab.setContent(FXMLLoader.load(getClass().getClassLoader().getResource("tab_set.fxml")));
                } else {
                    tab.setContent(FXMLLoader.load(getClass().getClassLoader().getResource("tab_cluster.fxml")));
                }
                tab.getContent().setUserData(identifiable);
                tabs.getTabs().add(tab);
                tabs.getSelectionModel().select(tab);
            } else {
                tabs.getSelectionModel().select(optionalTab.get());
            }
        } catch (Exception e) {
            ClairvoyanceLogger.logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @FXML
    protected void handleReconnect(ActionEvent event) {
        try {
            createNewClient();
            updateClusterTreeView().run();
        } catch (Exception e) {
            ClairvoyanceLogger.logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @FXML
    protected void handleClearCache(ActionEvent event) {
        ApplicationModel.INSTANCE.runInBackground(() -> {
            try {
                ClairvoyanceLogger.logger.log(Level.INFO, "deleting tmp clairvoyance directory");
                File mRootDir = new File(System.getProperty("java.io.tmpdir"));
                mRootDir = new File(mRootDir, "clairvoyance");
                FileUtils.deleteDirectory(mRootDir);
                ClairvoyanceLogger.logger.log(Level.INFO, "clairvoyance tmp directory has been deleted");
            } catch (Exception e) {
                ClairvoyanceLogger.logger.log(Level.SEVERE, e.getMessage(), e);
            }
        });
    }

    @FXML
    protected void handleAbout(ActionEvent event) {
        try {
            var optionalTab = getTab("about");
            if (optionalTab.isEmpty()) {
                Tab tab = new Tab();
                tab.setId("about");
                tab.setText("About");

                var resource = getClass().getClassLoader().getResource("tab_about.fxml");
                Objects.requireNonNull(resource, "tab_about.fxml is missing");
                Parent root = FXMLLoader.load(resource);
                tab.setContent(root);

                tabs.getTabs().add(tab);
                tabs.getSelectionModel().select(tab);
            } else {
                tabs.getSelectionModel().select(optionalTab.get());
            }
        } catch (Exception e) {
            ClairvoyanceLogger.logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @FXML
    protected void handleWeb(ActionEvent event) {
        if (event.getSource() instanceof MenuItem) {
            try {
                MenuItem item = (MenuItem) event.getSource();
                ClairvoyanceLogger.logger.info("opening " + item.getId());

                Tab tab = new Tab();
                tab.setId(item.getId());
                tab.setText(item.getText());

                var resource = getClass().getClassLoader().getResource("tab_web.fxml");
                Objects.requireNonNull(resource, "tab_web.fxml is missing");
                Parent root = FXMLLoader.load(resource);
                tab.setContent(root);

                WebEngine engine = ((WebView) root).getEngine();
                engine.load(item.getId());

                tabs.getTabs().add(tab);
                tabs.getSelectionModel().select(tab);
            } catch (Exception e) {
                ClairvoyanceLogger.logger.log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }

    @FXML
    protected void handleClusterDump(ActionEvent event) {
        try {
            Tab tab = new Tab();
            tab.setId("full-cluster-dump");
            tab.setText("Full cluster dump");

            var resource = getClass().getClassLoader().getResource("tab_cluster.fxml");
            Objects.requireNonNull(resource, "tab_cluster.fxml cannot be null");
            Parent root = FXMLLoader.load(resource);
            tab.setContent(root);

            tabs.getTabs().add(tab);
            tabs.getSelectionModel().select(tab);
        } catch (Exception e) {
            ClairvoyanceLogger.logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @FXML
    protected void handleExit(ActionEvent event) {
        System.exit(0);
    }

    public Runnable updateClusterTreeView() {
        return () -> Platform.runLater(() -> {
            ClairvoyanceLogger.logger.log(Level.INFO, "refreshing cluster tree view");
            try {
                var client = ClairvoyanceFxApplication.getClient();
                updateTreeView(client);
            } catch (Exception e) {
                ClairvoyanceLogger.logger.severe(e.getMessage());
            }
        });

    }

    private void updateTreeView(AerospikeClient client) {
        if (namespacesTree.getRoot() == null) {
            var rootNode = createRootModelNode(ApplicationModel.INSTANCE.getConnectionInfo());
            var treeRoot = new TreeItem<>(rootNode, rootIcon);
            treeRoot.setExpanded(true);
            namespacesTree.setRoot(treeRoot);
        }

        // todo: why only first node?!
        Node node = client.getNodes()[0];
        NodeInfo nodeInfo = nodeInfoMapper.getNodeInfo(node);


        var parent = namespacesTree.getRoot();

        buildNamespaces(nodeInfo, parent);
    }

    private void buildNamespaces(NodeInfo nodeInfo, TreeItem<SimpleTreeNode> parent) {
        for (NamespaceInfo namespace : nodeInfo.getNamespaces()) {
            var namespaceNode = buildNamespaceNode(parent, namespace);
            buildSets(namespace, namespaceNode);
        }
    }

    private TreeItem<SimpleTreeNode> buildNamespaceNode(TreeItem<SimpleTreeNode> parent, NamespaceInfo namespace) {
        var namespaceViewNodeResult = findViewNodeInChildren(namespace.getId(), parent);
        if (namespaceViewNodeResult.isPresent()) {
            var namespaceViewNode = namespaceViewNodeResult.get();
            var namespaceModelNode = createNamespaceModelNode(namespace);
            namespaceViewNode.setValue(namespaceModelNode);
            return namespaceViewNode;
        } else {
            var namespaceModelNode = createNamespaceModelNode(namespace);
            var namespaceViewNode = new TreeItem<>(namespaceModelNode, new ImageView(namespaceIcon));
            namespaceViewNode.setExpanded(true);
            parent.getChildren().add(namespaceViewNode);
            return namespaceViewNode;
        }
    }

    private void buildSets(NamespaceInfo namespace, TreeItem<SimpleTreeNode> namespaceNode) {
        for (SetInfo setInfo : namespace.getSets()) {
            var setViewNodeResult = findViewNodeInChildren(setInfo.getId(), namespaceNode);
            if (setViewNodeResult.isPresent()) {
                var setViewNode = setViewNodeResult.get();
                setViewNode.setValue(createSetModelNode(setInfo));
            } else {
                var setModelNode = createSetModelNode(setInfo);
                var setViewNode = new TreeItem<>(setModelNode, new ImageView(setIcon));
                namespaceNode.getChildren().add(setViewNode);
            }
        }
    }

    private Optional<TreeItem<SimpleTreeNode>> findViewNodeInChildren(String itemId, TreeItem<SimpleTreeNode> parent) {
        for (TreeItem<SimpleTreeNode> namespacesTreeNode : parent.getChildren()) {
            if (namespacesTreeNode.getValue().getValue().getId().equals(itemId)) {
                return Optional.of(namespacesTreeNode);
            }
        }
        return Optional.empty();
    }

    private SimpleTreeNode createSetModelNode(SetInfo setInfo) {
        return new SimpleTreeNode(
                String.format(
                        "%s (object count: %s, size: %s)",
                        setInfo.name,
                        numberFormat.format(setInfo.objectCount),
                        FileUtil.getSizeString(setInfo.bytesMemory, Locale.US)
                ),
                setInfo
        );
    }

    private SimpleTreeNode createNamespaceModelNode(NamespaceInfo namespaceInfo) {
        return new SimpleTreeNode(
                String.format(
                        "%s (object count: %s, size: %s, memory: %s)",
                        namespaceInfo.name,
                        numberFormat.format(namespaceInfo.getMasterObjects()),
                        FileUtil.getSizeString(namespaceInfo.getUsedBytesDisk(), Locale.US),
                        FileUtil.getSizeString(namespaceInfo.getUsedBytesMemory(), Locale.US)
                ),
                namespaceInfo
        );
    }

    private SimpleTreeNode createRootModelNode(ConnectionInfo connectionInfo) {
        var connectionString = connectionInfo.toString();
        return new SimpleTreeNode(
                connectionString,
                new RootInfo(connectionString)
        );
    }

    private Optional<Tab> getTab(String tabId) {
        for (Tab tab : tabs.getTabs()) {
            if (tab.getId() != null && tab.getId().equals(tabId)) {
                return Optional.of(tab);
            }
        }
        return Optional.empty();
    }

    private static AsyncClient createNewClient() {
        var aerospikeClientResult = ApplicationModel.INSTANCE.createNewAerospikeClient();
        if (aerospikeClientResult.hasError()) {
            new Alert(Alert.AlertType.ERROR, aerospikeClientResult.getError())
                    .showAndWait();
            throw new AerospikeException(aerospikeClientResult.getError());
        }
        return aerospikeClientResult.getData();
    }

}
