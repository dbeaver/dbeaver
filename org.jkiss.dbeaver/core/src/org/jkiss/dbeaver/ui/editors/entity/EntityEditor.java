/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.entity;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.ui.IDatabaseObjectEditor;
import org.jkiss.dbeaver.ext.IDatabaseObjectManager;
import org.jkiss.dbeaver.ext.ui.IMetaModelView;
import org.jkiss.dbeaver.ext.ui.IRefreshablePart;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.EntityEditorDescriptor;
import org.jkiss.dbeaver.registry.EntityEditorsRegistry;
import org.jkiss.dbeaver.registry.EntityManagerDescriptor;
import org.jkiss.dbeaver.registry.tree.DBXTreeItem;
import org.jkiss.dbeaver.registry.tree.DBXTreeNode;
import org.jkiss.dbeaver.ui.editors.MultiPageDatabaseEditor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EntityEditor
 */
public class EntityEditor extends MultiPageDatabaseEditor<EntityEditorInput> implements IDBNListener, IMetaModelView
{
    static final Log log = LogFactory.getLog(EntityEditor.class);

    private IDatabaseObjectManager objectManager;
    private Map<String, IEditorPart> editorMap = new HashMap<String, IEditorPart>();

    protected void createPages()
    {
        super.createPages();

        EntityEditorsRegistry editorsRegistry = DBeaverCore.getInstance().getEditorsRegistry();
        DBSObject databaseObject = getEditorInput().getDatabaseObject();

        // Instantiate object manager
        EntityManagerDescriptor managerDescriptor = editorsRegistry.getEntityManager(databaseObject.getClass());
        if (managerDescriptor != null) {
            this.objectManager = managerDescriptor.createMannager();
            if (this.objectManager == null) {
                log.warn("Could not instantiate object manager '" + managerDescriptor.getName() + "'");
            }
        }
        if (this.objectManager == null) {
            this.objectManager = new DefaultDatabaseObjectManager();
        }

        try {
            this.objectManager.init(databaseObject);
        } catch (DBException e) {
            log.error("Could not initialize object manager", e);
        }

        // Add object editor page
        EntityEditorDescriptor defaultEditor = editorsRegistry.getMainEntityEditor(databaseObject.getClass());
        boolean mainAdded = false;
        if (defaultEditor != null) {
            mainAdded = addEditorTab(defaultEditor);
        }
        if (!mainAdded) {
            try {
                DBNNode node = getEditorInput().getTreeNode();
                int index = addPage(new DefaultObjectEditor(node), getEditorInput());
                setPageText(index, "Properties");
                if (node instanceof DBNTreeNode) {
                    setPageToolTip(index, ((DBNTreeNode)node).getMeta().getLabel() + " Properties");
                }
                setPageImage(index, node.getNodeIconDefault());
            } catch (PartInitException e) {
                log.error("Error creating object editor");
            }
        }

        // Add contributed pages
        addContributions(EntityEditorDescriptor.POSITION_START);

        final List<TabInfo> tabs = new ArrayList<TabInfo>();
        DBeaverCore.getInstance().runAndWait(true, true, new DBRRunnableWithProgress() {
            public void run(DBRProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException
            {
                tabs.addAll(collectTabs(monitor));
            }
        });

        for (TabInfo tab : tabs) {
            if (tab.meta == null) {
                addNodeTab(tab.node);
            } else {
                addNodeTab(tab.node, tab.meta);
            }
        }

        // Add contributed pages
        addContributions(EntityEditorDescriptor.POSITION_END);

        String defPageId = getEditorInput().getDefaultPageId();
        if (defPageId != null) {
            IEditorPart defEditorPage = editorMap.get(defPageId);
            if (defEditorPage != null) {
                setActiveEditor(defEditorPage);
            }
        }
    }

    private static class TabInfo {
        DBNNode node;
        DBXTreeNode meta;
        private TabInfo(DBNNode node)
        {
            this.node = node;
        }
        private TabInfo(DBNNode node, DBXTreeNode meta)
        {
            this.node = node;
            this.meta = meta;
        }
    }

    private List<TabInfo> collectTabs(DBRProgressMonitor monitor)
    {
        List<TabInfo> tabs = new ArrayList<TabInfo>();

        // Add all nested folders as tabs
        DBNNode node = getEditorInput().getTreeNode();
        try {
            List<? extends DBNNode> children = node.getChildren(monitor);
            if (children != null) {
                for (DBNNode child : children) {
                    if (child instanceof DBNTreeFolder) {
                        tabs.add(new TabInfo(child));
                    }
                }
            }
        } catch (DBException e) {
            log.error("Error initializing entity editor");
        }
        // Add itself as tab (if it has child items)
        if (node instanceof DBNTreeNode) {
            DBNTreeNode treeNode = (DBNTreeNode)node;
            List<DBXTreeNode> subNodes = treeNode.getMeta().getChildren();
            if (subNodes != null) {
                for (DBXTreeNode child : subNodes) {
                    if (child instanceof DBXTreeItem) {
                        try {
                            if (!((DBXTreeItem)child).isOptional() || treeNode.hasChildren(monitor, child)) {
                                tabs.add(new TabInfo(node, child));
                            }
                        } catch (DBException e) {
                            log.error("Can't add child items tab", e);
                        }
                    }
                }
            }
        }
        return tabs;
    }

    private void addContributions(String position)
    {
        EntityEditorsRegistry editorsRegistry = DBeaverCore.getInstance().getEditorsRegistry();
        List<EntityEditorDescriptor> descriptors = editorsRegistry.getEntityEditors(getEditorInput().getDatabaseObject().getClass(), position);
        for (EntityEditorDescriptor descriptor : descriptors) {
            addEditorTab(descriptor);
        }
    }

    private boolean addEditorTab(EntityEditorDescriptor descriptor)
    {
        try {
            IEditorPart editor = descriptor.createEditor();
            if (editor == null) {
                return false;
            }
            Object object = this.objectManager.getObject();
            if (editor instanceof IDatabaseObjectEditor) {
                try {
                    Method initMethod = editor.getClass().getMethod("initObjectEditor", IDatabaseObjectManager.class);
                    Type initParam = initMethod.getGenericParameterTypes()[0];
                    if (initParam instanceof ParameterizedType) {
                        Type typeArgument = ((ParameterizedType) initParam).getActualTypeArguments()[0];
                        if (typeArgument instanceof Class && !((Class<?>) typeArgument).isAssignableFrom(object.getClass())) {
                            // Bad parameter type
                            log.error(descriptor.getName() + " editor misconfiguration - invalid object type '" + object.getClass().getName() + "' specified while '" + ((Class<?>) typeArgument).getName() + "' was expected");
                            return false;
                        }
                    }
                } catch (Throwable e) {
                    log.error(e);
                    return false;
                }
                ((IDatabaseObjectEditor)editor).initObjectEditor(this.objectManager);
            }
            int index = addPage(editor, getEditorInput());
            setPageText(index, descriptor.getName());
            if (descriptor.getIcon() != null) {
                setPageImage(index, descriptor.getIcon());
            }
            if (!CommonUtils.isEmpty(descriptor.getDescription())) {
                setPageToolTip(index, descriptor.getDescription());
            }
            editorMap.put(descriptor.getId(), editor);
            return true;
        } catch (Exception ex) {
            log.error("Error adding nested editor", ex);
            return false;
        }
    }

    private void addNodeTab(DBNNode node)
    {
        try {
            EntityNodeEditor nodeEditor = new EntityNodeEditor(node);
            int index = addPage(nodeEditor, getEditorInput());
            setPageText(index, node.getNodeName());
            setPageImage(index, node.getNodeIconDefault());
            if (getEditorInput().getTreeNode() instanceof DBNTreeNode) {
                setPageToolTip(index, ((DBNTreeNode)getEditorInput().getTreeNode()).getMeta().getLabel() + " " + node.getNodeName());
            }
            editorMap.put("node." + node.getNodeName(), nodeEditor);
        } catch (PartInitException ex) {
            log.error("Error adding nested editor", ex);
        }
    }

    private void addNodeTab(DBNNode node, DBXTreeNode metaItem)
    {
        try {
            EntityNodeEditor nodeEditor = new EntityNodeEditor(node, metaItem);
            int index = addPage(nodeEditor, getEditorInput());
            setPageText(index, metaItem.getLabel());
            if (metaItem.getDefaultIcon() != null) {
                setPageImage(index, metaItem.getDefaultIcon());
            } else {
                setPageImage(index, PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER));
            }
            setPageToolTip(index, metaItem.getLabel());
            editorMap.put("node." + node.getNodeName(), nodeEditor);
        } catch (PartInitException ex) {
            log.error("Error adding nested editor", ex);
        }
    }

    protected void refreshContent(final DBNEvent event)
    {
        int pageCount = getPageCount();
        for (int i = 0; i < pageCount; i++) {
            IWorkbenchPart part = getEditor(i);
            if (part instanceof IRefreshablePart) {
                ((IRefreshablePart)part).refreshPart(event);
            }
        }
        setTitleImage(getEditorInput().getImageDescriptor().createImage());
    }

    public DBNModel getMetaModel()
    {
        return getEditorInput().getTreeNode().getModel();
    }

    public Viewer getViewer()
    {
        IWorkbenchPart activePart = getActiveEditor();
        if (activePart instanceof IMetaModelView) {
            return ((IMetaModelView)activePart).getViewer();
        }
        return null;
    }

}
