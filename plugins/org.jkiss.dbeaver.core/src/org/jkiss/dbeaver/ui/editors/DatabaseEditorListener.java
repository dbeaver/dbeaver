/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.ui.IWorkbenchPage;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.INavigatorListener;

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
        IDatabaseEditorInput editorInput = databaseEditor.getEditorInput();
        if (editorInput.getDatabaseObject() instanceof DBPDataSourceContainer) {
            dataSourceContainer = (DBPDataSourceContainer) editorInput.getDatabaseObject();
        } else if (editorInput.getNavigatorNode() != null) {
            dataSourceContainer = editorInput.getNavigatorNode().getDataSourceContainer();
        }
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
            dataSourceContainer.release(databaseEditor);
            dataSourceContainer = null;
        }

        // Remove node listener
        DBeaverCore.getInstance().getNavigatorModel().removeListener(this);
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
                        runner = new Runnable() {
                            @Override
                            public void run()
                            {
                                databaseEditor.refreshPart(
                                    event,
                                    event.getNodeChange() == DBNEvent.NodeChange.REFRESH &&
                                    event.getSource() == DBNEvent.FORCE_REFRESH);
                            }
                        };
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
                DBeaverUI.asyncExec(runner);
            }
        }
    }

    protected boolean isValuableNode(DBNNode node)
    {
        DBNNode editorNode = getTreeNode();
        return node == editorNode || (editorNode != null && editorNode.isChildOf(node));
    }

}