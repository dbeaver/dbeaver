/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.entity;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.jkiss.dbeaver.ext.IDatabaseEditor;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.ext.ui.IActiveWorkbenchPart;
import org.jkiss.dbeaver.ext.ui.INavigatorModelView;
import org.jkiss.dbeaver.ext.ui.IRefreshablePart;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.registry.tree.DBXTreeNode;
import org.jkiss.dbeaver.ui.NavigatorUtils;
import org.jkiss.dbeaver.ui.controls.itemlist.ItemListControl;

/**
 * EntityNodeEditor
 */
class EntityNodeEditor extends EditorPart implements IRefreshablePart, INavigatorModelView, IDatabaseEditor, IActiveWorkbenchPart
{
    //static final Log log = LogFactory.getLog(EntityNodeEditor.class);

    private DBNNode node;
    private DBXTreeNode metaNode;
    private ItemListControl itemControl;
    private boolean activated;

    EntityNodeEditor(DBNNode node, DBXTreeNode metaNode)
    {
        this.node = node;
        this.metaNode = metaNode;
    }

    @Override
    public void doSave(IProgressMonitor monitor)
    {
    }

    @Override
    public void doSaveAs()
    {
    }

    @Override
    public void init(IEditorSite site, IEditorInput input)
        throws PartInitException
    {
        setSite(site);
        setInput(input);
    }

    @Override
    public boolean isDirty()
    {
        return false;
    }

    @Override
    public boolean isSaveAsAllowed()
    {
        return false;
    }

    @Override
    public void createPartControl(Composite parent)
    {
        itemControl = new ItemListControl(parent, SWT.NONE, this, node, metaNode);
        itemControl.createProgressPanel();

        // Hook context menu
        NavigatorUtils.addContextMenu(this, itemControl.getSelectionProvider(), itemControl);
        // Add drag and drop support
        NavigatorUtils.addDragAndDropSupport(itemControl.getNavigatorViewer());
        //ISelectionProvider selectionProvider = itemControl.getSelectionProvider();
        getSite().setSelectionProvider(itemControl.getSelectionProvider());
    }

    @Override
    public void setFocus()
    {
    }

    @Override
    public DBNNode getRootNode() {
        return node;
    }

    @Override
    public Viewer getNavigatorViewer()
    {
        return itemControl.getNavigatorViewer();
    }

    @Override
    public void activatePart()
    {
        if (!activated) {
            activated = true;
            itemControl.loadData();
        }
    }

    @Override
    public void deactivatePart()
    {
    }

    @Override
    public void refreshPart(Object source, boolean force)
    {
        if (!activated) {
            return;
        }
        // Check - do we need to load new content in editor
        // If this is DBM event then check node change type
        // UNLOAD usually means that connection was closed on connection's node is not removed but
        // is in "unloaded" state.
        // Without this check editor will try to reload it's content and thus will reopen just closed connection
        // (by calling getChildren() on DBNNode)
        boolean loadNewData = true;
        if (source instanceof DBNEvent) {
            DBNEvent.NodeChange nodeChange = ((DBNEvent) source).getNodeChange();
            if (nodeChange == DBNEvent.NodeChange.UNLOAD) {
                loadNewData = false;
            }
        }
        if (!itemControl.isDisposed()) {
            if (loadNewData) {
                itemControl.loadData(false);
            }
        }
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    public IDatabaseEditorInput getEditorInput()
    {
        return (IDatabaseEditorInput)super.getEditorInput();
    }

    @Override
    public DBPDataSource getDataSource()
    {
        return getEditorInput().getDataSource();
    }
}