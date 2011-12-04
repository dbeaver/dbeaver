/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.entity.properties;

import org.jkiss.dbeaver.ext.IDatabaseEditor;
import org.jkiss.dbeaver.ui.properties.tabbed.PropertiesContributor;
import org.jkiss.dbeaver.ui.properties.tabbed.PropertyTabDescriptor;
import org.jkiss.dbeaver.ui.properties.tabbed.SectionDescriptor;
import org.jkiss.dbeaver.ui.properties.tabbed.StandardPropertiesSection;
import org.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.tabbed.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.edit.DBEObjectTabProvider;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.tree.DBXTreeItem;
import org.jkiss.dbeaver.registry.tree.DBXTreeNode;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.DBIcon;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * PropertyTabDescriptorProvider
 */
public class PropertyTabDescriptorProvider implements ITabDescriptorProvider {

    static final Log log = LogFactory.getLog(PropertyTabDescriptorProvider.class);
    public static final String CONTRIBUTOR_ID = "org.jkiss.dbeaver.core.propertyViewContributor"; //$NON-NLS-1$

    private ISelection curSelection;
    private ITabDescriptor[] curTabs;

    public PropertyTabDescriptorProvider()
    {
    }

    public ITabDescriptor[] getTabDescriptors(IWorkbenchPart part, ISelection selection)
    {
        if (curTabs != null && CommonUtils.equalObjects(curSelection, selection)) {
            return curTabs;
        }
        List<ITabDescriptor> tabList = new ArrayList<ITabDescriptor>();
        makeStandardPropertiesTabs(tabList);
        if (part instanceof IDatabaseEditor) {
            makeDatabaseEditorTabs((IDatabaseEditor)part, tabList);
        }
        curTabs = tabList.toArray(new ITabDescriptor[tabList.size()]);
        curSelection = selection;
        return curTabs;
    }

    private void makeStandardPropertiesTabs(List<ITabDescriptor> tabList)
    {
        tabList.add(new PropertyTabDescriptor(
            PropertiesContributor.CATEGORY_INFO,
            PropertiesContributor.TAB_STANDARD,
            CoreMessages.ui_properties_category_information,
            DBIcon.TREE_INFO.getImage(),
            new SectionDescriptor(PropertiesContributor.SECTION_STANDARD, PropertiesContributor.TAB_STANDARD) {
                public ISection getSectionClass()
                {
                    return new StandardPropertiesSection();
                }
            }));
    }

    private static class NavigatorTabInfo {
        DBNDatabaseNode node;
        DBXTreeNode meta;
        private NavigatorTabInfo(DBNDatabaseNode node)
        {
            this.node = node;
        }
        private NavigatorTabInfo(DBNDatabaseNode node, DBXTreeNode meta)
        {
            this.node = node;
            this.meta = meta;
        }
        public String getName()
        {
            return meta == null ? node.getNodeName() : meta.getChildrenType(node.getObject().getDataSource());
        }
    }

    private void makeDatabaseEditorTabs(IDatabaseEditor part, List<ITabDescriptor> tabList)
    {
        final DBNDatabaseNode node = part.getEditorInput().getTreeNode();

        // Collect tabs from navigator tree model
        final List<NavigatorTabInfo> tabs = new ArrayList<NavigatorTabInfo>();
        DBRRunnableWithProgress tabsCollector = new DBRRunnableWithProgress() {
            public void run(DBRProgressMonitor monitor)
            {
                tabs.addAll(collectNavigatorTabs(monitor, node));
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

        for (NavigatorTabInfo tab : tabs) {
            addNavigatorNodeTab(part, tabList, tab);
        }

        // Query for tab providers
        final DBSObject object = node.getObject();
        final DBEObjectTabProvider tabProvider = DBeaverCore.getInstance().getEditorsRegistry().getObjectManager(object.getClass(), DBEObjectTabProvider.class);
        if (tabProvider != null) {
            final ITabDescriptor[] tabDescriptors = tabProvider.getTabDescriptors(part.getSite().getWorkbenchWindow(), part, object);
            if (!CommonUtils.isEmpty(tabDescriptors)) {
                Collections.addAll(tabList, tabDescriptors);
            }
        }
    }

    private List<NavigatorTabInfo> collectNavigatorTabs(DBRProgressMonitor monitor, DBNNode node)
    {
        List<NavigatorTabInfo> tabs = new ArrayList<NavigatorTabInfo>();

        // Add all nested folders as tabs
        if (node instanceof DBNDataSource && !((DBNDataSource)node).getDataSourceContainer().isConnected()) {
            // Do not add children tabs
        } else if (node != null) {
            try {
                List<? extends DBNNode> children = node.getChildren(monitor);
                if (children != null) {
                    for (DBNNode child : children) {
                        if (child instanceof DBNDatabaseFolder) {
                            monitor.subTask(CoreMessages.ui_properties_task_add_folder + child.getNodeName() + "'"); //$NON-NLS-2$
                            tabs.add(new NavigatorTabInfo((DBNDatabaseFolder)child));
                        }
                    }
                }
            } catch (DBException e) {
                log.error("Error initializing property tabs", e); //$NON-NLS-1$
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
                                    monitor.subTask(CoreMessages.ui_properties_task_add_node + node.getNodeName() + "'"); //$NON-NLS-2$
                                    tabs.add(new NavigatorTabInfo((DBNDatabaseNode)node, child));
                                }
                            } catch (DBException e) {
                                log.debug("Can't add child items tab", e); //$NON-NLS-1$
                            }
                        }
                    }
                }
            }
        }
        return tabs;
    }

    private void addNavigatorNodeTab(final IDatabaseEditor part, List<ITabDescriptor> tabList, final NavigatorTabInfo tabInfo)
    {
/*
        try {
            EntityNodeEditor nodeEditor = new EntityNodeEditor(tabInfo.node, tabInfo.meta);
            int index = addPage(nodeEditor, getEditorInput());
            if (tabInfo.meta == null) {
                setPageText(index, tabInfo.node.getNodeName());
                setPageImage(index, tabInfo.node.getNodeIconDefault());
                setPageToolTip(index, getEditorInput().getTreeNode().getNodeType() + " " + tabInfo.node.getNodeName());
            } else {
                setPageText(index, tabInfo.meta.getChildrenType());
                if (tabInfo.meta.getDefaultIcon() != null) {
                    setPageImage(index, tabInfo.meta.getDefaultIcon());
                } else {
                    setPageImage(index, PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER));
                }
                setPageToolTip(index, tabInfo.meta.getChildrenType());
            }
            editorMap.put("node." + tabInfo.getName(), nodeEditor);
        } catch (PartInitException ex) {
            log.error("Error adding nested editor", ex);
        }
*/
        tabList.add(new PropertyTabDescriptor(
            PropertiesContributor.CATEGORY_STRUCT,
            tabInfo.getName(),
            tabInfo.getName(),
            tabInfo.node.getNodeIconDefault(),
            new SectionDescriptor("default", tabInfo.getName()) { //$NON-NLS-1$
                public ISection getSectionClass()
                {
                    return new NodeEditorSection(part, tabInfo.node, tabInfo.meta);
                }
            }));
    }

}
