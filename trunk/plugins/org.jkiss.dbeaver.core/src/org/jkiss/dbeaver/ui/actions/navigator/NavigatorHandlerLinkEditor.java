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
package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.ui.editors.ProjectFileEditorInput;
import org.jkiss.dbeaver.ui.views.navigator.database.NavigatorViewBase;

public class NavigatorHandlerLinkEditor extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
        final IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
        if (activePart instanceof NavigatorViewBase) {
            if (activeEditor.getEditorInput() instanceof IDatabaseEditorInput) {
                IDatabaseEditorInput editorInput = (IDatabaseEditorInput) activeEditor.getEditorInput();
                if (editorInput != null) {
                    DBNNode dbnNode = editorInput.getTreeNode();
                    if (dbnNode != null) {
                        NavigatorViewBase view = (NavigatorViewBase)activePart;
                        view.showNode(dbnNode);
                    }
                }
            } else if (activeEditor.getEditorInput() instanceof ProjectFileEditorInput) {
                IFile editorFile = ((ProjectFileEditorInput)activeEditor.getEditorInput()).getFile();
                DBNProject projectNode = ((NavigatorViewBase) activePart).getModel().getRoot().getProject(editorFile.getProject());
                if (projectNode != null) {
                    DBNResource resource = projectNode.findResource(editorFile);
                    if (resource != null) {
                        ((NavigatorViewBase)activePart).showNode(resource);
                    }
                }
            }
        }
        return null;
    }
}