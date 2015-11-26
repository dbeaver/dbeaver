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
package org.jkiss.dbeaver.ext.mysql.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCDateTimeValueHandler;
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

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format)
    {
            if (value instanceof Date && format == DBDDisplayFormat.NATIVE) {
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
            return super.getValueDisplayString(column, value, format);
        }
    }
}
