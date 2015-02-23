/*
 * Copyright (C) 2010-2015 Serge Rieder
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
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDatabaseEditor;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.IDBNListener;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerRefresh;

/**
 * DatabaseEditorListener
 */
public class DatabaseEditorListener implements IDBNListener
{

    private final IDatabaseEditor databaseEditor;
    private DBSDataSourceContainer dataSourceContainer;

    DatabaseEditorListener(IDatabaseEditor databaseEditor) {
        this.databaseEditor = databaseEditor;
        // Acquire datasource
        dataSourceContainer = databaseEditor.getEditorInput().getTreeNode().getDataSourceContainer();
        if (dataSourceContainer != null) {
            dataSourceContainer.acquire(databaseEditor);
        }
        // Register node listener
        DBeaverCore.getInstance().getNavigatorModel().addListener(this);
    }

    public void dispose()
    {
        // Release datasource
        if (dataSourceContainer != null) {
            // Remove node listener
            DBeaverCore.getInstance().getNavigatorModel().removeListener(this);

            dataSourceContainer.release(databaseEditor);
            dataSourceContainer = null;
        }
    }

    public DBNNode getTreeNode()
    {
        return databaseEditor.getEditorInput().getTreeNode();
    }

    @Override
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
                        runner = new Runnable() { @Override
                                                  public void run() {
                            databaseEditor.refreshPart(
                                event,
                                event.getNodeChange() == DBNEvent.NodeChange.REFRESH &&
                                event.getSource() instanceof NavigatorHandlerRefresh);
                        }};
                    }
                } else if (event.getNodeChange() == DBNEvent.NodeChange.UNLOAD) {
                    closeEditor = true;
                }
            }
            if (closeEditor) {
                if (DBeaverCore.isClosing()) {
                    // Do not update editors during shutdown, just remove listeners
                    dispose();
                    return;
                }
                runner = new Runnable() { @Override
                                          public void run() {
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