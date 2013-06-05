/*
 * Copyright (C) 2010-2012 Serge Rieder
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

package org.jkiss.dbeaver.ui.dialogs.data;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCDateTimeValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.util.Calendar;
import java.util.Date;

/**
 * DateTimeViewDialog
 */
public class DateTimeViewDialog extends ValueViewDialog {

    private DateTime dateEditor;
    private DateTime timeEditor;

    public DateTimeViewDialog(DBDValueController valueController) {
        super(valueController);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Object value = getValueController().getValue();

        Composite dialogGroup = (Composite)super.createDialogArea(parent);

        Label label = new Label(dialogGroup, SWT.NONE);
        label.setText(CoreMessages.dialog_data_label_value);

        int style = SWT.BORDER;
        if (getValueController().isReadOnly()) {
            style |= SWT.READ_ONLY;
        }

        DBSTypedObject valueType = getValueController().getValueType();
        boolean isDate = valueType.getTypeID() == java.sql.Types.DATE;
        boolean isTime = valueType.getTypeID() == java.sql.Types.TIME;
        boolean isTimeStamp = valueType.getTypeID() == java.sql.Types.TIMESTAMP;

        dateEditor = isDate || isTimeStamp ? new DateTime(dialogGroup, SWT.CALENDAR | style) : null;
        timeEditor = isTime || isTimeStamp ? new DateTime(dialogGroup, SWT.TIME | SWT.LONG | style) : null;

        if (dateEditor != null) {
            GridData gd = new GridData();
            gd.horizontalAlignment = GridData.CENTER;
            dateEditor.setLayoutData(gd);
        }
        if (timeEditor != null) {
            GridData gd = new GridData();
            gd.horizontalAlignment = GridData.CENTER;
            timeEditor.setLayoutData(gd);
        }
        setEditorValue(value);

        return dialogGroup;
    }

    @Override
    protected Object getEditorValue()
    {
        return JDBCDateTimeValueHandler.getDate(dateEditor, timeEditor);
    }

    @Override
    protected void setEditorValue(Object value)
    {
        if (value instanceof Date) {
            Calendar cl = Calendar.getInstance();
            cl.setTime((Date)value);
            if (dateEditor != null) {
                dateEditor.setDate(cl.get(Calendar.YEAR), cl.get(Calendar.MONTH), cl.get(Calendar.DAY_OF_MONTH));
            }
            if (timeEditor != null) {
                timeEditor.setTime(cl.get(Calendar.HOUR_OF_DAY), cl.get(Calendar.MINUTE), cl.get(Calendar.SECOND));
            }
        }
    }

    @Override
    public Control getControl()
    {
        return null;
    }

    @Override
    public void refreshValue()
    {
        Object value = getValueController().getValue();
        if (value instanceof Date) {
            Calendar cl = Calendar.getInstance();
            cl.setTime((Date)value);
            if (dateEditor != null) {
                dateEditor.setDate(cl.get(Calendar.YEAR), cl.get(Calendar.MONTH), cl.get(Calendar.DAY_OF_MONTH));
            }
            if (timeEditor != null) {
                timeEditor.setTime(cl.get(Calendar.HOUR_OF_DAY), cl.get(Calendar.MINUTE), cl.get(Calendar.SECOND));
            }
        }
    }

}