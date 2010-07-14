/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
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
import org.jkiss.dbeaver.ext.ui.IEmbeddedWorkbenchPart;
import org.jkiss.dbeaver.ext.ui.IMetaModelView;
import org.jkiss.dbeaver.ext.ui.IRefreshablePart;
import org.jkiss.dbeaver.model.meta.DBMEvent;
import org.jkiss.dbeaver.model.meta.DBMModel;
import org.jkiss.dbeaver.model.meta.DBMNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.registry.tree.DBXTreeNode;
import org.jkiss.dbeaver.ui.controls.itemlist.ItemListControl;
import org.jkiss.dbeaver.utils.DBeaverUtils;
import org.jkiss.dbeaver.utils.ViewUtils;

import java.lang.reflect.InvocationTargetException;

/**
 * EntityNodeEditor
 */
class EntityNodeEditor extends EditorPart implements IRefreshablePart, IMetaModelView, IEmbeddedWorkbenchPart
{
    static final Log log = LogFactory.getLog(EntityNodeEditor.class);

    private DBMNode node;
    private DBXTreeNode metaNode;
    private ItemListControl itemControl;
    private boolean activated;

    EntityNodeEditor(DBMNode node)
    {
        this.node = node;
    }

    EntityNodeEditor(DBMNode node, DBXTreeNode metaNode)
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

    public DBMModel getMetaModel()
    {
        return node.getModel();
    }

    public Viewer getViewer()
    {
        return itemControl.getViewer();
    }

    public IWorkbenchPart getWorkbenchPart()
    {
        return this;
    }

    public void activatePart()
    {
        if (!activated) {
            try {
                DBeaverUtils.run(
                    getSite().getWorkbenchWindow(),
                    false,
                    false,
                    new DBRRunnableWithProgress() {
                        public void run(DBRProgressMonitor monitor)
                            throws InvocationTargetException, InterruptedException
                        {
                            itemControl.fillData(metaNode);
                        }
                    }
                );
            } catch (InvocationTargetException e) {
                log.error(e.getTargetException());
            } catch (InterruptedException e) {
                // do nothing
            }
            activated = true;
        }
    }

    public void deactivatePart()
    {
    }

    public void refreshPart(Object source)
    {
        // Check - do we need to load new content in editor
        // If this is DBM event then check node change type
        // UNLOADED usually means that connection was closed on connection's node is not removed but
        // is in "unloaded" state.
        // Without this check editor will try to reload it's content and thus will reopen just closed connection
        // (by calling getChildren() on DBMNode)
        boolean loadNewData = true;
        if (source instanceof DBMEvent) {
            DBMEvent.NodeChange nodeChange = ((DBMEvent) source).getNodeChange();
            if (nodeChange == DBMEvent.NodeChange.UNLOADED) {
                loadNewData = false;
            }
        }
        itemControl.clearData();
        if (loadNewData) {
            itemControl.fillData(metaNode);
        }
    }

}