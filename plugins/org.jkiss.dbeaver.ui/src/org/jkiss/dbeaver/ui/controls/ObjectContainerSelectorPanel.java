/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UIMessages;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * ObjectContainerSelectorPanel
 */
public abstract class ObjectContainerSelectorPanel extends Composite
{
    private static final Log log = Log.getLog(ObjectContainerSelectorPanel.class);
    public static final int MAX_HISTORY_LENGTH = 20;

    private final DBPProject project;
    private final String selectorId;
    private final Label containerIcon;
    private final Combo containerNameCombo;

    private final List<HistoryItem> historyItems = new ArrayList<>();
    private final ToolItem browseButton;

    private static class HistoryItem {
        private String containerName;
        private String containerPath;
        private String dataSourceName;
        private DBNDatabaseNode containerNode;

        HistoryItem(String containerName, String containerPath, String dataSourceName, DBNDatabaseNode node) {
            this.containerName = containerName;
            this.containerPath = containerPath;
            this.dataSourceName = dataSourceName;
            this.containerNode = node;
        }

        public String getFullName() {
            String dsName = containerNode != null ? containerNode.getDataSourceContainer().getName() : dataSourceName;
            if (CommonUtils.equalObjects(dsName, containerName)) {
                return containerName;
            }
            return containerName + "  [" + dsName + "]";
        }

        @Override
        public String toString() {
            return getFullName();
        }

        public boolean isSameNode(DBNDatabaseNode node) {
            return containerPath.equals(node.getNodeUri());
        }
    }

    protected ObjectContainerSelectorPanel(Composite parent, DBPProject project, String selectorId, String containerTitle, String containerHint) {
        super(parent, SWT.NONE);

        this.project = project;
        this.selectorId = selectorId;

        GridLayout layout = new GridLayout(4, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        this.setLayout(layout);
        this.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        UIUtils.createControlLabel(this, containerTitle);

        containerIcon = new Label(this, SWT.NONE);
        containerIcon.setImage(DBeaverIcons.getImage(DBIcon.TYPE_UNKNOWN));

        containerNameCombo = new Combo(this, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
        containerNameCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        containerNameCombo.setText("");
        if (containerHint != null) {
            UIUtils.addEmptyTextHint(containerNameCombo, text -> containerHint);
        }
        containerNameCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleContainerChange();
            }
        });

