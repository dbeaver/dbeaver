/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditor;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.IDBNListener;
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
        dataSourceContainer = databaseEditor.getEditorInput().getDataSource().getContainer();
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
        return databaseEditor.getEditorInput().getTreeNode();
    }

    public void nodeChanged(final DBNEvent event)
    {
        if (isValuableNode(event.getNode())) {
            Runnable runner = null;
            boolean closeEditor = false;
            if (event.getAction() == DBNEvent.Action.REMOVE) {
                closeEditor = true;
            } else if (event.getAction() == DBNEvent.Action.UPDATE) {
                if (event.getNodeChange() == DBNEvent.NodeChange.REFRESH ||
                    event.getNodeChange() == DBNEvent.NodeChange.LOAD)
                {
                    if (getTreeNode() == event.getNode()) {
                        runner = new Runnable() { public void run() {
                            databaseEditor.refreshPart(event);
                        }};
                    }
                } else if (event.getNodeChange() == DBNEvent.NodeChange.UNLOAD) {
                    closeEditor = true;
                }
            }
            if (closeEditor) {
                runner = new Runnable() { public void run() {
                    IWorkbenchPage workbenchPage = databaseEditor.getSite().getWorkbenchWindow().getActivePage();
                    if (workbenchPage != null) {
                        workbenchPage.closeEditor(databaseEditor, false);
                    }
                }};
            }
            if (runner != null) {
                Display.getDefault().asyncExec(runner);
            }
        }
    }

    protected boolean isValuableNode(DBNNode node)
    {
        DBNNode editorNode = getTreeNode();
        return node == editorNode || editorNode.isChildOf(node);
    }


}