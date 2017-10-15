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

    public void setSQLText(String text) {
        this.text = text;
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