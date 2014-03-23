/*
 * Copyright (C) 2010-2014 Serge Rieder
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
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.data.DefaultDataFormatter;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.data.DateTimeViewDialog;
import org.jkiss.dbeaver.ui.properties.PropertySourceAbstract;

import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.ParseException;
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
    //private Calendar calendar;

    public JDBCDateTimeValueHandler(DBDDataFormatterProfile formatterProfile)
    {
        this.formatterProfile = formatterProfile;
        //this.calendar = Calendar.getInstance(formatterProfile.getLocale());
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
    protected Object fetchColumnValue(DBCSession session, JDBCResultSet resultSet, DBSTypedObject type,
                                      int index)
        throws DBCException, SQLException
    {
        // It seems that some drivers doesn't support reading date/time values with explicit calendar
        // So let's use simple version
        switch (type.getTypeID()) {
            case java.sql.Types.TIME:
                return resultSet.getTime(index); //, this.calendar
            case java.sql.Types.DATE:
                return resultSet.getDate(index); //, this.calendar
            default:
                return resultSet.getTimestamp(index); //, this.calendar
        }
    }

    @Override
    protected void bindParameter(JDBCSession session, JDBCPreparedStatement statement, DBSTypedObject paramType,
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
    public DBDValueEditor createEditor(@NotNull DBDValueController controller)
        throws DBException
    {
        switch (controller.getEditType()) {
            case INLINE:
            case PANEL:
                return new ValueEditor<DateTime>(controller) {
                    DateTime dateEditor;
                    DateTime timeEditor;
                    @Override
                    public void primeEditorValue(Object value) throws DBException
                    {
                        if (value instanceof Date) {
                            Calendar cl = Calendar.getInstance();
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

                        boolean isDate = valueController.getValueType().getTypeID() == java.sql.Types.DATE;
                        boolean isTime = valueController.getValueType().getTypeID() == java.sql.Types.TIME;
                        boolean isTimeStamp = valueController.getValueType().getTypeID() == java.sql.Types.TIMESTAMP || (!isDate && !isTime);

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
                        return getDate(dateEditor, timeEditor);
                    }
                };
            case EDITOR:
                return new DateTimeViewDialog(controller);
            default:
                return null;
        }
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format)
    {
        if (value == null) {
            return super.getValueDisplayString(column, value, format);
        }
        if (value instanceof Date && format == DBDDisplayFormat.NATIVE) {
            Calendar cal = Calendar.getInstance();
            cal.setTime((Date) value);
            final String hourOfDay = getTwoDigitValue(cal.get(Calendar.HOUR_OF_DAY));
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
            return getFormatter(column).formatValue(value);
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
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy) throws DBCException
    {
        if (object == null) {
            return null;
        } else if (object instanceof Date) {
            return copy ? ((Date)object).clone() : object;
        } else if (object instanceof String) {
            String strValue = (String)object;
            try {
                return getFormatter(type).parseValue(strValue, null);
            } catch (ParseException e) {
                // Try to parse with standard date/time formats

                //DateFormat.get
                try {
                    // Try to parse as java date
                    @SuppressWarnings("deprecation")
                    Date result = new Date(strValue);
                    return result;
                } catch (Exception e1) {
                    log.debug("Can't parse string value [" + strValue + "] to date/time value", e);
                    return null;
                }
            }
        } else {
            log.warn("Unrecognized type '" + object.getClass().getName() + "' - can't convert to date/time value");
            return null;
        }
    }

    @Override
    public void contributeActions(@NotNull IContributionManager manager, @NotNull final DBDValueController controller)
        throws DBCException
    {
        manager.add(new Action(CoreMessages.model_jdbc_set_to_current_time) {
            @Override
            public void run()
            {
                controller.updateValue(new Date());
            }
        });
    }

    @Override
    public void contributeProperties(@NotNull PropertySourceAbstract propertySource, @NotNull DBDValueController controller)
    {
        super.contributeProperties(propertySource, controller);
        propertySource.addProperty(
            "Date/Time",
            "format", //$NON-NLS-1$
            "Pattern",
            getFormatter(controller.getValueType()).getPattern());
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

    private static java.sql.Time getTimeValue(Object value)
    {
        if (value instanceof java.sql.Time) {
            return (java.sql.Time) value;
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
            return (java.sql.Date) value;
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
            return (java.sql.Timestamp) value;
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

    protected DBDDataFormatter getFormatter(DBSTypedObject column)
    {
        switch (column.getTypeID()) {
            case java.sql.Types.TIME:
                return getFormatter(TYPE_NAME_TIME);
            case java.sql.Types.DATE:
                return getFormatter(TYPE_NAME_DATE);
            default:
                return getFormatter(TYPE_NAME_TIMESTAMP);
        }
    }

}