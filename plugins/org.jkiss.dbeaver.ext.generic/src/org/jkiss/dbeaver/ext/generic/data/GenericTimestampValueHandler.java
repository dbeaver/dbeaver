/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.generic.data;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCDateTimeValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.Types;
import java.text.Format;

/**
 * Object type support
 */
public class GenericTimestampValueHandler extends JDBCDateTimeValueHandler {

    private final GenericDataSource dataSource;

    public GenericTimestampValueHandler(GenericDataSource dataSource, DBDFormatSettings formatSettings)
    {
        super(formatSettings);

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
