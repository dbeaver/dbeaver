/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.clickhouse.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.impl.data.formatters.DefaultDataFormatter;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCDateTimeValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.Types;

/**
 * Clickhouse has two timestamp types with different formats in the same time
 * They contain different values range and should be formatted separately:
 * Datetime: 2019-01-01 03:00:00
 * Datetime64: 2019-01-01 00:00:00.000
 */
public class ClickhouseDateTimeValueHandler extends JDBCDateTimeValueHandler {
    private static final String DATETIME_64_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";
    private static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    public ClickhouseDateTimeValueHandler(DBDFormatSettings formatSettings) {
        super(formatSettings);
    }

    @Override
    protected DBDDataFormatter getFormatter(DBSTypedObject typedObject, String typeId) {
        try {
            DBDDataFormatter formatter = formatSettings.getDataFormatterProfile().createFormatter(typeId, typedObject);
            if ("datetime64".equalsIgnoreCase(typedObject.getTypeName())) {
                formatter.setPattern(DATETIME_64_PATTERN);
            } else if ("datetime32".equalsIgnoreCase(typedObject.getTypeName()) || "datetime".equalsIgnoreCase(typedObject.getTypeName())) {
                formatter.setPattern(DATETIME_PATTERN);
            }
            return formatter;
        } catch (ReflectiveOperationException e) {
            log.error("Can't create formatter for datetime value handler", e);
            return DefaultDataFormatter.INSTANCE;
        }
    }

    @NotNull
    @Override
    protected String getFormatterId(DBSTypedObject column) {
        switch (column.getTypeID()) {
            case Types.DATE:
                return DBDDataFormatter.TYPE_NAME_DATE;
            case Types.TIMESTAMP_WITH_TIMEZONE:
                return DBDDataFormatter.TYPE_NAME_TIMESTAMP_TZ;
            default:
                return DBDDataFormatter.TYPE_NAME_TIMESTAMP;
        }
    }
}
