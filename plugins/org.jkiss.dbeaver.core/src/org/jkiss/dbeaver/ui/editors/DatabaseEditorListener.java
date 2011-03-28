/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors;

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditor;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditorInput;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.IDBNListener;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

/**
 * DatabaseEditorListener
 */
public class DatabaseEditorListener implements IDBNListener
{

    private IDatabaseNodeEditor databaseEditor;
    private DBSDataSourceContainer dataSourceContainer;

    DatabaseEditorListener(IDatabaseNodeEditor databaseEditor) {
        this.databaseEditor = databaseEditor;
        // Acquire datasource
        dataSourceContainer = (DBSDataSourceContainer) Platform.getAdapterManager().getAdapter(databaseEditor, DBSDataSourceContainer.class);
        if (dataSourceContainer != null) {
            dataSourceContainer.acquire(databaseEditor);
        }
        // Register node listener
        DBeaverCore.getInstance().getNavigatorModel().addListener(this);
    }

    public void dispose()
    {
        // Remove node listener
        DBeaverCore.getInstance().getNavigatorModel().removeListener(this);
        // Release datasource
        if (dataSourceContainer != null) {
            dataSourceContainer.release(databaseEditor);
            dataSourceContainer = null;
        }
    }

    public DBNNode getTreeNode()
    {
        if (databaseEditor.getEditorInput() instanceof IDatabaseNodeEditorInput) {
            return ((IDatabaseNodeEditorInput) databaseEditor.getEditorInput()).getTreeNode();
        } else {
            return null;
        }
    }

    public void nodeChanged(final DBNEvent event)
    {
        if (isValuableNode(event.getNode())) {
            boolean closeEditor = false;
            if (event.getAction() == DBNEvent.Action.REMOVE) {
                closeEditor = true;
            } else if (event.getAction() == DBNEvent.Action.UPDATE) {
                if (event.getNodeChange() == DBNEvent.NodeChange.REFRESH ||
                    event.getNodeChange() == DBNEvent.NodeChange.LOAD)
                {
                    if (getTreeNode() == event.getNode()) {
                        databaseEditor.refreshDatabaseContent(event);
                    }
                } else if (event.getNodeChange() == DBNEvent.NodeChange.UNLOAD) {
                    closeEditor = true;
                }
            }
            if (closeEditor) {
                Display.getDefault().asyncExec(new Runnable() { public void run() {
                    IWorkbenchPage workbenchPage = databaseEditor.getSite().getWorkbenchWindow().getActivePage();
                    if (workbenchPage != null) {
                        workbenchPage.closeEditor(databaseEditor, false);
                    }
                }});
            }
        }
    }

    protected boolean isValuableNode(DBNNode node)
    {
        DBNNode editorNode = getTreeNode();
        return node == editorNode || editorNode.isChildOf(node);
    }


}