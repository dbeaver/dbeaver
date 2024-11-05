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
package org.jkiss.dbeaver.ui.navigator.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.database.NavigatorViewBase;
import org.jkiss.dbeaver.ui.navigator.project.ProjectExplorerView;
import org.jkiss.dbeaver.ui.navigator.project.ProjectNavigatorView;

public class NavigatorHandlerLinkEditor extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final IWorkbenchPage activePage = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();
        final IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
        if (activeEditor == null) {
            return null;
        }
        NavigatorViewBase navigatorView = NavigatorUtils.getActiveNavigatorView(event);
        if (navigatorView == null) {
            return null;
        }

        if (navigatorView instanceof ProjectExplorerView ||
                (navigatorView instanceof ProjectNavigatorView && activeEditor instanceof ITextEditor)) {
            IFile file = EditorUtils.getFileFromInput(activeEditor.getEditorInput());
            if (file != null) {
                showResourceInNavigator(navigatorView, file);
            }
        } else {
            DBNModel globalNavigatorModel = navigatorView.getGlobalNavigatorModel();
            if (activeEditor.getEditorInput() instanceof IDatabaseEditorInput editorInput) {
                DBNNode dbnNode = editorInput.getNavigatorNode();
                if (dbnNode == null) {
                    DBSObject databaseObject = editorInput.getDatabaseObject();
                    if (databaseObject != null) {
                        dbnNode = globalNavigatorModel.findNode(databaseObject);
                    }
                }
                if (dbnNode != null) {
                    navigatorView.showNode(dbnNode);
                }
            } else if (activeEditor instanceof DBPDataSourceContainerProvider provider) {
                DBPDataSourceContainer dsContainer = provider.getDataSourceContainer();

                DBSObject activeObject = null;
                if (dsContainer != null) {
                    if (activeEditor instanceof DBPContextProvider contextProvider) {
                        DBCExecutionContext executionContext = contextProvider.getExecutionContext();
                        if (executionContext != null) {
                            DBCExecutionContextDefaults<?,?> contextDefaults = executionContext.getContextDefaults();
                            if (contextDefaults != null) {
                                activeObject = contextDefaults.getDefaultSchema();
                                if (activeObject == null) {
                                    activeObject = contextDefaults.getDefaultCatalog();
                                }
                            }
                        }
                    }
                    if (activeObject == null) {
                        DBPDataSource dataSource = dsContainer.getDataSource();
                        if (dataSource != null) {
                            activeObject = DBUtils.getDefaultOrActiveObject(dataSource.getDefaultInstance());
                        } else {
                            activeObject = dsContainer;
                        }
                    }

                    DBSObject objectToSelect = activeObject;
                    final NavigatorViewBase view = navigatorView;
                    UIUtils.runInUI(activePage.getWorkbenchWindow(), monitor -> {
                        DBSObject showObject = objectToSelect;
                        if (showObject instanceof DBSInstance && !(showObject instanceof DBPDataSourceContainer)) {
                            showObject = objectToSelect.getParentObject();
                        }

                        if (showObject instanceof DBPDataSource dataSource) {
                            showObject = dataSource.getContainer();
                        }

                        DBNDatabaseNode objectNode = globalNavigatorModel.getNodeByObject(
                            monitor,
                            showObject,
                            true
                        );
                        if (objectNode != null) {
                            view.showNode(objectNode);
                        }
                    });
                }
            }
        }
        activePage.activate(navigatorView);

        return null;
    }

    private void showResourceInNavigator(NavigatorViewBase activePart, IFile editorFile) {
        DBNProject projectNode = NavigatorResources.getProjectNode(
            NavigatorViewBase.getGlobalNavigatorModel().getRoot(), editorFile.getProject());
        DBNResource resource = NavigatorResources.findResource(projectNode, editorFile);
        if (resource != null) {
            activePart.showNode(resource);
        }
    }
}