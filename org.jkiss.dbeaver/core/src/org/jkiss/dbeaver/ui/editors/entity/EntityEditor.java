/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.entity;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.ui.*;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.ui.IDataSourceUser;
import org.jkiss.dbeaver.ext.ui.IMetaModelView;
import org.jkiss.dbeaver.ext.ui.IObjectEditor;
import org.jkiss.dbeaver.ext.ui.IRefreshablePart;
import org.jkiss.dbeaver.ext.ui.IEmbeddedWorkbenchPart;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.meta.*;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.registry.EntityEditorDescriptor;
import org.jkiss.dbeaver.registry.EntityEditorsRegistry;
import org.jkiss.dbeaver.registry.tree.DBXTreeItem;
import org.jkiss.dbeaver.registry.tree.DBXTreeNode;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.InvocationTargetException;

/**
 * EntityEditor
 */
public class EntityEditor extends MultiPageEditorPart implements IDBMListener, IMetaModelView, IDataSourceUser
{
    static final Log log = LogFactory.getLog(EntityEditor.class);

    private EntityEditorInput entityInput;
    private Map<String, IEditorPart> editorMap = new HashMap<String, IEditorPart>();

    public void doSave(IProgressMonitor monitor)
    {
    }

    public void doSaveAs()
    {
    }

    public void init(IEditorSite site, IEditorInput input)
        throws PartInitException
    {
        super.init(site, input);
        this.entityInput = (EntityEditorInput) input;
        setPartName(this.entityInput.getName());
        setTitleImage(this.entityInput.getImageDescriptor().createImage());

        DBeaverCore.getInstance().getMetaModel().addListener(this);
    }

    public void dispose()
    {
        DBeaverCore.getInstance().getMetaModel().removeListener(this);
        super.dispose();
    }

    public boolean isDirty()
    {
        return false;
    }

    public boolean isSaveAsAllowed()
    {
        return false;
    }

    protected int getContainerStyle()
    {
        return SWT.TOP | SWT.FLAT | SWT.BORDER;
    }

    protected int getContainerMargin()
    {
        return 5;
    }

