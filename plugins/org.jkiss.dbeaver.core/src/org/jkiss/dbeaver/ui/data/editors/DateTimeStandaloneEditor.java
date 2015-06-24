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

package org.jkiss.dbeaver.ui.data.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomTimeEditor;
import org.jkiss.dbeaver.ui.dialogs.data.ValueViewDialog;

import java.util.Date;

/**
 * DateTimeStandaloneEditor
 */
public class DateTimeStandaloneEditor extends ValueViewDialog {

    private final DateTimeEditorHelper helper;
    private CustomTimeEditor timeEditor;

    public DateTimeStandaloneEditor(IValueController valueController, DateTimeEditorHelper helper) {
        super(valueController);
        this.helper = helper;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        IValueController valueController = getValueController();
        Object value = valueController.getValue();

        Composite dialogGroup = (Composite)super.createDialogArea(parent);
        Composite panel = UIUtils.createPlaceholder(dialogGroup, 3);
        panel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        int style = SWT.BORDER;
        if (valueController.isReadOnly()) {
            style |= SWT.READ_ONLY;
        }

        UIUtils.createControlLabel(panel, "Time").setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        DBDDataFormatter formatter = helper.getFormatter(valueController, valueController.getValueType());
        timeEditor = new CustomTimeEditor(panel, style, formatter);

        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalAlignment = GridData.CENTER;
        timeEditor.getControl().setLayoutData(gd);

        primeEditorValue(value);

        Button button = UIUtils.createPushButton(panel, "Set Current", null);
        button.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        button.setEnabled(!valueController.isReadOnly());
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                primeEditorValue(new Date());
            }
        });

        return dialogGroup;
    }

    @Override
    public void primeEditorValue(@Nullable Object value)
    {
        timeEditor.setValue(value);
    }

    @Override
    public Object extractEditorValue() throws DBException {
        return timeEditor.getValue();
    }

    @Override
    public Control getControl()
    {
        return null;
    }

}