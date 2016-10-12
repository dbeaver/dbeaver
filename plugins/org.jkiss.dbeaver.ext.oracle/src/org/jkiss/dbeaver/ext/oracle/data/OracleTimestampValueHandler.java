/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.oracle.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCDateTimeValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.Types;

/**
 * Object type support
 */
public class OracleTimestampValueHandler extends JDBCDateTimeValueHandler {

    public OracleTimestampValueHandler(DBPDataSource dataSource, DBDDataFormatterProfile formatterProfile)
    {
        super(dataSource, formatterProfile);
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format) {
        String dtString = super.getValueDisplayString(column, value, format);
        if (format == DBDDisplayFormat.NATIVE) {
            switch (column.getTypeID()) {
                case Types.TIMESTAMP:
                case Types.TIMESTAMP_WITH_TIMEZONE:
                case -102: // TIMESTAMP_WITH_LOCAL_TIMEZONE
                    return "TIMESTAMP " + dtString;
                case Types.TIME:
                case Types.TIME_WITH_TIMEZONE:
                    return "TIME " + dtString;
                case Types.DATE:
                    return "DATE " + dtString;
            }
        }
        return dtString;
    }
}
