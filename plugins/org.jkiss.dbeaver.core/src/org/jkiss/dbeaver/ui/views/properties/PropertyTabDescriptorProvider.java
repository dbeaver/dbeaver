/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.tabbed.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditor;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.registry.tree.DBXTreeItem;
import org.jkiss.dbeaver.registry.tree.DBXTreeNode;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * PropertyTabDescriptorProvider
 */
public class PropertyTabDescriptorProvider implements ITabDescriptorProvider {

    static final Log log = LogFactory.getLog(PropertyTabDescriptorProvider.class);

    public ITabDescriptor[] getTabDescriptors(IWorkbenchPart part, ISelection selection)
    {
        List<ITabDescriptor> tabList = new ArrayList<ITabDescriptor>();
        makeStandardPropertiesTabs(part, selection, tabList);
        if (part instanceof IDatabaseNodeEditor) {
            makeDatabaseEditorTabs((IDatabaseNodeEditor)part, selection, tabList);
        }
        return tabList.toArray(new ITabDescriptor[tabList.size()]);
    }

    private void makeStandardPropertiesTabs(IWorkbenchPart part, ISelection selection, List<ITabDescriptor> tabList)
    {
        List<ISectionDescriptor> standardSections = new ArrayList<ISectionDescriptor>();
        standardSections.add(new AbstractSectionDescriptor() {
            public String getId()
            {
                return PropertiesContributor.SECTION_STANDARD;
            }

            public ISection getSectionClass()
            {
                return new PropertySectionStandard();
            }

            public String getTargetTab()
            {
                return PropertiesContributor.TAB_STANDARD;
            }

            @Override
            public boolean appliesTo(IWorkbenchPart part, ISelection selection)
            {
                return true;
            }
        });
        tabList.add(new PropertyTabDescriptor(
            PropertiesContributor.CATEGORY_MAIN,
            PropertiesContributor.TAB_STANDARD,
            "Information",
            standardSections));
    }

    private static class TabInfo {
        DBNDatabaseNode node;
        DBXTreeNode meta;
        private TabInfo(DBNDatabaseNode node)
        {
            this.node = node;
        }
        private TabInfo(DBNDatabaseNode node, DBXTreeNode meta)
        {
            this.node = node;
            this.meta = meta;
        }
        public String getName()
        {
            return meta == null ? node.getNodeName() : meta.getLabel();
        }
    }

    private void makeDatabaseEditorTabs(IDatabaseNodeEditor part, ISelection selection, List<ITabDescriptor> tabList)
    {
        final DBNDatabaseNode node = part.getEditorInput().getTreeNode();

        // Collect tabs from navigator tree model
        final List<TabInfo> tabs = new ArrayList<TabInfo>();
        DBRRunnableWithProgress tabsCollector = new DBRRunnableWithProgress() {
            public void run(DBRProgressMonitor monitor)
            {
                tabs.addAll(collectTabs(monitor, node));
            }
        };
        try {
            if (node.isLazyNode()) {
                DBeaverCore.getInstance().runInProgressService(tabsCollector);
            } else {
                tabsCollector.run(VoidProgressMonitor.INSTANCE);
            }
        } catch (InvocationTargetException e) {
            log.error(e.getTargetException());
        } catch (InterruptedException e) {
            // just go further
        }

        for (TabInfo tab : tabs) {
            addNodeTab(part, tabList, tab);
        }
    }

    private List<TabInfo> collectTabs(DBRProgressMonitor monitor, DBNNode node)
    {
        List<TabInfo> tabs = new ArrayList<TabInfo>();

        // Add all nested folders as tabs
        if (node instanceof DBNDataSource && !((DBNDataSource)node).getDataSourceContainer().isConnected()) {
            // Do not add children tabs
        } else if (node != null) {
            try {
                List<? extends DBNNode> children = node.getChildren(monitor);
                if (children != null) {
                    for (DBNNode child : children) {
                        if (child instanceof DBNDatabaseFolder) {
                            monitor.subTask("Add folder '" + child.getNodeName() + "'");
                            tabs.add(new TabInfo((DBNDatabaseFolder)child));
                        }
                    }
                }
            } catch (DBException e) {
                log.error("Error initializing property tabs", e);
            }
            // Add itself as tab (if it has child items)
            if (node instanceof DBNDatabaseNode) {
                DBNDatabaseNode databaseNode = (DBNDatabaseNode)node;
                List<DBXTreeNode> subNodes = databaseNode.getMeta().getChildren(databaseNode);
                if (subNodes != null) {
                    for (DBXTreeNode child : subNodes) {
                        if (child instanceof DBXTreeItem) {
                            try {
                                if (!((DBXTreeItem)child).isOptional() || databaseNode.hasChildren(monitor, child)) {
                                    monitor.subTask("Add node '" + node.getNodeName() + "'");
                                    tabs.add(new TabInfo((DBNDatabaseNode)node, child));
                                }
                            } catch (DBException e) {
                                log.debug("Can't add child items tab", e);
                            }
                        }
                    }
                }
            }
        }
        return tabs;
    }

    private void addNodeTab(final IDatabaseNodeEditor part, List<ITabDescriptor> tabList, final TabInfo tabInfo)
    {
        List<ISectionDescriptor> tabSections = new ArrayList<ISectionDescriptor>();

/*
        try {
            EntityNodeEditor nodeEditor = new EntityNodeEditor(tabInfo.node, tabInfo.meta);
            int index = addPage(nodeEditor, getEditorInput());
            if (tabInfo.meta == null) {
                setPageText(index, tabInfo.node.getNodeName());
                setPageImage(index, tabInfo.node.getNodeIconDefault());
                setPageToolTip(index, getEditorInput().getTreeNode().getNodeType() + " " + tabInfo.node.getNodeName());
            } else {
                setPageText(index, tabInfo.meta.getLabel());
                if (tabInfo.meta.getDefaultIcon() != null) {
                    setPageImage(index, tabInfo.meta.getDefaultIcon());
                } else {
                    setPageImage(index, PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER));
                }
                setPageToolTip(index, tabInfo.meta.getLabel());
            }
            editorMap.put("node." + tabInfo.getName(), nodeEditor);
        } catch (PartInitException ex) {
            log.error("Error adding nested editor", ex);
        }
*/
        tabSections.add(new AbstractSectionDescriptor() {
            public String getId()
            {
                return "default";
            }

            public ISection getSectionClass()
            {
                return new NodeEditorSection(part, tabInfo.node, tabInfo.meta);
            }

            public String getTargetTab()
            {
                return tabInfo.getName();
            }

            @Override
            public boolean appliesTo(IWorkbenchPart part, ISelection selection)
            {
                return true;
            }
        });
        tabList.add(new PropertyTabDescriptor(
            PropertiesContributor.CATEGORY_MAIN,
            tabInfo.getName(),
            tabInfo.getName(),
            tabSections));
    }

}
