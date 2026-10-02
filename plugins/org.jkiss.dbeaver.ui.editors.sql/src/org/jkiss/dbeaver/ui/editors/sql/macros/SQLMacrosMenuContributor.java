/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.macros;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Dynamic content of the SQL editor "Macros" menu.
 * <p>
 * Lists configured macros (each applies the macro on click), allows to save the
 * current editor selection as a macro and opens the "Manage macros" dialog.
 */
public class SQLMacrosMenuContributor extends CompoundContributionItem {

    @Override
    protected IContributionItem[] getContributionItems() {
        List<IContributionItem> items = new ArrayList<>();
        List<SQLMacro> macros = SQLMacrosRegistry.getInstance().getMacros();
        if (macros.isEmpty()) {
            Action noMacrosAction = new Action(SQLEditorMessages.menu_macros_empty) {
            };
            noMacrosAction.setEnabled(false);
            items.add(new ActionContributionItem(noMacrosAction));
        } else {
            for (SQLMacro macro : macros) {
                items.add(new ActionContributionItem(new ApplyMacroAction(macro)));
            }
        }
        items.add(new Separator());
        items.add(new ActionContributionItem(new SaveSelectionAction()));
        items.add(new ActionContributionItem(new ManageMacrosAction()));
        return items.toArray(new IContributionItem[0]);
    }

    private class ApplyMacroAction extends Action {

        private final SQLMacro macro;

        ApplyMacroAction(@NotNull SQLMacro macro) {
            super(macro.getName() + (macro.hasShortcut()
                    ? " (" + macro.getShortcutLabel() + ")" //$NON-NLS-1$ //$NON-NLS-2$
                    : "")); //$NON-NLS-1$
            this.macro = macro;
            setImageDescriptor(DBeaverIcons.getImageDescriptor(DBIcon.SQL_TEXT));
        }

        @Override
        public void run() {
            SQLEditorBase editor = getActiveSQLEditor();
            if (editor != null) {
                SQLMacrosApplyHandler.applyMacro(editor, macro);
            }
        }
    }

    private class SaveSelectionAction extends Action {

        SaveSelectionAction() {
            super(SQLEditorMessages.menu_macros_save_selection);
            setImageDescriptor(DBeaverIcons.getImageDescriptor(DBIcon.SQL_TEXT));
        }

        @Override
        public void run() {
            SQLEditorBase editor = getActiveSQLEditor();
            if (editor == null) {
                return;
            }
            ISelection selection = editor.getSelectionProvider().getSelection();
            String selectionText = selection instanceof ITextSelection textSelection ? textSelection.getText() : ""; //$NON-NLS-1$
            SQLMacro initialMacro = null;
            if (!selectionText.isBlank()) {
                initialMacro = new SQLMacro(
                        UUID.randomUUID().toString(),
                        "Macro", //$NON-NLS-1$
                        selectionText);
            }
            SQLMacrosEditDialog dialog = new SQLMacrosEditDialog(UIUtils.getActiveWorkbenchShell(), initialMacro);
            if (dialog.open() == IDialogConstants.OK_ID) {
                SQLMacro macro = dialog.getMacro();
                if (macro != null) {
                    SQLMacrosRegistry.getInstance().updateMacro(macro);
                }
            }
        }

        @Override
        public boolean isEnabled() {
            SQLEditorBase editor = getActiveSQLEditor();
            if (editor == null) {
                return false;
            }
            ISelection selection = editor.getSelectionProvider().getSelection();
            return selection instanceof ITextSelection textSelection && textSelection.getLength() > 0;
        }
    }

    private class ManageMacrosAction extends Action {

        ManageMacrosAction() {
            super(SQLEditorMessages.menu_macros_manage);
            setImageDescriptor(DBeaverIcons.getImageDescriptor(DBIcon.TREE_INFO));
        }

        @Override
        public void run() {
            SQLMacrosDialog dialog = new SQLMacrosDialog(UIUtils.getActiveWorkbenchShell());
            dialog.open();
        }
    }

    @Nullable
    private SQLEditorBase getActiveSQLEditor() {
        IEditorPart activeEditor = UIUtils.getActiveWorkbenchWindow().getActivePage().getActiveEditor();
        return activeEditor instanceof SQLEditorBase editor ? editor : null;
    }
}