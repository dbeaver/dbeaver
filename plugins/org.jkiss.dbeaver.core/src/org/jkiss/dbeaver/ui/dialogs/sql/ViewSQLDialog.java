/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.ui.UIUtils;

public class ViewSQLDialog extends BaseSQLDialog {

    private static final String DIALOG_ID = "DBeaver.ViewSQLDialog";//$NON-NLS-1$

    private DBCExecutionContext context;
    private String text;
    private boolean showSaveButton = false;
    private boolean enlargeViewPanel = true;
    private boolean wordWrap = false;

    public ViewSQLDialog(final IWorkbenchPartSite parentSite, @Nullable DBCExecutionContext context, String title, @Nullable DBPImage image, String text)
    {
        super(parentSite, title, image);
        this.context = context;
        this.text = text;
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings()
    {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    public void setShowSaveButton(boolean showSaveButton)
    {
        this.showSaveButton = showSaveButton;
    }

    public void setEnlargeViewPanel(boolean enlargeViewPanel) {
        this.enlargeViewPanel = enlargeViewPanel;
    }

    @Override
    protected boolean isWordWrap() {
        return wordWrap;
    }

    public void setWordWrap(boolean wordWrap) {
        this.wordWrap = wordWrap;
    }

    @Override
    protected Composite createDialogArea(Composite parent)
    {
        Composite composite = super.createDialogArea(parent);
        Composite sqlPanel = createSQLPanel(composite);
        GridData gd = (GridData) sqlPanel.getLayoutData();
        if (enlargeViewPanel) {
            gd.widthHint = 500;
            gd.heightHint = 400;
        } else {
            gd.widthHint = 400;
            gd.heightHint = 100;
        }
        return sqlPanel;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        if (showSaveButton) {
            createButton(parent, IDialogConstants.PROCEED_ID, CoreMessages.dialog_view_sql_button_persist, true);
            createCopyButton(parent);
            createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        } else {
            createCopyButton(parent);
            createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, true);
        }
    }

    @Override
    protected String getSQLText()
    {
        return text;
    }

    @Override
    protected void buttonPressed(int buttonId)
    {
        if (buttonId == IDialogConstants.PROCEED_ID) {
            setReturnCode(IDialogConstants.PROCEED_ID);
            close();
        } else {
            super.buttonPressed(buttonId);
        }
    }

    @Override
    protected DBCExecutionContext getExecutionContext() {
        return context;
    }
}