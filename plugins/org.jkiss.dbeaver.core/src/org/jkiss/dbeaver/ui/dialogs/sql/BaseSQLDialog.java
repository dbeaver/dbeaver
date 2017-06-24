/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.dialogs.sql;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
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
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.sql.SQLDataSource;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.SubEditorSite;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.utils.GeneralUtils;

public abstract class BaseSQLDialog extends BaseDialog {

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
            UIUtils.createControlLabel(panel, "SQL Preview");
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

        parent.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                sqlViewer.dispose();
            }
        });

        return panel;
    }

    protected SQLDialect getSQLDialect() {
        DBCExecutionContext executionContext = getExecutionContext();
        if (executionContext != null && executionContext.getDataSource() instanceof SQLDataSource) {
            return ((SQLDataSource) executionContext.getDataSource()).getSQLDialect();
        }
        return null;
    }

    protected abstract DBCExecutionContext getExecutionContext();

    protected abstract String getSQLText();

    protected void createCopyButton(Composite parent)
    {
        createButton(parent, IDialogConstants.DETAILS_ID, CoreMessages.dialog_view_sql_button_copy, false);
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
        } else {
            super.buttonPressed(buttonId);
        }
    }

    protected void updateSQL()
    {
        try {
            this.sqlInput.setText(getSQLText());
            sqlViewer.init(subSite, sqlInput);
            sqlViewer.reloadSyntaxRules();
        } catch (PartInitException e) {
            DBUserInterface.getInstance().showError(getShell().getText(), null, e);
        }
    }
}