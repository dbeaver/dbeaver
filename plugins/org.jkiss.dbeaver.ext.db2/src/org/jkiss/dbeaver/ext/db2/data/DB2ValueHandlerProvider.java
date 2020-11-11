/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.data;

import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.ArrayUtils;

import java.sql.Types;

/**
 * DB2 Data Types provider
 * 
 * @author Denis Forveille
 */
public class DB2ValueHandlerProvider implements DBDValueHandlerProvider {

    private static final int[] NUMERIC_TYPES = {
        Types.DECIMAL,
        Types.NUMERIC,
        Types.REAL,
        Types.FLOAT,
        Types.DOUBLE,
    };

    @Override
    public DBDValueHandler getValueHandler(DBPDataSource dataSource, DBDFormatSettings preferences, DBSTypedObject typedObject)
    {
        final String typeName = typedObject.getTypeName();
        if (DB2Constants.TYPE_NAME_DECFLOAT.equals(typeName)) {
            return new DB2DecFloatValueHandler(typedObject, preferences);
        } else if (ArrayUtils.contains(NUMERIC_TYPES, typedObject.getTypeID())) {
            //return new DB2NumericValueHandler(typedObject, preferences.getDataFormatterProfile());
        } else if (typeName.contains("TIMESTAMP") || typedObject.getDataKind() == DBPDataKind.DATETIME) {
            return new DB2TimestampValueHandler(preferences);
        }
        return null;
    }
}