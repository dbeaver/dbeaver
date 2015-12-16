/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.SubEditorSite;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.utils.ContentUtils;

public abstract class BaseSQLDialog extends BaseDialog {

    private IEditorSite subSite;
    private SQLEditorBase sqlViewer;

    public BaseSQLDialog(final IWorkbenchPartSite parentSite, String title, @Nullable DBPImage image)
    {
        super(parentSite.getShell(), title, image);
        this.subSite = new SubEditorSite(parentSite);
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
            StringEditorInput sqlInput = new StringEditorInput(getShell().getText(), getSQLText(), true, ContentUtils.DEFAULT_CHARSET);
            sqlViewer.init(subSite, sqlInput);
        } catch (PartInitException e) {
            UIUtils.showErrorDialog(getShell(), getShell().getText(), null, e);
        }
    }
}