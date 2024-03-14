/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.altibase.data;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCDateTimeValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.time.ExtendedDateFormat;

import java.sql.Types;
import java.text.Format;
import java.text.SimpleDateFormat;

public class AltibaseDateValueHandler extends JDBCDateTimeValueHandler {
    
    private final SimpleDateFormat defaultDateTimeFormat = new ExtendedDateFormat("''yyyy-MM-dd HH:mm:ss.ffffff''");

    public AltibaseDateValueHandler(DBDFormatSettings formatSettings) {
        super(formatSettings);
    }
    
    @Nullable
    @Override
    public Format getNativeValueFormat(DBSTypedObject type) {
        
        if (type.getTypeID() == Types.TIMESTAMP) {
            return defaultDateTimeFormat;
        }

        return super.getNativeValueFormat(type);
    }
}
