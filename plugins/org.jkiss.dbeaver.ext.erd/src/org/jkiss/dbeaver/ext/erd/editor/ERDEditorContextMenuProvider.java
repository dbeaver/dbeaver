/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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
/*
 * Created on Jul 22, 2004
 */
package org.jkiss.dbeaver.ext.erd.editor;

import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.jkiss.dbeaver.ext.erd.action.DiagramLayoutAction;
import org.jkiss.dbeaver.ui.navigator.NavigatorCommands;

/**
 * Provides a context menu for the schema diagram editor. A virtual cut and paste from the flow example
 *
 * @author Daniel Lee
 */
public class ERDEditorContextMenuProvider extends ContextMenuProvider {
    private ERDEditorPart editor;

    /**
     * Creates a new FlowContextMenuProvider associated with the given viewer
     * and action registry.
     *
     * @param editor the editor
     */
    public ERDEditorContextMenuProvider(ERDEditorPart editor) {
        super(editor.getViewer());
        this.editor = editor;
    }

    /**
     * @see ContextMenuProvider#buildContextMenu(org.eclipse.jface.action.IMenuManager)
     */
    @Override
    public void buildContextMenu(IMenuManager menu) {
        if (editor.isLoaded()) {
            ISelection selection = editor.getGraphicalViewer().getSelection();
            if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
                editor.fillPartContextMenu(menu, (IStructuredSelection) selection);
            }

            menu.add(new Separator());
            editor.fillAttributeVisibilityMenu(menu);
            menu.add(new DiagramLayoutAction(editor));

            menu.add(new Separator());

            menu.add(new Separator("org.eclipse.gef.group.undo"));
            menu.add(new Separator("org.eclipse.gef.group.copy"));
            //menu.add(ActionUtils.makeCommandContribution(editor.getSite(), IWorkbenchCommandConstants.EDIT_COPY));

            menu.add(new Separator("org.eclipse.gef.group.print"));
            menu.add(new Separator("org.eclipse.gef.group.edit"));
            menu.add(new Separator("org.eclipse.gef.group.view"));
            menu.add(new Separator("org.eclipse.gef.group.find"));
            menu.add(new Separator("org.eclipse.gef.group.rest"));
            menu.add(new Separator("org.eclipse.gef.group.save"));

            menu.add(new Separator());

            menu.add(new GroupMarker(NavigatorCommands.GROUP_TOOLS));
            menu.add(new GroupMarker(NavigatorCommands.GROUP_NAVIGATOR_ADDITIONS));
            //menu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
            //menu.add(new GroupMarker(IActionConstants.MB_ADDITIONS_END));

        }
    }
}