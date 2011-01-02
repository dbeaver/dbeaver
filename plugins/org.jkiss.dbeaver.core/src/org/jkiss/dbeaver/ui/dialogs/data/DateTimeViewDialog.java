/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.data;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.exec.DBCColumnMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCDateTimeValueHandler;

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
        label.setText("Value: ");

        int style = SWT.BORDER;
        if (getValueController().isReadOnly()) {
            style |= SWT.READ_ONLY;
        }

        DBCColumnMetaData column = getValueController().getColumnMetaData();
        boolean isDate = column.getValueType() == java.sql.Types.DATE;
        boolean isTime = column.getValueType() == java.sql.Types.TIME;
        boolean isTimeStamp = column.getValueType() == java.sql.Types.TIMESTAMP;

        dateEditor = isDate || isTimeStamp ? new DateTime(dialogGroup, SWT.CALENDAR | style) : null;
        timeEditor = isTime || isTimeStamp ? new DateTime(dialogGroup, SWT.TIME | SWT.LONG | style) : null;

        if (dateEditor != null) {
            GridData gd = new GridData();
            gd.horizontalAlignment = GridData.CENTER;
            dateEditor.setLayoutData(gd);
            if (value instanceof Date) {
                Calendar cl = Calendar.getInstance();
                cl.setTime((Date)value);
                dateEditor.setDate(cl.get(Calendar.YEAR), cl.get(Calendar.MONTH), cl.get(Calendar.DAY_OF_MONTH));
            }
        }
        if (timeEditor != null) {
            GridData gd = new GridData();
            gd.horizontalAlignment = GridData.CENTER;
            timeEditor.setLayoutData(gd);
            if (value instanceof Date) {
                Calendar cl = Calendar.getInstance();
                cl.setTime((Date)value);
                timeEditor.setTime(cl.get(Calendar.HOUR_OF_DAY), cl.get(Calendar.MINUTE), cl.get(Calendar.SECOND));
            }
        }

        return dialogGroup;
    }

    @Override
    protected Object getEditorValue()
    {
        return JDBCDateTimeValueHandler.getDate(dateEditor, timeEditor);
    }

}