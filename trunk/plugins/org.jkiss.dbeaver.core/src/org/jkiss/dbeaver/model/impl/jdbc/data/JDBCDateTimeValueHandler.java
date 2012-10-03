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
package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.data.DBDValueHandler2;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.data.DefaultDataFormatter;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.dialogs.data.DateTimeViewDialog;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

/**
 * JDBC string value handler
 */
public class JDBCDateTimeValueHandler extends JDBCAbstractValueHandler implements DBDValueHandler2 {

    public static final String TYPE_NAME_DATE = "date"; //$NON-NLS-1$
    public static final String TYPE_NAME_TIME = "time"; //$NON-NLS-1$
    public static final String TYPE_NAME_TIMESTAMP = "timestamp"; //$NON-NLS-1$

    private DBDDataFormatterProfile formatterProfile;
    private DBDDataFormatter formatter;

    public JDBCDateTimeValueHandler(DBDDataFormatterProfile formatterProfile)
    {
        this.formatterProfile = formatterProfile;
    }

    private DBDDataFormatter getFormatter(String typeId)
    {
        if (formatter == null) {
            try {
                formatter = formatterProfile.createFormatter(typeId);
            } catch (Exception e) {
                log.error("Could not create formatter for datetime value handler", e); //$NON-NLS-1$
                formatter = DefaultDataFormatter.INSTANCE;
            }
        }
        return formatter;
    }

    @Override
    protected Object getColumnValue(DBCExecutionContext context, JDBCResultSet resultSet, DBSTypedObject column,
                                    int columnIndex)
        throws DBCException, SQLException
    {
        switch (column.getTypeID()) {
        case java.sql.Types.TIME:
            return resultSet.getTime(columnIndex);
        case java.sql.Types.DATE:
            return resultSet.getDate(columnIndex);
        default:
            return resultSet.getTimestamp(columnIndex);
        }
    }

    @Override
    protected void bindParameter(JDBCExecutionContext context, JDBCPreparedStatement statement, DBSTypedObject paramType,
                                 int paramIndex, Object value) throws SQLException
    {
        if (value == null) {
            statement.setNull(paramIndex, paramType.getTypeID());
        } else {
            switch (paramType.getTypeID()) {
            case java.sql.Types.TIME:
                statement.setTime(paramIndex, getTimeValue(value));
                break;
            case java.sql.Types.DATE:
                statement.setDate(paramIndex, getDateValue(value));
                break;
            default:
                statement.setTimestamp(paramIndex, getTimestampValue(value));
                break;
            }
        }
    }

    @Override
    public boolean editValue(final DBDValueController controller)
        throws DBException
    {
        if (controller.isInlineEdit()) {
            Object value = controller.getValue();

            final Composite dateTimeGroup = controller.getInlinePlaceholder();

            boolean isDate = controller.getAttributeMetaData().getTypeID() == java.sql.Types.DATE;
            boolean isTime = controller.getAttributeMetaData().getTypeID() == java.sql.Types.TIME;
            boolean isTimeStamp = controller.getAttributeMetaData().getTypeID() == java.sql.Types.TIMESTAMP;

            final DateTime dateEditor = isDate || isTimeStamp ? new DateTime(dateTimeGroup, SWT.BORDER | SWT.DATE | SWT.MEDIUM | SWT.DROP_DOWN) : null;
            final DateTime timeEditor = isTime || isTimeStamp ? new DateTime(dateTimeGroup, SWT.BORDER | SWT.TIME | SWT.LONG) : null;

            if (dateEditor != null) {
                initInlineControl(controller, dateEditor, new ValueExtractor<DateTime>() {
                    @Override
                    public Object getValueFromControl(DateTime control)
                    {
                        return getDate(dateEditor, timeEditor);
                    }
                });
                if (value instanceof Date) {
                    Calendar cl = Calendar.getInstance();
                    cl.setTime((Date)value);
                    dateEditor.setDate(cl.get(Calendar.YEAR), cl.get(Calendar.MONTH), cl.get(Calendar.DAY_OF_MONTH));
                }
            }
            if (timeEditor != null) {
                initInlineControl(controller, timeEditor, new ValueExtractor<DateTime>() {
                    @Override
                    public Object getValueFromControl(DateTime control)
                    {
                        return getDate(dateEditor, timeEditor);
                    }
                });
                if (value instanceof Date) {
                    Calendar cl = Calendar.getInstance();
                    cl.setTime((Date)value);
                    timeEditor.setTime(cl.get(Calendar.HOUR_OF_DAY), cl.get(Calendar.MINUTE), cl.get(Calendar.SECOND));
                }
            }

/*
            // There is a bug in windows. First time date control gain focus it renders cell editor incorrectly.
            // Let's focus on it in async mode
*/
            dateTimeGroup.getDisplay().asyncExec(new Runnable() {
                @Override
                public void run()
                {
                    dateTimeGroup.setFocus();
                }
            });
            return true;
        } else {
            DateTimeViewDialog dialog = new DateTimeViewDialog(controller);
            dialog.open();
            return true;
        }
    }

    @Override
    public String getValueDisplayString(DBSTypedObject column, Object value)
    {
        if (value == null) {
            return super.getValueDisplayString(column, value);
        }
        switch (column.getTypeID()) {
        case java.sql.Types.TIME:
            return getFormatter(TYPE_NAME_TIME).formatValue(value);
        case java.sql.Types.DATE:
            return getFormatter(TYPE_NAME_DATE).formatValue(value);
        default:
            return getFormatter(TYPE_NAME_TIMESTAMP).formatValue(value);
        }
    }

