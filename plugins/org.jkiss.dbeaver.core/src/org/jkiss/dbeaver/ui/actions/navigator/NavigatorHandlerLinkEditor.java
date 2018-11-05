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
package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.IDataSourceContainerProvider;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.editors.ProjectFileEditorInput;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.database.NavigatorViewBase;
import org.jkiss.dbeaver.ui.navigator.project.ProjectExplorerView;

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

        if (navigatorView instanceof ProjectExplorerView) {
            if (activeEditor instanceof SQLEditor) {
                IFile file = EditorUtils.getFileFromInput(activeEditor.getEditorInput());
                if (file != null) {
                    showResourceInNavigator(navigatorView, file);
                }
            } else if (activeEditor.getEditorInput() instanceof ProjectFileEditorInput) {
                IFile editorFile = ((ProjectFileEditorInput) activeEditor.getEditorInput()).getFile();
                showResourceInNavigator(navigatorView, editorFile);
            }
        } else if (activeEditor.getEditorInput() instanceof IDatabaseEditorInput) {
                IDatabaseEditorInput editorInput = (IDatabaseEditorInput) activeEditor.getEditorInput();
                DBNNode dbnNode = editorInput.getNavigatorNode();
                if (dbnNode != null) {
                    navigatorView.showNode(dbnNode);
                }
        } else if (activeEditor instanceof IDataSourceContainerProvider) {
            DBPDataSourceContainer dsContainer = ((IDataSourceContainerProvider) activeEditor).getDataSourceContainer();
            @NotNull
            final DBSObject activeObject;
            if (dsContainer != null) {
                DBPDataSource dataSource = dsContainer.getDataSource();
                if (dataSource != null) {
                    activeObject = DBUtils.getDefaultOrActiveObject(dataSource.getDefaultInstance());
                } else {
                    activeObject = dsContainer;
                }

                final NavigatorViewBase view = navigatorView;
                UIUtils.runInUI(activePage.getWorkbenchWindow(), monitor -> {
                    DBSObject showObject = activeObject;
                    if (showObject instanceof DBSInstance && !(showObject instanceof DBPDataSourceContainer)) {
                        showObject = activeObject.getParentObject();
                    }

                    if (showObject instanceof DBPDataSource) {
                        showObject = ((DBPDataSource) showObject).getContainer();
                    }

                    DBNDatabaseNode objectNode = view.getModel().getNodeByObject(
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
        activePage.activate(navigatorView);

        return null;
    }

    private void showResourceInNavigator(NavigatorViewBase activePart, IFile editorFile) {
        DBNProject projectNode = activePart.getModel().getRoot().getProject(editorFile.getProject());
        if (projectNode != null) {
            DBNResource resource = projectNode.findResource(editorFile);
            if (resource != null) {
                activePart.showNode(resource);
            }
        }
    }
}