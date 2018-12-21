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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.texteditor.FindReplaceAction;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.jkiss.dbeaver.ui.ActionUtils;

import java.util.ResourceBundle;

/**
 * StyledTextContentAdapter
 */
public class StyledTextUtils {

    public static void enableDND(StyledText control) {
        DropTarget dropTarget = new DropTarget(control, DND.DROP_DEFAULT | DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK);
        dropTarget.setTransfer(TextTransfer.getInstance());
        dropTarget.addDropListener(new DropTargetAdapter() {
            public void dragEnter(DropTargetEvent e) {
                if (e.detail == DND.DROP_DEFAULT) {
                    e.detail = DND.DROP_COPY;
                }
            }
            public void dragOperationChanged(DropTargetEvent e) {
                if (e.detail == DND.DROP_DEFAULT) {
                    e.detail = DND.DROP_COPY;
                }
            }
            public void drop(DropTargetEvent e) {
                control.insert((String) e.data);
            }
        });
    }

    public static void fillDefaultStyledTextContextMenu(final StyledText text) {
        MenuManager menuMgr = new MenuManager();
        menuMgr.addMenuListener(manager ->
            fillDefaultStyledTextContextMenu(manager, text));
        menuMgr.setRemoveAllWhenShown(true);
        text.setMenu(menuMgr.createContextMenu(text));
    }

    public static void fillDefaultStyledTextContextMenu(IMenuManager menu, final StyledText text) {
        final Point selectionRange = text.getSelectionRange();
        menu.add(new StyledTextAction(IWorkbenchCommandConstants.EDIT_COPY, selectionRange.y > 0, text, ST.COPY));
        menu.add(new StyledTextAction(IWorkbenchCommandConstants.EDIT_PASTE, text.getEditable(), text, ST.PASTE));
        menu.add(new StyledTextAction(IWorkbenchCommandConstants.EDIT_CUT, selectionRange.y > 0, text, ST.CUT));
        menu.add(new StyledTextAction(IWorkbenchCommandConstants.EDIT_SELECT_ALL, true, text, ST.SELECT_ALL));

        menu.add(new StyledTextActionEx(ITextEditorActionDefinitionIds.WORD_WRAP, Action.AS_CHECK_BOX) {
            @Override
            public boolean isChecked() {
                return text.getWordWrap();
            }

            @Override
            public void run() {
                text.setWordWrap(!text.getWordWrap());
            }
        });

        IFindReplaceTarget stFindReplaceTarget = new StyledTextFindReplaceTarget(text);
        menu.add(new FindReplaceAction(
            ResourceBundle.getBundle("org.eclipse.ui.texteditor.ConstructedEditorMessages"),
            "Editor.FindReplace.",
            text.getShell(),
            stFindReplaceTarget));
        menu.add(new GroupMarker("styled_text_additions"));
    }

    private static class StyledTextAction extends Action {
        private final StyledText styledText;
        private final int action;
        public StyledTextAction(String actionId, boolean enabled, StyledText styledText, int action) {
            super(ActionUtils.findCommandName(actionId));
            this.setActionDefinitionId(actionId);
            this.setEnabled(enabled);
            this.styledText = styledText;
            this.action = action;
        }

        @Override
        public void run() {
            styledText.invokeAction(action);
        }
    }

    private static class StyledTextActionEx extends Action {
        public StyledTextActionEx(String actionId, int style) {
            super(ActionUtils.findCommandName(actionId), style);
            this.setActionDefinitionId(actionId);
        }

    }

}
