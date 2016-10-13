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
package org.jkiss.dbeaver.ext.generic.data;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCDateTimeValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.Types;
import java.text.Format;

/**
 * Object type support
 */
public class GenericTimestampValueHandler extends JDBCDateTimeValueHandler {

    private final GenericDataSource dataSource;

    public GenericTimestampValueHandler(GenericDataSource dataSource, DBDDataFormatterProfile formatterProfile)
    {
        super(formatterProfile);

        this.dataSource = dataSource;
    }

    @Nullable
    @Override
    public Format getNativeValueFormat(DBSTypedObject type) {
        Format nativeFormat = null;
        switch (type.getTypeID()) {
            case Types.TIMESTAMP:
            case Types.TIMESTAMP_WITH_TIMEZONE:
                nativeFormat = dataSource.getNativeFormatTimestamp();
                break;
            case Types.TIME:
            case Types.TIME_WITH_TIMEZONE:
                nativeFormat = dataSource.getNativeFormatTime();
                break;
            case Types.DATE:
                nativeFormat = dataSource.getNativeFormatDate();
                break;
        }
        if (nativeFormat != null) {
            return nativeFormat;
        }
        return super.getNativeValueFormat(type);
    }

}
