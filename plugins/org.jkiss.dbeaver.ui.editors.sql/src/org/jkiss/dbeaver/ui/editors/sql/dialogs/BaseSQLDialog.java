/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.SubEditorSite;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.dbeaver.utils.GeneralUtils;

public abstract class BaseSQLDialog extends BaseDialog {

    private static final Log log = Log.getLog(BaseSQLDialog.class);

    private IEditorSite subSite;
    private SQLEditorBase sqlViewer;
    private StringEditorInput sqlInput;

    public BaseSQLDialog(final IWorkbenchPartSite parentSite, String title, @Nullable DBPImage image) {
        this(parentSite.getShell(), parentSite, title, image);
    }

    public BaseSQLDialog(final Shell shell, final IWorkbenchPartSite parentSite, String title, @Nullable DBPImage image)
    {
        super(shell, title, image);
        this.subSite = new SubEditorSite(parentSite);
        this.sqlInput = new StringEditorInput(title, "", true, GeneralUtils.getDefaultFileEncoding());
    }

    public boolean isReadOnly() {
        return sqlInput.isReadOnly();
    }

    public void setReadOnly(boolean readOnly) {
        sqlInput.setReadOnly(readOnly);
    }

    public String getText() {
        return sqlInput.getBuffer().toString();
    }

    protected boolean isWordWrap() {
        return false;
    }

    protected boolean isLabelVisible() {
        return true;
    }

    protected Composite createSQLPanel(Composite parent)
    {
        Composite panel = UIUtils.createPlaceholder(parent, 1);
        panel.setLayoutData(new GridData(GridData.FILL_BOTH));

        if (isLabelVisible()) {
            UIUtils.createControlLabel(panel, SQLEditorMessages.pref_page_sql_format_label_SQLPreview);
        }
//        new Label(panel, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Composite editorPH = new Composite(panel, SWT.BORDER);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.verticalIndent = 3;
        gd.horizontalSpan = 1;
        gd.minimumHeight = 100;
        gd.minimumWidth = 100;
        editorPH.setLayoutData(gd);
        editorPH.setLayout(new FillLayout());

        sqlViewer = new SQLEditorBase() {
            @NotNull
            @Override
            public SQLDialect getSQLDialect() {
                return BaseSQLDialog.this.getSQLDialect();
            }

            @Override
            public DBCExecutionContext getExecutionContext() {
                return BaseSQLDialog.this.getExecutionContext();
            }

            @Override
            protected boolean isAnnotationRulerVisible() {
                return false;
            }
        };
        updateSQL();
        sqlViewer.createPartControl(editorPH);
        if (isWordWrap()) {
            Object text = sqlViewer.getAdapter(Control.class);
            if (text instanceof StyledText) {
                ((StyledText) text).setWordWrap(true);
            }
        }
        sqlViewer.reloadSyntaxRules();

        UIUtils.asyncExec(editorPH::layout);

        return panel;
    }

    protected SQLDialect getSQLDialect() {
        DBCExecutionContext executionContext = getExecutionContext();
        if (executionContext != null) {
            return executionContext.getDataSource().getSQLDialect();
        }
        return BasicSQLDialect.INSTANCE;
    }

    protected abstract DBCExecutionContext getExecutionContext();

    protected abstract String getSQLText();

    protected void createCopyButton(Composite parent) {
        createButton(parent, IDialogConstants.DETAILS_ID, SQLEditorMessages.dialog_view_sql_button_copy, false);
    }
    
    protected void createRefreshButton(Composite parent) {
        createButton(parent, IDialogConstants.RETRY_ID, SQLEditorMessages.dialog_view_sql_button_refresh, false);
    }

    protected void saveToClipboard()
    {
        CharSequence text = getSQLText();
        UIUtils.setClipboardContents(getShell().getDisplay(), TextTransfer.getInstance(), text);
    }

    @Override
    protected void buttonPressed(int buttonId)
    {
        if (buttonId == IDialogConstants.DETAILS_ID) {
            saveToClipboard();
            super.buttonPressed(IDialogConstants.CANCEL_ID);
        } else {
            super.buttonPressed(buttonId);
        }
    }

    @Override
    protected void okPressed() {
        if (sqlViewer != null && sqlViewer.getTextViewer() != null && sqlViewer.getTextViewer().getDocument() != null) {
            sqlInput.setText(sqlViewer.getTextViewer().getDocument().get());
        }
        super.okPressed();
    }

    @Override
    public boolean close() {
        if (sqlViewer != null) {
            try {
                sqlViewer.dispose();
            } catch (Exception e) {
                log.debug("Error disposing embedded SQL editor", e);
            }
        }
        return super.close();
    }

    protected void updateSQL()
    {
        try {
            this.sqlInput.setText(getSQLText());
            if (sqlViewer.getSite() != null) {
                sqlViewer.setInput(sqlInput);
            } else {
                sqlViewer.init(subSite, sqlInput);
            }
            sqlViewer.reloadSyntaxRules();
        } catch (PartInitException e) {
            DBWorkbench.getPlatformUI().showError(getShell().getText(), null, e);
        }
    }
}