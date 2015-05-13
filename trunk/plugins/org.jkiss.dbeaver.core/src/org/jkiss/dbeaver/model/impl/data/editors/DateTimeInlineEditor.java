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
package org.jkiss.dbeaver.model.impl.data.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomTimeEditor;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

/**
* DateTimeInlineEditor
*/
public class DateTimeInlineEditor extends BaseValueEditor<Control> {
    private final DateTimeEditorHelper helper;
    private DateTime dateEditor;
    private CustomTimeEditor timeEditor;

    public DateTimeInlineEditor(DBDValueController controller, DateTimeEditorHelper helper) {
        super(controller);
        this.helper = helper;
    }

    @Override
    public void primeEditorValue(@Nullable Object value) throws DBException
    {
        Calendar cl = Calendar.getInstance();
        if (value == null) {
            if (dateEditor != null) {
                dateEditor.setDate(cl.get(Calendar.YEAR), 0, 1);
            }
            if (timeEditor != null) {
                timeEditor.setValue(null);
            }
        } else if (value instanceof Date) {
            cl.setTime((Date) value);
            if (dateEditor != null) {
                dateEditor.setDate(cl.get(Calendar.YEAR), cl.get(Calendar.MONTH), cl.get(Calendar.DAY_OF_MONTH));
            }
            if (timeEditor != null) {
                timeEditor.setValue(value);
            }
        }
    }

    @Override
    protected Control createControl(Composite editPlaceholder)
    {
        boolean inline = valueController.getEditType() == DBDValueController.EditType.INLINE;
        final Composite dateTimeGroup = inline ?
            valueController.getEditPlaceholder() :
            new Composite(valueController.getEditPlaceholder(), SWT.BORDER);
        if (!inline) {
            dateTimeGroup.setLayout(new GridLayout(2, false));
        }

        boolean isDate = helper.isDate(valueController);
        boolean isTime = helper.isTime(valueController);
        boolean isTimeStamp = helper.isTimestamp(valueController) || (!isDate && !isTime);

        if (!inline && (isDate || isTimeStamp)) {
            UIUtils.createControlLabel(dateTimeGroup, "Date");
        }
        if (isDate || isTimeStamp) {
            dateEditor = new DateTime(dateTimeGroup,
                (inline ? SWT.DATE | SWT.DROP_DOWN | SWT.MEDIUM | SWT.BORDER : SWT.DATE | SWT.DROP_DOWN | SWT.LONG));
            dateEditor.setEnabled(!valueController.isReadOnly());
        }
        if (!inline && (isTime || isTimeStamp)) {
            UIUtils.createControlLabel(dateTimeGroup, "Time");
        }
        if (isTime || isTimeStamp) {
            timeEditor = new CustomTimeEditor(dateTimeGroup,
                (inline ? SWT.BORDER : SWT.NONE) | SWT.TIME | SWT.LONG,
                helper.getFormatter(DBDDataFormatter.TYPE_NAME_TIME));
            timeEditor.setEnabled(!valueController.isReadOnly());
            if (!inline) {
                timeEditor.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            }
        }

        if (dateEditor != null) {
            if (timeEditor != null) {
                initInlineControl(timeEditor.getControl());
            }
            return dateEditor;
        }
        return timeEditor == null ? dateEditor : timeEditor.getControl();
    }

    @Override
    public Object extractEditorValue()
    {
        return getDateFromControls(dateEditor, timeEditor);
    }

    public static Date getDateFromControls(DateTime dateEditor, CustomTimeEditor timeEditor) {
        Date timeValue = null;
        if (timeEditor != null) {
            timeValue = timeEditor.getValue();
        }
        if (dateEditor != null) {
            Calendar cl = Calendar.getInstance();
            cl.clear();
            if (timeValue != null) {
                cl.setTime(timeValue);
            }
            cl.set(Calendar.YEAR, dateEditor.getYear());
            cl.set(Calendar.MONTH, dateEditor.getMonth());
            cl.set(Calendar.DAY_OF_MONTH, dateEditor.getDay());
            Date dateTimeValue = cl.getTime();
            if (timeValue instanceof Timestamp && ((Timestamp) timeValue).getNanos() > 0) {
                dateTimeValue = new Timestamp(dateTimeValue.getTime());
                ((Timestamp)dateTimeValue).setNanos(((Timestamp) timeValue).getNanos());
            }
            return dateTimeValue;
        } else {
            return timeValue;
        }
    }

}
