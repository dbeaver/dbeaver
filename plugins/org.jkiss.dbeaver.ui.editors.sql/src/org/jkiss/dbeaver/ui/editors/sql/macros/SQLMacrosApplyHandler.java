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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * Applies a macro in the active SQL editor.
 * <p>
 * This handler is registered for every apply-macro command
 * ({@link SQLMacrosConstants#APPLY_MACRO_COMMAND_PREFIX}&lt;N&gt;).
 * Each command id corresponds to a shortcut slot (Ctrl+Alt+F1..Ctrl+Alt+F12).
 */
public class SQLMacrosApplyHandler extends AbstractHandler {

    private static final Log log = Log.getLog(SQLMacrosApplyHandler.class);

    @Override
    public Object execute(@NotNull ExecutionEvent event) throws ExecutionException {
        SQLMacro macro = SQLMacrosRegistry.getInstance().getMacroByCommandId(event.getCommand().getId());
        if (macro == null) {
            return null;
        }
        IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
        if (activeEditor instanceof SQLEditorBase editor) {
            applyMacro(editor, macro);
        }
        return null;
    }

    /**
     * Applies the macro according to its {@link MacroAction}.
     * <p>
     * The {@link SQLMacrosConstants#SELECTION_PLACEHOLDER} placeholder is replaced with the
     * selected text. If there is no selection and the macro is executed (insert-and-execute
     * or execute-only), the macro is not applied and a warning is shown instead.
     */
    public static void applyMacro(@NotNull SQLEditorBase editor, @NotNull SQLMacro macro) {
        ITextSelection selection = getTextSelection(editor);
        int offset = selection == null ? 0 : selection.getOffset();
        int length = selection == null ? 0 : selection.getLength();
        String selectionText = selection == null ? "" : selection.getText(); //$NON-NLS-1$
        boolean hasSelection = !CommonUtils.isEmpty(selectionText);

        MacroAction action = macro.getAction();
        boolean insertText = action == MacroAction.INSERT || action == MacroAction.INSERT_AND_EXECUTE;
        boolean executeText = action == MacroAction.INSERT_AND_EXECUTE || action == MacroAction.EXECUTE_ONLY;

        // A query with a selection placeholder requires a selection to be executed correctly
        if (executeText && !hasSelection
            && macro.getQuery().contains(SQLMacrosConstants.SELECTION_PLACEHOLDER)) {
            UIUtils.showMessageBox(
                    editor.getSite().getShell(),
                    SQLEditorMessages.dialog_macros_no_selection_title,
                    SQLEditorMessages.dialog_macros_no_selection_message,
                    SWT.ICON_WARNING);
            return;
        }

        String text = macro.getQuery().replace(
                SQLMacrosConstants.SELECTION_PLACEHOLDER,
                hasSelection ? selectionText : SQLMacrosConstants.SELECTION_PLACEHOLDER);

        if (insertText) {
            IDocument document = editor.getDocument();
            if (document == null) {
                return;
            }
            try {
                document.replace(offset, length, text);
            } catch (BadLocationException e) {
                log.error("Can't insert macro '" + macro.getName() + "' into SQL editor", e);
                return;
            }
        }

        if (executeText && editor instanceof SQLEditor sqlEditor && sqlEditor.getDataSource() != null) {
            // Execute exactly the macro query without touching the editor document
            SQLQuery query = new SQLQuery(sqlEditor.getDataSource(), text, 0, text.length());
            sqlEditor.processQueries(List.of(query), false, false, false, true, null, null);
            sqlEditor.refreshActions();
        }

        if (insertText) {
            editor.getSelectionProvider().setSelection(new TextSelection(offset + text.length(), 0));
        }
    }

    /**
     * Returns the current text selection of the editor or null if no text selection is available.
     */
    @Nullable
    private static ITextSelection getTextSelection(@NotNull SQLEditorBase editor) {
        ISelectionProvider selectionProvider = editor.getSelectionProvider();
        if (selectionProvider != null && selectionProvider.getSelection() instanceof ITextSelection textSelection) {
            return textSelection;
        }
        return null;
    }
}