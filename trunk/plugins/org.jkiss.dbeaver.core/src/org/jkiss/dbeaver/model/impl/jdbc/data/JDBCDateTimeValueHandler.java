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
package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.data.editors.DateTimeEditorHelper;
import org.jkiss.dbeaver.model.impl.data.editors.DateTimeInlineEditor;
import org.jkiss.dbeaver.model.impl.data.editors.DateTimeStandaloneEditor;
import org.jkiss.dbeaver.model.impl.data.formatters.DefaultDataFormatter;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.DBIcon;
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
public class JDBCDateTimeValueHandler extends JDBCAbstractValueHandler implements DateTimeEditorHelper {

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
                return new DateTimeInlineEditor(controller, this);
            case EDITOR:
                return new DateTimeStandaloneEditor(controller, this);
            default:
                return null;
        }
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format)
    {
        if (value == null) {
            return super.getValueDisplayString(column, null, format);
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
            try {
                return getFormatter(column).formatValue(value);
            } catch (Exception e) {
                return String.valueOf(value);
            }
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
        manager.add(new Action(CoreMessages.model_jdbc_set_to_current_time, DBIcon.TYPE_DATETIME.getImageDescriptor()) {
            @Override
            public void run() {
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

    @Nullable
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

    @Nullable
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

    @Nullable
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

    @NotNull
    protected DBDDataFormatter getFormatter(DBSTypedObject column)
    {
        switch (column.getTypeID()) {
            case java.sql.Types.TIME:
                return getFormatter(DBDDataFormatter.TYPE_NAME_TIME);
            case java.sql.Types.DATE:
                return getFormatter(DBDDataFormatter.TYPE_NAME_DATE);
            default:
                return getFormatter(DBDDataFormatter.TYPE_NAME_TIMESTAMP);
        }
    }

    @Override
    public boolean isTimestamp(DBDValueController valueController) {
        return valueController.getValueType().getTypeID() == java.sql.Types.TIMESTAMP;
    }

    @Override
    public boolean isTime(DBDValueController valueController) {
        return valueController.getValueType().getTypeID() == java.sql.Types.TIME;
    }

    @Override
    public boolean isDate(DBDValueController valueController) {
        return valueController.getValueType().getTypeID() == java.sql.Types.DATE;
    }

    @Override
    public Object getValueFromMillis(DBDValueController valueController, long ms) {
        if (isTimestamp(valueController)) {
            return new Timestamp(ms);
        } else if (isTime(valueController)) {
            return new java.sql.Time(ms);
        } else {
            return new java.sql.Date(ms);
        }
    }
}