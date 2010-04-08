package org.jkiss.dbeaver.model.impl.data;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.dbc.DBCStatement;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.util.Calendar;
import java.util.Date;
import java.sql.Timestamp;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * JDBC string value handler
 */
public class JDBCDateTimeValueHandler extends JDBCAbstractValueHandler {

    public static final JDBCDateTimeValueHandler INSTANCE = new JDBCDateTimeValueHandler();
    private static final int MAX_STRING_LENGTH = 0xffff;

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

    @Override
    public void bindParameter(DBCStatement statement, DBSTypedObject columnMetaData, int paramIndex, Object value) throws DBCException
    {
        PreparedStatement dbStat = getPreparedStatement(statement);
        try {
            if (value == null) {
                dbStat.setNull(paramIndex + 1, columnMetaData.getValueType());
            } else {
                switch (columnMetaData.getValueType()) {
                case java.sql.Types.TIME:
                    dbStat.setTime(paramIndex + 1, (java.sql.Time)value);
                    break;
                case java.sql.Types.DATE:
                    dbStat.setDate(paramIndex + 1, (java.sql.Date)value);
                    break;
                default:
                    dbStat.setTimestamp(paramIndex + 1, (Timestamp)value);
                    break;
                }
            }
        } catch (SQLException e) {
            throw new DBCException("Could not bind date-time parameter", e);
        }
    }

}