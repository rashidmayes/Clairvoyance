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

    private final Image rootIcon;
    // TODO: 11/06/2023 fix this icon
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
        this.rootIcon = new Image(rootIconImage);

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
        ApplicationModel.INSTANCE.runInBackground(FileUtil::clearCache);
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
        buildRoot(client);
    }

    private void buildRoot(AerospikeClient client) {
        var rootNode = createRootModelNode(ApplicationModel.INSTANCE.getConnectionInfo());
        if (namespacesTree.getRoot() == null) {
            var treeRootView = new TreeItem<>(rootNode, new ImageView(rootIcon));
            treeRootView.setExpanded(true);
            namespacesTree.setRoot(treeRootView);
        } else {
            namespacesTree.getRoot().setValue(rootNode);
        }
        buildNodes(client);
    }

    private void buildNodes(AerospikeClient client) {
        for (Node node : client.getNodes()) {
            buildNode(node);
        }
    }

    private void buildNode(Node node) {
        var nodeInfo = nodeInfoMapper.getNodeInfo(node);
        var nodeViewNodeResult = findViewNodeInChildren(nodeInfo.getId(), namespacesTree.getRoot());
        if (nodeViewNodeResult.isPresent()) {
            var nodeModelNode = createNodeModelNode(nodeInfo);
            var nodeViewNode = nodeViewNodeResult.get();
            nodeViewNode.setValue(nodeModelNode);

            buildNamespaces(nodeInfo, nodeViewNode);
        } else {
            var nodeModelNode = createNodeModelNode(nodeInfo);
            // todo: uncomment after fixing icon
            var nodeViewNode = new TreeItem<>(nodeModelNode /*, new ImageView(nodeIcon)*/);
            nodeViewNode.setExpanded(true);
            namespacesTree.getRoot().getChildren().add(nodeViewNode);

            buildNamespaces(nodeInfo, nodeViewNode);
        }
    }

    private void buildNamespaces(NodeInfo nodeInfo, TreeItem<SimpleTreeNode> parent) {
        for (NamespaceInfo namespace : nodeInfo.getNamespaces()) {
            buildNamespaceNode(parent, namespace);
        }
    }

    private void buildNamespaceNode(TreeItem<SimpleTreeNode> parent, NamespaceInfo namespace) {
        var namespaceViewNodeResult = findViewNodeInChildren(namespace.getId(), parent);
        var namespaceModelNode = createNamespaceModelNode(namespace);
        if (namespaceViewNodeResult.isPresent()) {
            var namespaceViewNode = namespaceViewNodeResult.get();
            namespaceViewNode.setValue(namespaceModelNode);

            buildSets(namespace, namespaceViewNode);
        } else {
            var namespaceViewNode = new TreeItem<>(namespaceModelNode, new ImageView(namespaceIcon));
            namespaceViewNode.setExpanded(true);
            parent.getChildren().add(namespaceViewNode);

            buildSets(namespace, namespaceViewNode);
        }
    }

    private void buildSets(NamespaceInfo namespace, TreeItem<SimpleTreeNode> namespaceNode) {
        for (SetInfo setInfo : namespace.getSets()) {
            buildSetNode(namespaceNode, setInfo);
        }
    }

    private void buildSetNode(TreeItem<SimpleTreeNode> namespaceNode, SetInfo setInfo) {
        var setViewNodeResult = findViewNodeInChildren(setInfo.getId(), namespaceNode);
        var setModelNode = createSetModelNode(setInfo);
        if (setViewNodeResult.isPresent()) {
            var setViewNode = setViewNodeResult.get();
            setViewNode.setValue(setModelNode);
        } else {
            var setViewNode = new TreeItem<>(setModelNode, new ImageView(setIcon));
            namespaceNode.getChildren().add(setViewNode);
        }
    }

    private Optional<TreeItem<SimpleTreeNode>> findViewNodeInChildren(String itemId, TreeItem<SimpleTreeNode> parent) {
        for (var namespacesTreeNode : parent.getChildren()) {
            if (namespacesTreeNode.getValue().getValue().getId().equals(itemId)) {
                return Optional.of(namespacesTreeNode);
            }
        }
        return Optional.empty();
    }

    private SimpleTreeNode createRootModelNode(ConnectionInfo connectionInfo) {
        var connectionString = connectionInfo.toString();
        return new SimpleTreeNode(
                connectionString,
                new RootInfo(connectionString)
        );
    }

    private SimpleTreeNode createNodeModelNode(NodeInfo nodeInfo) {
        return new SimpleTreeNode(nodeInfo.getName(), nodeInfo);
    }

    private SimpleTreeNode createNamespaceModelNode(NamespaceInfo namespaceInfo) {
        return new SimpleTreeNode(namespaceInfo.getName(), namespaceInfo);
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

    private Optional<Tab> getTab(String tabId) {
        for (var tab : tabs.getTabs()) {
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