        ToolBar buttonToolbar = new ToolBar(this, SWT.FLAT | SWT.RIGHT);
        browseButton = new ToolItem(buttonToolbar, SWT.NONE);
        browseButton.setImage(DBeaverIcons.getImage(DBIcon.TREE_FOLDER));
        browseButton.setText(UIMessages.browse_button_choose);
        browseButton.setToolTipText(UIMessages.browse_button_choose_tooltip);
        Runnable containerSelector = () -> {
            if (project != null) {
                final DBNModel navigatorModel = DBWorkbench.getPlatform().getNavigatorModel();
                final DBNProject rootNode = navigatorModel.getRoot().getProjectNode(project);
                DBNNode selectedNode = getSelectedNode();
                DBNNode node = DBWorkbench.getPlatformUI().selectObject(
                    getShell(),
                    containerHint != null ? containerHint : containerTitle,
                    rootNode.getDatabases(),
                    selectedNode,
                    new Class[]{ DBSInstance.class, DBSObjectContainer.class },
                    new Class[] { DBSObjectContainer.class },
                    new Class[]{ DBSSchema.class });
                if (node != null) {
                    try {
                        checkValidContainerNode(node);
                        setSelectedNode((DBNDatabaseNode) node);
                        addNodeToHistory((DBNDatabaseNode) node);
                        saveHistory();
                    } catch (DBException e) {
                        DBWorkbench.getPlatformUI().showError(UIMessages.bad_container_node,
                            NLS.bind(UIMessages.bad_container_node_message, node.getName()), e);
                    }
                }
                updateToolTips();
            }
        };
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                containerSelector.run();
            }
        });
        containerNameCombo.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(MouseEvent e) {
                containerSelector.run();
            }
        });

        loadHistory();

        updateToolTips();
    }

    public void checkValidContainerNode(DBNNode node) throws DBException
    {
        if (node instanceof DBNDatabaseNode) {
            DBPObject nodeObject = DBUtils.getPublicObject(((DBNDatabaseNode) node).getObject());
            if (nodeObject instanceof DBSObjectContainer) {
                try {
                    Class<?> childrenClass = ((DBSObjectContainer) nodeObject).getPrimaryChildType(null);
                    if (childrenClass != null) {
                        if (!DBSEntity.class.isAssignableFrom(childrenClass)) {
                            // Upper level of container
                            throw new DBException("You can select only table container (e.g. schema).");
                        }
                    } else {
                        throw new DBException("Can't determine container child objects for " + nodeObject);
                    }
                } catch (DBException e) {
                    throw new DBException("Error determining container elements type", e);
                }
            }
        } else {
            throw new DBException("Non-database node " + node);
        }
    }

    private HistoryItem addNodeToHistory(DBNDatabaseNode node) {
        for (int i = 0; i < historyItems.size(); i++) {
            HistoryItem item = historyItems.get(i);
            if (item.isSameNode(node)) {
                item.containerNode = node;
                moveHistoryItemToBeginning(item);
                return item;
            }
        }
        HistoryItem newItem = new HistoryItem(
            node.getNodeFullName(),
            node.getNodeUri(),
            node.getDataSourceContainer().getName(),
            node
        );
        historyItems.add(0, newItem);
        containerNameCombo.add(newItem.getFullName(), 0);
        return newItem;
    }

    private void moveHistoryItemToBeginning(HistoryItem item) {
        int itemIndex = historyItems.indexOf(item);
        historyItems.remove(item);
        historyItems.add(0, item);

        containerNameCombo.remove(itemIndex);
        containerNameCombo.add(item.getFullName(), 0);
        containerNameCombo.select(0);
    }

    private void handleContainerChange() {
        int historyIndex = containerNameCombo.getSelectionIndex();
        if (historyIndex >= 0 && historyIndex < historyItems.size()) {
            HistoryItem historyItem = historyItems.get(historyIndex);
            if (historyItem.containerNode == null) {
                // Load node
                try {
                    UIUtils.runInProgressDialog(monitor -> {
                        try {
                            DBNNode node = DBWorkbench.getPlatform().getNavigatorModel().getNodeByPath(monitor, project, historyItem.containerPath);
                            if (node instanceof DBNDatabaseNode) {
                                historyItem.containerNode = (DBNDatabaseNode) node;
                            }
                        } catch (DBException e) {
                            throw new InvocationTargetException(e);
                        }
                    });
                } catch (InvocationTargetException e) {
                    DBWorkbench.getPlatformUI().showError("Bad container path", "Can't find database node by path " + historyItem.containerPath, e.getTargetException());
                }
            }
            if (historyItem.containerNode != null) {
                setSelectedNode(historyItem.containerNode);
                moveHistoryItemToBeginning(historyItem);
            } else {
                historyItems.remove(historyIndex);
                containerNameCombo.remove(historyIndex);
            }
        }
        updateToolTips();
        //setSelectedNode(node);
    }

    private void updateToolTips() {
        DBNNode selectedNode = getSelectedNode();
        if (selectedNode instanceof DBNDatabaseNode node) {
            browseButton.setToolTipText(
                NLS.bind(
                    UIMessages.label_choose,
                    UIUtils.getCatalogSchemaTerms(node.getDataSourceContainer(), true)));
        } else {
            browseButton.setToolTipText(UIMessages.browse_button_choose_tooltip);
        }
    }

    private void loadHistory() {
        IDialogSettings historySection = UIUtils.getDialogSettings("ObjectContainerSelector");
        IDialogSettings projectSection = historySection.getSection(project.getName());
        if (projectSection != null) {
            IDialogSettings selectorSection = projectSection.getSection(selectorId);
            if (selectorSection != null) {
                for (int i = 1; i < MAX_HISTORY_LENGTH; i++) {
                    IDialogSettings itemSection = selectorSection.getSection("item" + i);
                    if (itemSection == null) {
                        break;
                    }
                    historyItems.add(new HistoryItem(
                        itemSection.get("name"),
                        itemSection.get("path"),
                        itemSection.get("data-source"),
                        null));
                }
            }
        }

        for (HistoryItem item : historyItems) {
            containerNameCombo.add(item.getFullName());
        }
    }

    private void saveHistory() {
        IDialogSettings selectorHistorySection = UIUtils.getDialogSettings("ObjectContainerSelector");
        IDialogSettings projectSection = UIUtils.getSettingsSection(selectorHistorySection, project.getName());
        IDialogSettings selectorSection = projectSection.addNewSection(selectorId);
        for (int i = 0; i < historyItems.size(); i++) {
            HistoryItem item = historyItems.get(i);
            IDialogSettings itemSection = selectorSection.addNewSection("item" + (i + 1));
            itemSection.put("name", item.containerName);
            itemSection.put("path", item.containerPath);
            itemSection.put("data-source", item.dataSourceName);
        }

    }

    public void setContainerInfo(DBNDatabaseNode node) {
        if (node == null) {
            containerIcon.setImage(DBeaverIcons.getImage(DBIcon.TYPE_UNKNOWN));
            containerNameCombo.select(-1);
            return;
        }
        HistoryItem item = addNodeToHistory(node);
        containerIcon.setImage(DBeaverIcons.getImage(node.getNodeIconDefault()));

        moveHistoryItemToBeginning(item);
    }

    private void removeItemFromCombo(HistoryItem item) {
        int itemCount = containerNameCombo.getItemCount();
        for (int i = 0; i < itemCount; i++) {
            if (containerNameCombo.getItem(i).equals(item.getFullName())) {
                containerNameCombo.remove(i);
                break;
            }
        }
    }

    protected abstract void setSelectedNode(DBNDatabaseNode node);

    @Nullable
    protected abstract DBNNode getSelectedNode();
}