    @Override
    public int getFeatures()
    {
        return FEATURE_VIEWER | FEATURE_EDITOR | FEATURE_INLINE_EDITOR;
    }

    @Override
    public Class getValueObjectType()
    {
        return Date.class;
    }

    @Override
    public Object copyValueObject(DBCExecutionContext context, DBSTypedObject column, Object value)
        throws DBCException
    {
        if (value instanceof Timestamp) {
            Timestamp copy = new Timestamp(((Timestamp)value).getTime());
            copy.setNanos(((Timestamp)value).getNanos());
            return copy;
        } else if (value instanceof java.sql.Time) {
            return new java.sql.Time(((java.sql.Time)value).getTime());
        } else if (value instanceof java.sql.Date) {
            return new java.sql.Date(((java.sql.Date)value).getTime());
        } else if (value instanceof java.util.Date) {
            return new java.util.Date(((java.util.Date)value).getTime());
        } else {
            // Not supported
            return null;
        }
    }

    @Override
    public Object getValueFromClipboard(DBSTypedObject column, Clipboard clipboard)
    {
        String strValue = (String) clipboard.getContents(TextTransfer.getInstance());
        if (CommonUtils.isEmpty(strValue)) {
            return null;
        }
        Object dateValue;
        try {
            switch (column.getTypeID()) {
            case java.sql.Types.TIME:
                dateValue = getFormatter(TYPE_NAME_TIME).parseValue(strValue);
                break;
            case java.sql.Types.DATE:
                dateValue = getFormatter(TYPE_NAME_DATE).parseValue(strValue);
                break;
            default:
                dateValue = getFormatter(TYPE_NAME_TIMESTAMP).parseValue(strValue);
                break;
            }
        } catch (ParseException e) {
            return null;
        }
        return dateValue;
    }

    @Override
    public void fillContextMenu(IMenuManager menuManager, final DBDValueController controller)
        throws DBCException
    {
        menuManager.add(new Action(CoreMessages.model_jdbc_set_to_current_time) {
            @Override
            public void run() {
                controller.updateValue(new Date());
            }
        });
    }

    public static Date getDate(DateTime dateEditor, DateTime timeEditor)
    {
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
        if (timeEditor == null) {
            return new java.sql.Date(cl.getTimeInMillis());
        } else if (dateEditor == null) {
            return new java.sql.Time(cl.getTimeInMillis());
        } else {
            return new Timestamp(cl.getTimeInMillis());
        }
    }

    @Override
    public String getValueDisplayString(DBSTypedObject column, String format, Object value)
    {
        if (value instanceof Date && DBConstants.FORMAT_SQL.equals(format)) {
            Calendar cal = Calendar.getInstance();
            cal.setTime((Date) value);
            final String hourOfDay = getTwoDigitValue(cal.get(Calendar.HOUR_OF_DAY) + 1);
            final String minutes = getTwoDigitValue(cal.get(Calendar.MINUTE));
            final String seconds = getTwoDigitValue(cal.get(Calendar.SECOND));
            final String year = String.valueOf(cal.get(Calendar.YEAR));
            final String month = getTwoDigitValue(cal.get(Calendar.MONTH) + 1);
            final String dayOfMonth = getTwoDigitValue(cal.get(Calendar.DAY_OF_MONTH));
            switch (column.getTypeID()) {
            case java.sql.Types.TIME:
                return "TO_DATE('" + hourOfDay + ":" + minutes + ":" + seconds + "','HH24:MI:SS')";
            case java.sql.Types.DATE:
                return "TO_DATE('" + year + "-" + month + "-" + dayOfMonth + "','YYYY-MM-DD')";
            default:
                return "TO_DATE('" + year + "-" + month + "-" + dayOfMonth +
                    " " + hourOfDay + ":" + minutes + ":" + seconds +
                    "','YYYY-MM-DD HH24:MI:SS')";
            }
        } else {
            return getValueDisplayString(column, value);
        }
    }

    private static java.sql.Time getTimeValue(Object value)
    {
        if (value instanceof java.sql.Time) {
            return (java.sql.Time)value;
        } else if (value instanceof Date) {
            return new java.sql.Time(((Date) value).getTime());
        } else if (value != null) {
            return Time.valueOf(value.toString());
        } else {
            return null;
        }
    }

    private static java.sql.Date getDateValue(Object value)
    {
        if (value instanceof java.sql.Date) {
            return (java.sql.Date)value;
        } else if (value instanceof Date) {
            return new java.sql.Date(((Date) value).getTime());
        } else if (value != null) {
            return java.sql.Date.valueOf(value.toString());
        } else {
            return null;
        }
    }

    private static java.sql.Timestamp getTimestampValue(Object value)
    {
        if (value instanceof java.sql.Timestamp) {
            return (java.sql.Timestamp)value;
        } else if (value instanceof Date) {
            return new java.sql.Timestamp(((Date) value).getTime());
        } else if (value != null) {
            return java.sql.Timestamp.valueOf(value.toString());
        } else {
            return null;
        }
    }

    protected static String getTwoDigitValue(int value)
    {
        if (value < 10) {
            return "0" + value;
        } else {
            return String.valueOf(value);
        }
    }

}