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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.SubEditorSite;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.UUID;

/**
 * Create/edit single SQL macro dialog.
 */
public class SQLMacrosEditDialog extends BaseDialog {

    private static final Log log = Log.getLog(SQLMacrosEditDialog.class);

    private SQLMacro macro;
    private Text nameText;
    private Combo shortcutCombo;
    private SQLEditorBase sqlEditor;
    @Nullable
    private Text queryText;
    private Combo actionCombo;

    public SQLMacrosEditDialog(@NotNull Shell parentShell, @Nullable SQLMacro macro) {
        super(parentShell, macro == null
                ? SQLEditorMessages.dialog_macros_edit_new_title
                : SQLEditorMessages.dialog_macros_edit_edit_title, null);
        this.macro = macro;
    }

    /**
     * Returns the edited macro or null if the dialog was cancelled.
     */
    @Nullable
    public SQLMacro getMacro() {
        return macro;
    }

    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        Composite composite = super.createDialogArea(parent);

        nameText = UIUtils.createLabelText(composite, SQLEditorMessages.dialog_macros_edit_label_name,
                macro == null ? "" : macro.getName()); //$NON-NLS-1$
        nameText.addModifyListener(textModifyListener());

        shortcutCombo = UIUtils.createLabelCombo(composite, SQLEditorMessages.dialog_macros_edit_label_shortcut, SWT.READ_ONLY);
        for (int i = 0; i < SQLMacrosConstants.MACRO_KEY_COUNT; i++) {
            shortcutCombo.add(SQLMacrosConstants.getShortcutLabel(i));
        }
        shortcutCombo.select(macro == null
                ? 0
                : Math.clamp(macro.getShortcutIndex(), 0, SQLMacrosConstants.MACRO_KEY_COUNT - 1));
        shortcutCombo.addModifyListener(textModifyListener());

        SQLEditorBase queryViewer = createQueryEditor(composite);
        if (queryViewer == null) {
            queryText = UIUtils.createLabelText(composite, SQLEditorMessages.dialog_macros_edit_label_query,
                    macro == null ? "" : macro.getQuery(), SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL, queryLayout()); //$NON-NLS-1$
            queryText.addModifyListener(textModifyListener());
        }

        actionCombo = UIUtils.createLabelCombo(composite, SQLEditorMessages.dialog_macros_edit_label_action, SWT.READ_ONLY);
        for (MacroAction action : MacroAction.values()) {
            actionCombo.add(actionLabel(action));
        }
        MacroAction initialAction = macro == null ? MacroAction.INSERT : macro.getAction();
        actionCombo.select(initialAction.ordinal());
        actionCombo.addModifyListener(textModifyListener());

        UIUtils.createInfoLabel(composite, SQLEditorMessages.dialog_macros_selection_placeholder_hint, SWT.WRAP, 1);

        UIUtils.asyncExec(() -> {
            nameText.setFocus();
            nameText.selectAll();
        });

        return composite;
    }

    @NotNull
    private GridData queryLayout() {
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.minimumHeight = 100;
        gd.widthHint = 400;
        gd.heightHint = 120;
        return gd;
    }

    @NotNull
    private ModifyListener textModifyListener() {
        return e -> updateButtonsState();
    }

    /**
     * Creates an embedded SQL editor with syntax highlighting for the macro query.
     * Returns null if the SQL editor cannot be created (e.g. no active workbench part site),
     * in which case a plain text widget is used instead.
     */
    @Nullable
    private SQLEditorBase createQueryEditor(@NotNull Composite parent) {
        IWorkbenchPartSite partSite = findActivePartSite();
        if (partSite == null) {
            return null;
        }
        SQLEditorBase queryViewer = new SQLEditorBase() {
            @Nullable
            @Override
            public DBCExecutionContext getExecutionContext() {
                return null;
            }

            @Override
            protected boolean isAnnotationRulerVisible() {
                return false;
            }
        };
        try {
            queryViewer.init(new SubEditorSite(partSite),
                    new StringEditorInput(
                            SQLEditorMessages.dialog_macros_edit_label_query,
                            macro == null ? "" : macro.getQuery(), //$NON-NLS-1$
                            false,
                            GeneralUtils.getDefaultFileEncoding()));
        } catch (PartInitException e) {
            log.error("Error initializing macro SQL editor", e);
            return null;
        }

        UIUtils.createControlLabel(parent, SQLEditorMessages.dialog_macros_edit_label_query);

        Composite editorPH = new Composite(parent, SWT.NONE);
        editorPH.setLayoutData(queryLayout());
        editorPH.setLayout(new FillLayout());
        queryViewer.createPartControl(editorPH);
        queryViewer.reloadSyntaxRules();
        queryViewer.getEditorControl().addModifyListener(textModifyListener());
        editorPH.addDisposeListener(e -> queryViewer.dispose());

        this.sqlEditor = queryViewer;
        return queryViewer;
    }

    /**
     * Returns the part site of the currently active workbench part.
     * It is used to create an embedded SQL editor for the macro query.
     */
    @Nullable
    private static IWorkbenchPartSite findActivePartSite() {
        IWorkbenchPage page = UIUtils.getActiveWorkbenchWindow().getActivePage();
        IWorkbenchPart part = page == null ? null : page.getActivePart();
        return part == null ? null : part.getSite();
    }

    /**
     * Returns the query text either from the embedded SQL editor or from the plain text widget.
     */
    @NotNull
    private String getQueryText() {
        if (sqlEditor != null && sqlEditor.getTextViewer() != null) {
            return sqlEditor.getTextViewer().getDocument().get();
        }
        return queryText == null ? "" : queryText.getText(); //$NON-NLS-1$
    }

    private void updateButtonsState() {
        if (getButton(IDialogConstants.OK_ID) != null) {
            getButton(IDialogConstants.OK_ID).setEnabled(isInputValid());
        }
    }

    private boolean isInputValid() {
        return !nameText.getText().trim().isEmpty() && !getQueryText().trim().isEmpty();
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        updateButtonsState();
    }

    @NotNull
    private String actionLabel(@NotNull MacroAction action) {
        return switch (action) {
            case INSERT -> SQLEditorMessages.dialog_macros_action_insert;
            case INSERT_AND_EXECUTE -> SQLEditorMessages.dialog_macros_action_insert_execute;
            case EXECUTE_ONLY -> SQLEditorMessages.dialog_macros_action_execute;
        };
    }

    @Override
    protected void okPressed() {
        String id = macro != null ? macro.getId() : UUID.randomUUID().toString();
        macro = new SQLMacro(
                id,
                nameText.getText().trim(),
                getQueryText().trim(),
                shortcutCombo.getSelectionIndex(),
                MacroAction.values()[actionCombo.getSelectionIndex()]);
        super.okPressed();
    }
}