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
package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.IDataSourceContainerProvider;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.editors.ProjectFileEditorInput;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.database.NavigatorViewBase;
import org.jkiss.dbeaver.ui.navigator.project.ProjectExplorerView;

import java.lang.reflect.InvocationTargetException;

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
                    activeObject = DBUtils.getDefaultOrActiveObject(dataSource);
                } else {
                    activeObject = dsContainer;
                }

                final NavigatorViewBase view = navigatorView;
                DBeaverUI.runInUI(activePage.getWorkbenchWindow(), new DBRRunnableWithProgress() {
                    @Override
                    public void run(DBRProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException {
                        DBSObject showObject = activeObject;
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