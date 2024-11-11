/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;

/**
 * DatabaseEditorListener
 */
public class DatabaseEditorListener implements INavigatorListener {
    private final IDatabaseEditor databaseEditor;
    private DBPDataSourceContainer dataSourceContainer;

    DatabaseEditorListener(IDatabaseEditor databaseEditor) {
        this.databaseEditor = databaseEditor;
        // Acquire datasource
        IEditorInput editorInput = databaseEditor.getEditorInput();
        if (editorInput instanceof IDatabaseEditorInput databaseEditorInput) {
            if (databaseEditorInput.getDatabaseObject() instanceof DBPDataSourceContainer container) {
                dataSourceContainer = container;
            } else if (databaseEditorInput.getNavigatorNode() != null) {
                dataSourceContainer = databaseEditorInput.getNavigatorNode().getDataSourceContainer();
            }
            if (dataSourceContainer != null) {
                dataSourceContainer.acquire(databaseEditor);
            }
        }
        // Register node listener
        DBWorkbench.getPlatform().getNavigatorModel().addListener(this);
    }

    public void dispose() {
        // Release datasource
        if (dataSourceContainer != null) {
            dataSourceContainer.release(databaseEditor);
            dataSourceContainer = null;
        }

        // Remove node listener
        DBWorkbench.getPlatform().getNavigatorModel().removeListener(this);
    }

    @Override
    public void nodeChanged(final DBNEvent event) {
        if (isValuableNode(event.getNode())) {
            Strategy strategy = Strategy.DO_NOTHING;
            if (event.getAction() == DBNEvent.Action.REMOVE) {
                strategy = Strategy.CLOSE;
            } else if (event.getAction() == DBNEvent.Action.UPDATE) {
                if (event.getNodeChange() == DBNEvent.NodeChange.REFRESH ||
                    event.getNodeChange() == DBNEvent.NodeChange.LOAD) {
                    if (isSameNode(event.getNode())) {
                        databaseEditor.refreshPart(
                            event,
                            event.getNodeChange() == DBNEvent.NodeChange.REFRESH &&
                            event.getSource() == DBNEvent.FORCE_REFRESH);
                    } else if (event.getNodeChange() == DBNEvent.NodeChange.LOAD && event.getNode() instanceof DBNDataSource) {
                        strategy = Strategy.LOAD;
                    }
                } else if (event.getNodeChange() == DBNEvent.NodeChange.UNLOAD) {
                    strategy = Strategy.UNLOAD;
                }
            }
            if (strategy != Strategy.DO_NOTHING) {
                if (DBWorkbench.getPlatform().isShuttingDown()) {
                    // Do not update editors during shutdown, just remove listeners
                    dispose();
                    return;
                }
                if (strategy == Strategy.LOAD) {
                    if (databaseEditor instanceof ILazyEditor) {
                        ((ILazyEditor) databaseEditor).loadEditorInput();
                    }
                    return;
                }
                if (strategy == Strategy.UNLOAD &&
                    DBWorkbench.getPlatform().getPreferenceStore().getBoolean(DatabaseEditorPreferences.PROP_KEEP_EDITORS_ON_DISCONNECT) &&
                    databaseEditor instanceof ILazyEditor && ((ILazyEditor) databaseEditor).unloadEditorInput()
                ) {
                    return;
                }
                IWorkbenchWindow workbenchWindow = databaseEditor.getSite().getWorkbenchWindow();
                if (workbenchWindow != null) {
                    IWorkbenchPage workbenchPage = workbenchWindow.getActivePage();
                    if (workbenchPage != null) {
                        workbenchPage.closeEditor(databaseEditor, false);
                    }
                }
            }
        }
    }

    private boolean isSameNode(@NotNull DBNNode other) {
        final DBNNode editorNode = getEditorNode();

        if (editorNode != null) {
            return editorNode == other;
        } else {
            final String path = getEditorNodePath();
            return path != null && path.equals(other.getNodeUri());
        }
    }

    private boolean isValuableNode(@NotNull DBNNode node) {
        final DBNNode editorNode = getEditorNode();

        if (editorNode != null) {
            return editorNode == node || editorNode.isChildOf(node);
        } else {
            final String path = getEditorNodePath();
            return path != null && path.startsWith(node.getNodeUri());
        }
    }

    @Nullable
    private DBNNode getEditorNode() {
        final IEditorInput input = databaseEditor.getEditorInput();

        if (input instanceof INavigatorEditorInput navigatorEditorInput) {
            return navigatorEditorInput.getNavigatorNode();
        } else {
            return null;
        }
    }

    @Nullable
    private String getEditorNodePath() {
        final IEditorInput input = databaseEditor.getEditorInput();

        if (input instanceof DatabaseLazyEditorInput lazyEditorInput) {
            return lazyEditorInput.getNodePath();
        } else if (input instanceof IDatabaseEditorInput databaseEditorInput) {
            DBNDatabaseNode navigatorNode = databaseEditorInput.getNavigatorNode();
            return navigatorNode == null ? null : navigatorNode.getNodeUri();
        } else {
            return null;
        }
    }

    private enum Strategy {
        DO_NOTHING,
        CLOSE,
        UNLOAD,
        LOAD
    }
}