/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.entity;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.jkiss.dbeaver.ext.ui.INavigatorModelView;
import org.jkiss.dbeaver.ext.ui.IObjectEditorPart;
import org.jkiss.dbeaver.ext.ui.IRefreshablePart;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.registry.tree.DBXTreeNode;
import org.jkiss.dbeaver.ui.controls.itemlist.ItemListControl;
import org.jkiss.dbeaver.utils.ViewUtils;

/**
 * EntityNodeEditor
 */
class EntityNodeEditor extends EditorPart implements IRefreshablePart, INavigatorModelView, IObjectEditorPart
{
    static final Log log = LogFactory.getLog(EntityNodeEditor.class);

    private DBNNode node;
    private DBXTreeNode metaNode;
    private ItemListControl itemControl;
    private boolean activated;

    EntityNodeEditor(DBNNode node)
    {
        if (node == null) {
            throw new IllegalArgumentException("Node can't be null");
        }
        this.node = node;
    }

    EntityNodeEditor(DBNNode node, DBXTreeNode metaNode)
    {
        this.node = node;
        this.metaNode = metaNode;
    }

    public void doSave(IProgressMonitor monitor)
    {
    }

    public void doSaveAs()
    {
    }

    public void init(IEditorSite site, IEditorInput input)
        throws PartInitException
    {
        setSite(site);
        setInput(input);
    }

    public boolean isDirty()
    {
        return false;
    }

    public boolean isSaveAsAllowed()
    {
        return false;
    }

    public void createPartControl(Composite parent)
    {
        itemControl = new ItemListControl(parent, SWT.NONE, this, node);

        // Hook context menu
        ViewUtils.addContextMenu(this);
        // Add drag and drop support
        ViewUtils.addDragAndDropSupport(this);
        getSite().setSelectionProvider(itemControl.getSelectionProvider());
    }

    public void setFocus()
    {
    }

    public DBNNode getRootNode() {
        return node;
    }

    public Viewer getNavigatorViewer()
    {
        return itemControl.getNavigatorViewer();
    }

    public IWorkbenchPart getWorkbenchPart()
    {
        return this;
    }

    public void activatePart()
    {
        if (!activated) {
            activated = true;
            itemControl.fillData(metaNode);
        }
    }

    public void deactivatePart()
    {
    }

    public void refreshPart(Object source)
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
            itemControl.clearData();
            if (loadNewData) {
                itemControl.fillData(metaNode);
            }
        }
    }

    @Override
    public void dispose() {
        if (itemControl != null) {
            itemControl.dispose();
            itemControl = null;
        }
        super.dispose();
    }
}