package org.jkiss.dbeaver.model.impl.data;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.util.Calendar;
import java.util.Date;
import java.sql.Timestamp;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;

/**
 * JDBC string value handler
 */
public class JDBCDateTimeValueHandler extends JDBCAbstractValueHandler {

    public static final JDBCDateTimeValueHandler INSTANCE = new JDBCDateTimeValueHandler();

    protected Object getValueObject(ResultSet resultSet, DBSTypedObject columnType, int columnIndex)
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

/*
            if (controller.getColumnMetaData().getValueType() == java.sql.Types.TIMESTAMP) {
                dateTimeGroup = new Composite(controller.getInlinePlaceholder(), SWT.NONE);
                GridLayout layout = new GridLayout(2, false);
                layout.marginWidth = 0;
                layout.marginHeight = 0;
                dateTimeGroup.setLayout(layout);
                GridData gd = new GridData(GridData.FILL_BOTH);
                gd.horizontalIndent = 0;
                gd.verticalIndent = 0;
                gd.grabExcessHorizontalSpace = true;
                gd.grabExcessVerticalSpace = true;
                dateTimeGroup.setLayoutData(gd);
            }
*/
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
            return false;
        }
    }

    private static Date getDate(DateTime dateEditor, DateTime timeEditor)
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