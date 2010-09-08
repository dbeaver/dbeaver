/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors;

import org.eclipse.ui.IWorkbenchPage;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNTreeFolder;
import org.jkiss.dbeaver.model.navigator.IDBNListener;

/**
 * DatabaseEditorListener
 */
public class DatabaseEditorListener implements IDBNListener
{

    private IDatabaseEditor databaseEditor;

    DatabaseEditorListener(IDatabaseEditor databaseEditor) {
        this.databaseEditor = databaseEditor;
        DBeaverCore.getInstance().getNavigatorModel().addListener(this);
    }

    public void dispose()
    {
        DBeaverCore.getInstance().getNavigatorModel().removeListener(this);
    }

    public void nodeChanged(final DBNEvent event)
    {
        if (isValuableNode(event.getNode())) {
            if (event.getAction() == DBNEvent.Action.REMOVE) {
                databaseEditor.getSite().getShell().getDisplay().asyncExec(new Runnable() { public void run() {
                    IWorkbenchPage workbenchPage = databaseEditor.getSite().getWorkbenchWindow().getActivePage();
                    if (workbenchPage != null) {
                        workbenchPage.closeEditor(databaseEditor, false);
                    }
                }});
            } else if (event.getAction() == DBNEvent.Action.UPDATE) {
                if (event.getNodeChange() == DBNEvent.NodeChange.REFRESH) {
                    databaseEditor.refreshDatabaseContent(event);
                }
            }
        }
    }

    protected boolean isValuableNode(DBNNode node)
    {
        DBNNode editorNode = databaseEditor.getEditorInput().getTreeNode();
        while (editorNode instanceof DBNTreeFolder) {
            editorNode = editorNode.getParentNode();
        }
        return node == editorNode;
    }


}