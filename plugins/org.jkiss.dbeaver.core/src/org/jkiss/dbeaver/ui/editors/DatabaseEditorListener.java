/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDatabaseEditor;
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
            boolean closeEditor = false;
            if (event.getAction() == DBNEvent.Action.REMOVE) {
                closeEditor = true;
            } else if (event.getAction() == DBNEvent.Action.UPDATE) {
                if (event.getNodeChange() == DBNEvent.NodeChange.REFRESH ||
                    event.getNodeChange() == DBNEvent.NodeChange.LOAD)
                {
                    if (databaseEditor.getEditorInput().getTreeNode() == event.getNode()) {
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
        DBNNode editorNode = databaseEditor.getEditorInput().getTreeNode();
        return node == editorNode || editorNode.isChildOf(node);
    }


}