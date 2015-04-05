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
package org.jkiss.dbeaver.model.impl.data.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.Calendar;
import java.util.Date;

/**
* DateTimeInlineEditor
*/
public class DateTimeInlineEditor extends BaseValueEditor<DateTime> {
    private final DateTimeEditorHelper helper;
    private DateTime dateEditor;
    private DateTime timeEditor;

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
                timeEditor.setTime(0, 0, 0);
            }
        } else if (value instanceof Date) {
            cl.setTime((Date) value);
            if (dateEditor != null) {
                dateEditor.setDate(cl.get(Calendar.YEAR), cl.get(Calendar.MONTH), cl.get(Calendar.DAY_OF_MONTH));
            }
            if (timeEditor != null) {
                timeEditor.setTime(cl.get(Calendar.HOUR_OF_DAY), cl.get(Calendar.MINUTE), cl.get(Calendar.SECOND));
            }
        }
    }

    @Override
    protected DateTime createControl(Composite editPlaceholder)
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
            timeEditor = new DateTime(dateTimeGroup,
                (inline ? SWT.BORDER : SWT.NONE) | SWT.TIME | SWT.LONG);
            timeEditor.setEnabled(!valueController.isReadOnly());
        }

        if (dateEditor != null) {
            if (timeEditor != null) {
                initInlineControl(timeEditor);
            }
            return dateEditor;
        }
        return timeEditor;
    }

    @Override
    public Object extractEditorValue()
    {
        Calendar cl = getCalendarFromControls(dateEditor, timeEditor);
        return helper.getValueFromMillis(valueController, cl.getTimeInMillis());
    }

    public static Calendar getCalendarFromControls(DateTime dateEditor, DateTime timeEditor) {
        Calendar cl = Calendar.getInstance();
        cl.clear();
        if (dateEditor != null) {
            cl.set(Calendar.YEAR, dateEditor.getYear());
            cl.set(Calendar.MONTH, dateEditor.getMonth());
            cl.set(Calendar.DAY_OF_MONTH, dateEditor.getDay());
        }
        if (timeEditor != null) {
            cl.set(Calendar.HOUR_OF_DAY, timeEditor.getHours());
            cl.set(Calendar.MINUTE, timeEditor.getMinutes());
            cl.set(Calendar.SECOND, timeEditor.getSeconds());
            cl.set(Calendar.MILLISECOND, 0);
        }
        return cl;
    }

}
