/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.erd.ui.editor;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.jkiss.dbeaver.erd.ui.action.DiagramLayoutAction;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.IActionConstants;
import org.jkiss.dbeaver.ui.navigator.NavigatorCommands;

/**
 * Provides a context menu for the schema diagram editor. A virtual cut and paste from the flow example
 */
public class ERDEditorContextMenuProvider extends MenuManager implements IMenuListener {
    private ERDEditorPart editor;

    /**
     * Creates a new FlowContextMenuProvider associated with the given viewer
     * and action registry.
     *
     * @param editor the editor
     */
    ERDEditorContextMenuProvider(ERDEditorPart editor) {
        super("ERD Editor Context Menu", "#ERDEditorContext");
        this.editor = editor;

        this.addMenuListener(this);
        this.setRemoveAllWhenShown(true);

        editor.getEditorSite().registerContextMenu(
            "#ERDEditorContext", this, editor.getEditorSite().getSelectionProvider(), false);
    }

    public void menuAboutToShow(IMenuManager menu) {
        this.buildContextMenu(menu);
    }

    private void buildContextMenu(IMenuManager menu) {
        if (editor.isLoaded()) {
            menu.add(new Separator(IWorkbenchActionConstants.M_EDIT));

            ISelection selection = editor.getGraphicalViewer().getSelection();
            if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
                editor.fillPartContextMenu(menu, (IStructuredSelection) selection);
            }

            menu.add(new Separator());
            editor.fillAttributeVisibilityMenu(menu);
            menu.add(new DiagramLayoutAction(editor));

            menu.add(new Separator());

            menu.add(ActionUtils.makeCommandContribution(editor.getSite(), IWorkbenchCommandConstants.EDIT_COPY));
            if (ActionUtils.isCommandEnabled(IWorkbenchCommandConstants.EDIT_DELETE, editor.getSite())) {
                menu.add(ActionUtils.makeCommandContribution(editor.getSite(), IWorkbenchCommandConstants.EDIT_DELETE));
            }

            menu.add(new Separator());

            menu.add(new GroupMarker(NavigatorCommands.GROUP_TOOLS));
//            menu.add(new GroupMarker(NavigatorCommands.GROUP_NAVIGATOR_ADDITIONS));
//            menu.add(new GroupMarker(NavigatorCommands.GROUP_NAVIGATOR_ADDITIONS_END));
            menu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
            menu.add(new GroupMarker(IActionConstants.MB_ADDITIONS_END));

        }
    }
}