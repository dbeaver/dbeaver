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
import org.eclipse.ui.*;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.ui.editors.ProjectFileEditorInput;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorView;
import org.jkiss.dbeaver.ui.navigator.database.NavigatorViewBase;
import org.jkiss.dbeaver.ui.navigator.project.ProjectExplorerView;

public class NavigatorHandlerLinkEditor extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchPage activePage = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();
        IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
        final IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
        if (activePart instanceof NavigatorViewBase) {
            if (activeEditor.getEditorInput() instanceof IDatabaseEditorInput) {
                IDatabaseEditorInput editorInput = (IDatabaseEditorInput) activeEditor.getEditorInput();
                DBNNode dbnNode = editorInput.getNavigatorNode();
                if (dbnNode != null) {
                    NavigatorViewBase view = (NavigatorViewBase)activePart;
                    view.showNode(dbnNode);
                }
            } else if (activeEditor.getEditorInput() instanceof ProjectFileEditorInput) {
                IFile editorFile = ((ProjectFileEditorInput)activeEditor.getEditorInput()).getFile();
                showResourceInNavigator((NavigatorViewBase) activePart, editorFile);
            }
        } else if (activePart instanceof EntityEditor) {
            DBNDatabaseNode curNode = ((EntityEditor) activePart).getEditorInput().getNavigatorNode();
            DatabaseNavigatorView dbNavigatorView = (DatabaseNavigatorView)activePage.findView(DatabaseNavigatorView.VIEW_ID);
            if (dbNavigatorView != null) {
                dbNavigatorView.showNode(curNode);
                activePage.activate(dbNavigatorView);
            }
        } else if (activePart instanceof SQLEditor) {
            ProjectExplorerView explorerView = (ProjectExplorerView)activePage.findView(ProjectExplorerView.VIEW_ID);
            if (explorerView != null) {
                IFile file = EditorUtils.getFileFromEditorInput(((SQLEditor) activePart).getEditorInput());
                if (file != null) {
                    showResourceInNavigator(explorerView, file);
                    activePage.activate(explorerView);
                }
            }
        }
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