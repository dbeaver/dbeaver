/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCDateTimeValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.time.ExtendedDateFormat;

import java.text.SimpleDateFormat;

/**
 * DB2 Date/Time/Timestamp handler
 * 
 * @author Denis Forveille
 */
public class DB2DateTimeValueHandler extends JDBCDateTimeValueHandler {

    private static final String FMT_DATE = "''yyyy-MM-dd''";
    private static final String FMT_TIME = "''HH.mm.ss''";
    private static final String FMT_TIMESTAMP = "yyyy-MM-dd-HH.mm.ss.ffffff";

    public DB2DateTimeValueHandler(DBDDataFormatterProfile formatterProfile)
    {
        super(formatterProfile);
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format)
    {

        if (format == DBDDisplayFormat.NATIVE) {
            SimpleDateFormat sdf;

            switch (column.getTypeID()) {
            case java.sql.Types.DATE:
                sdf = new SimpleDateFormat(FMT_DATE);
                return sdf.format(value);
            case java.sql.Types.TIME:
                sdf = new SimpleDateFormat(FMT_TIME);
                return sdf.format(value);
            case java.sql.Types.TIMESTAMP:
                sdf = new ExtendedDateFormat(FMT_TIMESTAMP);
                return "'" + sdf.format(value) + "'";
            }
        }

        return super.getValueDisplayString(column, value, format);
    }
}
