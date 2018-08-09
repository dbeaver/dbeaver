/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.oracle.data;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCDateTimeValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.time.ExtendedDateFormat;

import java.sql.Types;
import java.text.Format;
import java.text.SimpleDateFormat;

/**
 * Object type support
 */
public class OracleTimestampValueHandler extends JDBCDateTimeValueHandler {

    private static final SimpleDateFormat DEFAULT_DATETIME_FORMAT = new ExtendedDateFormat("'TIMESTAMP '''yyyy-MM-dd HH:mm:ss.ffffff''");
    private static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("'DATE '''yyyy-MM-dd''");
    private static final SimpleDateFormat DEFAULT_TIME_FORMAT = new SimpleDateFormat("'TIME '''HH:mm:ss.SSS''");

    public OracleTimestampValueHandler(DBDDataFormatterProfile formatterProfile) {
        super(formatterProfile);
    }


    @Nullable
    @Override
    public Format getNativeValueFormat(DBSTypedObject type) {
        switch (type.getTypeID()) {
            case Types.TIMESTAMP:
                return DEFAULT_DATETIME_FORMAT;
            case Types.TIMESTAMP_WITH_TIMEZONE:
            case OracleConstants.DATA_TYPE_TIMESTAMP_WITH_TIMEZONE:
            case OracleConstants.DATA_TYPE_TIMESTAMP_WITH_LOCAL_TIMEZONE:
                return DEFAULT_DATETIME_FORMAT;
            case Types.TIME:
                return DEFAULT_TIME_FORMAT;
            case Types.TIME_WITH_TIMEZONE:
                return DEFAULT_TIME_FORMAT;
            case Types.DATE:
                return DEFAULT_DATE_FORMAT;
        }
        // Have to revert DATE format. I can't realize what is difference between TIMESTAMP and DATE without time part.
        // Column types and lengths are the same. Data type name is the same. Oh, Oracle...
/*
        if (type.getMaxLength() == OracleConstants.DATE_TYPE_LENGTH) {
            return DEFAULT_DATE_FORMAT;
        }
*/
        return super.getNativeValueFormat(type);
    }

    protected String getFormatterId(DBSTypedObject column)
    {
/*
        if (column.getMaxLength() == OracleConstants.DATE_TYPE_LENGTH) {
            return DBDDataFormatter.TYPE_NAME_DATE;
        }
*/
        return super.getFormatterId(column);
    }

}
