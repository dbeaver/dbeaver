/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.dialogs.sql;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSource;

public class ViewSQLDialog extends BaseSQLDialog {

    private DBPDataSource dataSource;
    private String text;
    private boolean showSaveButton = false;
    private boolean enlargeViewPanel = true;
    private boolean wordWrap = false;

    public ViewSQLDialog(final IWorkbenchPartSite parentSite, @Nullable DBPDataSource dataSource, String title, @Nullable Image image, String text)
    {
        super(parentSite, title, image);
        this.dataSource = dataSource;
        this.text = text;
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
    protected Control createDialogArea(Composite parent)
    {
        Composite composite = (Composite) super.createDialogArea(parent);
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

    @Nullable
    @Override
    public DBPDataSource getDataSource()
    {
        return dataSource;
    }
}