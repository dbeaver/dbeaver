/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.data.DefaultDataFormatter;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.dialogs.data.DateTimeViewDialog;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

/**
 * JDBC string value handler
 */
public class JDBCDateTimeValueHandler extends JDBCAbstractValueHandler {

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

    protected void bindParameter(JDBCExecutionContext context, JDBCPreparedStatement statement, DBSTypedObject paramType,
                                 int paramIndex, Object value) throws SQLException
    {
        if (value == null) {
            statement.setNull(paramIndex, paramType.getTypeID());
        } else {
            switch (paramType.getTypeID()) {
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

            final Composite dateTimeGroup = controller.getInlinePlaceholder();

            boolean isDate = controller.getColumnMetaData().getTypeID() == java.sql.Types.DATE;
            boolean isTime = controller.getColumnMetaData().getTypeID() == java.sql.Types.TIME;
            boolean isTimeStamp = controller.getColumnMetaData().getTypeID() == java.sql.Types.TIMESTAMP;

            final DateTime dateEditor = isDate || isTimeStamp ? new DateTime(dateTimeGroup, SWT.BORDER | SWT.DATE | SWT.MEDIUM | SWT.DROP_DOWN) : null;
            final DateTime timeEditor = isTime || isTimeStamp ? new DateTime(dateTimeGroup, SWT.BORDER | SWT.TIME | SWT.LONG) : null;

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

/*
            // There is a bug in windows. First time date control gain focus it renders cell editor incorrectly.
            // Let's focus on it in async mode
*/
            dateTimeGroup.getDisplay().asyncExec(new Runnable() {
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

    public int getFeatures()
    {
        return FEATURE_VIEWER | FEATURE_EDITOR | FEATURE_INLINE_EDITOR;
    }

    public Class getValueObjectType()
    {
        return Date.class;
    }

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

}