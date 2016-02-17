/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.INavigatorListener;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerRefresh;

/**
 * DatabaseEditorListener
 */
public class DatabaseEditorListener implements INavigatorListener
{

    private final IDatabaseEditor databaseEditor;
    private DBPDataSourceContainer dataSourceContainer;

    DatabaseEditorListener(IDatabaseEditor databaseEditor) {
        this.databaseEditor = databaseEditor;
        // Acquire datasource
        dataSourceContainer = databaseEditor.getEditorInput().getNavigatorNode().getDataSourceContainer();
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
        return databaseEditor.getEditorInput().getNavigatorNode();
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
                                event.getSource() == NavigatorHandlerRefresh.FORCE_REFRESH);
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