    protected void createPages()
    {
        setContainerStyles();

        // Add object editor page
        EntityEditorsRegistry editorsRegistry = DBeaverCore.getInstance().getEditorsRegistry();
        EntityEditorDescriptor defaultEditor = editorsRegistry.getMainEntityEditor(entityInput.getDatabaseObject().getClass());
        boolean mainAdded = false;
        if (defaultEditor != null) {
            mainAdded = addEditorTab(defaultEditor);
        }
        if (!mainAdded) {
            try {
                DBMNode node = entityInput.getNode();
                int index = addPage(new DefaultObjectEditor(node), entityInput);
                setPageText(index, "Properties");
                if (node instanceof DBMTreeNode) {
                    setPageToolTip(index, ((DBMTreeNode)node).getMeta().getLabel() + " Properties");
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

        String defPageId = entityInput.getDefaultPageId();
        if (defPageId != null) {
            IEditorPart defEditorPage = editorMap.get(defPageId);
            if (defEditorPage != null) {
                setActiveEditor(defEditorPage);
            }
        }
    }

    private void setContainerStyles()
    {
        Composite pageContainer = getContainer();
        if (pageContainer instanceof CTabFolder) {
            CTabFolder tabFolder = (CTabFolder)pageContainer;
            tabFolder.setSimple(false);
            tabFolder.setTabPosition(SWT.TOP);
            tabFolder.setBorderVisible(true);
            Layout parentLayout = tabFolder.getParent().getLayout();
            if (parentLayout instanceof FillLayout) {
                ((FillLayout)parentLayout).marginHeight = 5;
                ((FillLayout)parentLayout).marginWidth = 5;
            }
        }
    }

    private void setPageToolTip(int index, String toolTip)
    {
        Composite pageContainer = getContainer();
        if (pageContainer instanceof CTabFolder) {
            CTabFolder tabFolder = (CTabFolder)pageContainer;
            if (index > 0 && index < tabFolder.getItemCount()) {
                tabFolder.getItem(index).setToolTipText(toolTip);
            }
        }
    }

    protected void pageChange(int newPageIndex)
    {
        deactivateEditor();
        super.pageChange(newPageIndex);
        activateEditor();
    }

    protected final void deactivateEditor()
    {
        // Deactivate the nested services from the last active service locator.
        final int pageIndex = getActivePage();
        final IWorkbenchPart part = getEditor(pageIndex);
        if (part instanceof IEmbeddedWorkbenchPart) {
            ((IEmbeddedWorkbenchPart) part).deactivatePart();
        }
    }

    protected final void activateEditor()
    {
        final int pageIndex = getActivePage();
        final IWorkbenchPart part = getEditor(pageIndex);

        if (part instanceof IEmbeddedWorkbenchPart) {
            ((IEmbeddedWorkbenchPart) part).activatePart();
        }
    }

    private static class TabInfo {
        DBMNode node;
        DBXTreeNode meta;
        private TabInfo(DBMNode node)
        {
            this.node = node;
        }
        private TabInfo(DBMNode node, DBXTreeNode meta)
        {
            this.node = node;
            this.meta = meta;
        }
    }

    private List<TabInfo> collectTabs(DBRProgressMonitor monitor)
    {
        List<TabInfo> tabs = new ArrayList<TabInfo>();

        // Add all nested folders as tabs
        DBMNode node = entityInput.getNode();
        try {
            List<? extends DBMNode> children = node.getChildren(monitor);
            if (children != null) {
                for (DBMNode child : children) {
                    if (child instanceof DBMTreeFolder) {
                        tabs.add(new TabInfo(child));
                    }
                }
            }
        } catch (DBException e) {
            log.error("Error initializing entity editor");
        }
        // Add itself as tab (if it has child items)
        if (node instanceof DBMTreeNode) {
            DBMTreeNode treeNode = (DBMTreeNode)node;
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
        List<EntityEditorDescriptor> descriptors = editorsRegistry.getEntityEditors(entityInput.getDatabaseObject().getClass(), position);
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
            if (editor instanceof IObjectEditor) {
                ((IObjectEditor)editor).setObject(entityInput.getDatabaseObject());
            }
            int index = addPage(editor, entityInput);
            setPageText(index, descriptor.getName());
            if (descriptor.getIcon() != null) {
                setPageImage(index, descriptor.getIcon());
            }
            if (!CommonUtils.isEmpty(descriptor.getDescription())) {
                setPageToolTip(index, descriptor.getDescription());
            }
            editorMap.put(descriptor.getId(), editor);
            return true;
        } catch (PartInitException ex) {
            log.error("Error adding nested editor", ex);
            return false;
        }
    }

    private void addNodeTab(DBMNode node)
    {
        try {
            EntityNodeEditor nodeEditor = new EntityNodeEditor(node);
            int index = addPage(nodeEditor, entityInput);
            setPageText(index, node.getNodeName());
            setPageImage(index, node.getNodeIconDefault());
            if (entityInput.getNode() instanceof DBMTreeNode) {
                setPageToolTip(index, ((DBMTreeNode)entityInput.getNode()).getMeta().getLabel() + " " + node.getNodeName());
            }
            editorMap.put("node." + node.getNodeName(), nodeEditor);
        } catch (PartInitException ex) {
            log.error("Error adding nested editor", ex);
        }
    }

    private void addNodeTab(DBMNode node, DBXTreeNode metaItem)
    {
        try {
            EntityNodeEditor nodeEditor = new EntityNodeEditor(node, metaItem);
            int index = addPage(nodeEditor, entityInput);
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

    private void refreshContent(DBMEvent event)
    {
        int pageCount = getPageCount();
        for (int i = 0; i < pageCount; i++) {
            IWorkbenchPart part = getEditor(i);
            if (part instanceof IRefreshablePart) {
                ((IRefreshablePart)part).refreshPart(event);
            }
        }
        setTitleImage(this.entityInput.getImageDescriptor().createImage());
    }

    public void nodeChanged(final DBMEvent event)
    {
        if (event.getNode() == entityInput.getNode()) {
            if (event.getAction() == DBMEvent.Action.REMOVE) {
                getSite().getShell().getDisplay().asyncExec(new Runnable() { public void run() {
                    IWorkbenchPage workbenchPage = getSite().getWorkbenchWindow().getActivePage();
                    if (workbenchPage != null) {
                        workbenchPage.closeEditor(EntityEditor.this, false);
                    }
                }});
            } else if (event.getAction() == DBMEvent.Action.REFRESH) {
                getSite().getShell().getDisplay().asyncExec(new Runnable() { public void run() {
                    refreshContent(event);
                }});
            }
        }
    }

    public DBMModel getMetaModel()
    {
        return entityInput.getNode().getModel();
    }

    public Viewer getViewer()
    {
        IWorkbenchPart activePart = getActiveEditor();
        if (activePart instanceof IMetaModelView) {
            return ((IMetaModelView)activePart).getViewer();
        }
        return null;
    }

    public IWorkbenchPart getWorkbenchPart()
    {
        return this;
    }

    public DBSDataSourceContainer getDataSourceContainer() {
        DBPDataSource dataSource = getDataSource();
        return dataSource == null ? null : dataSource.getContainer();
    }

    public DBPDataSource getDataSource() {
        return entityInput == null || entityInput.getDatabaseObject() == null ? null : entityInput.getDatabaseObject().getDataSource();
    }

}
