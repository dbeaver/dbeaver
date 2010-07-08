/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.dialogs.data.DateTimeViewDialog;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * JDBC string value handler
 */
public class JDBCDateTimeValueHandler extends JDBCAbstractValueHandler {

    public static final JDBCDateTimeValueHandler INSTANCE = new JDBCDateTimeValueHandler();
    public static final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.LONG);//new SimpleDateFormat("yyyy-MMM-dd");
    public static final DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.LONG);//new SimpleDateFormat("HH:mm:ss");
    public static final DateFormat timeStampFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);//new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    protected Object getColumnValue(ResultSet resultSet, DBSTypedObject columnType, int columnIndex)
        throws DBCException, SQLException
    {
        switch (columnType.getValueType()) {
        case java.sql.Types.TIME:
            return resultSet.getTime(columnIndex);
        case java.sql.Types.DATE:
            return resultSet.getDate(columnIndex);
        default:
            return resultSet.getTimestamp(columnIndex);
        }
    }

    protected void bindParameter(PreparedStatement statement, DBSTypedObject paramType, int paramIndex, Object value) throws SQLException
    {
        if (value == null) {
            statement.setNull(paramIndex + 1, paramType.getValueType());
        } else {
            switch (paramType.getValueType()) {
            case java.sql.Types.TIME:
                statement.setTime(paramIndex, (java.sql.Time)value);
                break;
            case java.sql.Types.DATE:
                statement.setDate(paramIndex, (java.sql.Date)value);
                break;
            default:
                statement.setTimestamp(paramIndex, (Timestamp)value);
                break;
            }
        }
    }

    public boolean editValue(final DBDValueController controller)
        throws DBException
    {
        if (controller.isInlineEdit()) {
            Object value = controller.getValue();

            Composite dateTimeGroup = controller.getInlinePlaceholder();

            boolean isDate = controller.getColumnMetaData().getValueType() == java.sql.Types.DATE;
            boolean isTime = controller.getColumnMetaData().getValueType() == java.sql.Types.TIME;
            boolean isTimeStamp = controller.getColumnMetaData().getValueType() == java.sql.Types.TIMESTAMP;

            final DateTime dateEditor = isDate || isTimeStamp ? new DateTime(dateTimeGroup, SWT.DATE | SWT.LONG) : null;
            final DateTime timeEditor = isTime || isTimeStamp ? new DateTime(dateTimeGroup, SWT.TIME | SWT.LONG) : null;

            if (dateEditor != null) {
                if (value instanceof Date) {
                    Calendar cl = Calendar.getInstance();
                    cl.setTime((Date)value);
                    dateEditor.setDate(cl.get(Calendar.YEAR), cl.get(Calendar.MONTH), cl.get(Calendar.DAY_OF_MONTH));
                }
                initInlineControl(controller, dateEditor, new ValueExtractor<DateTime>() {
                    public Object getValueFromControl(DateTime control)
                    {
                        return getDate(dateEditor, timeEditor);
                    }
                });
            }
            if (timeEditor != null) {
                if (value instanceof Date) {
                    Calendar cl = Calendar.getInstance();
                    cl.setTime((Date)value);
                    timeEditor.setTime(cl.get(Calendar.HOUR_OF_DAY), cl.get(Calendar.MINUTE), cl.get(Calendar.SECOND));
                }
                initInlineControl(controller, timeEditor, new ValueExtractor<DateTime>() {
                    public Object getValueFromControl(DateTime control)
                    {
                        return getDate(dateEditor, timeEditor);
                    }
                });
            }
            dateTimeGroup.setFocus();
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
        DateFormat formatter;
        switch (column.getValueType()) {
        case java.sql.Types.TIME:
            formatter = timeFormat;
            break;
        case java.sql.Types.DATE:
            formatter = dateFormat;
            break;
        default:
            formatter = timeStampFormat;
            break;
        }
        synchronized (formatter) {
            return formatter.format(value);
        }
    }

    public Object copyValueObject(Object value)
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
    public void fillContextMenu(IMenuManager menuManager, final DBDValueController controller)
        throws DBCException
    {
        menuManager.add(new Action("Set to current time") {
            @Override
            public void run() {
                controller.updateValue(new Date());
            }
        });
    }

    public static Date getDate(DateTime dateEditor, DateTime timeEditor)
    {
        Calendar cl = Calendar.getInstance();
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

}