/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.data;

import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.data.DBDValueHandler2;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCDateTimeValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.util.Calendar;
import java.util.Date;

/**
 * MySQL datetime handler
 */
public class MySQLDateTimeValueHandler extends JDBCDateTimeValueHandler {

    public MySQLDateTimeValueHandler(DBDDataFormatterProfile formatterProfile)
    {
        super(formatterProfile);
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
                return "STR_TO_DATE('" + hourOfDay + ":" + minutes + ":" + seconds + "','%H:%i:%s')";
            case java.sql.Types.DATE:
                return "STR_TO_DATE('" + year + "-" + month + "-" + dayOfMonth + "','%Y-%m-%d')";
            default:
                return "STR_TO_DATE('" + year + "-" + month + "-" + dayOfMonth +
                    " " + hourOfDay + ":" + minutes + ":" + seconds +
                    "','%Y-%m-%d %H:%i:%s')";
            }
        } else {
            return getValueDisplayString(column, value);
        }
    }
}
