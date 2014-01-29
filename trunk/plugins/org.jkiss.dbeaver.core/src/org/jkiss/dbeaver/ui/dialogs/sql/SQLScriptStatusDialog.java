/*
 * Copyright (C) 2010-2013 Serge Rieder
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

public abstract class SQLScriptStatusDialog<T extends DBSObject> extends BaseDialog implements SQLScriptProgressListener<T> {

    private ProgressBar progressBar;
    private int objectCount;

    public SQLScriptStatusDialog(final Shell shell, String title, @Nullable Image image)
    {
        super(shell, title, image);
    }


    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite)super.createDialogArea(parent);
        UIUtils.createCheckbox(composite, "sdfsdf", true);

        progressBar = new ProgressBar(composite, SWT.HORIZONTAL);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.widthHint = 200;
        progressBar.setLayoutData(gd);
        progressBar.setMinimum(0);
        progressBar.setMaximum(this.objectCount);
        return composite;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        Button button = createButton(parent, IDialogConstants.CLOSE_ID, IDialogConstants.CLOSE_LABEL, true);
        button.setEnabled(false);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.CLOSE_ID) {
            okPressed();
        } else {
            super.buttonPressed(buttonId);
        }
    }

    @Override
    public void beginScriptProcessing(int objectCount) {
        this.objectCount = objectCount;
        this.open();
    }

    @Override
    public void endScriptProcessing() {
        getButton(IDialogConstants.CLOSE_ID).setEnabled(true);
    }

    @Override
    public void beginObjectProcessing(T object, int objectNumber) {
        progressBar.setSelection(objectNumber + 1);
    }

    @Override
    public void endObjectProcessing(T object) {

    }

    @Override
    public void processObjectResults(T object, DBCResultSet resultSet) {

    }
}