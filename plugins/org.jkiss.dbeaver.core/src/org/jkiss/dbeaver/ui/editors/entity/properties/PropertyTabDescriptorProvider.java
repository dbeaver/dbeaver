/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.editors.entity.properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.tabbed.ISection;
import org.eclipse.ui.views.properties.tabbed.ITabDescriptor;
import org.eclipse.ui.views.properties.tabbed.ITabDescriptorProvider;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDatabaseEditor;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.EntityEditorDescriptor;
import org.jkiss.dbeaver.registry.tree.DBXTreeItem;
import org.jkiss.dbeaver.registry.tree.DBXTreeNode;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.properties.tabbed.PropertiesContributor;
import org.jkiss.dbeaver.ui.properties.tabbed.PropertyTabDescriptor;
import org.jkiss.dbeaver.ui.properties.tabbed.SectionDescriptor;
import org.jkiss.dbeaver.ui.properties.tabbed.StandardPropertiesSection;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
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

    @Override
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
                @Override
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
        final DBSObject object = node.getObject();

        // Collect tabs from navigator tree model
        final List<NavigatorTabInfo> tabs = new ArrayList<NavigatorTabInfo>();
        DBRRunnableWithProgress tabsCollector = new DBRRunnableWithProgress() {
            @Override
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

        // Query for entity editors
        List<EntityEditorDescriptor> editors = DBeaverCore.getInstance().getEditorsRegistry().getEntityEditors(object, null);
        if (!CommonUtils.isEmpty(editors)) {
            for (EntityEditorDescriptor descriptor : editors) {
                if (descriptor.getType() == EntityEditorDescriptor.Type.section) {
                    tabList.add(new EditorTabDescriptor(part, descriptor));
                }
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
        tabList.add(new PropertyTabDescriptor(
            PropertiesContributor.CATEGORY_STRUCT,
            tabInfo.getName(),
            tabInfo.getName(),
            tabInfo.node.getNodeIconDefault(),
            new SectionDescriptor(PropertiesContributor.SECTION_STANDARD, tabInfo.getName()) { //$NON-NLS-1$
                @Override
                public ISection getSectionClass()
                {
                    return new NodeEditorSection(part, tabInfo.node, tabInfo.meta);
                }
            }));
    }

}
