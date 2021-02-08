/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.Types;

/**
 * Oracle data types provider
 */
public class OracleValueHandlerProvider implements DBDValueHandlerProvider {

    @Override
    public DBDValueHandler getValueHandler(DBPDataSource dataSource, DBDFormatSettings preferences, DBSTypedObject typedObject)
    {
        switch (typedObject.getTypeID()) {
            case Types.BLOB:
                return OracleBLOBValueHandler.INSTANCE;
            case Types.CLOB:
            case Types.NCLOB:
                return OracleCLOBValueHandler.INSTANCE;
            case Types.TIME_WITH_TIMEZONE:
            case Types.TIMESTAMP_WITH_TIMEZONE:
            case OracleConstants.DATA_TYPE_TIMESTAMP_WITH_TIMEZONE:
                if (((OracleDataSource)dataSource).isDriverVersionAtLeast(12, 2)) {
                    return new OracleTemporalAccessorValueHandler(preferences);
                } else {
                    return new OracleTimestampValueHandler(preferences);
                }
            case Types.STRUCT:
                return OracleObjectValueHandler.INSTANCE;
            case OracleConstants.DATA_TYPE_REFCURSOR:
                return OracleRefCursorValueHandler.INSTANCE;
        }

        final String typeName = typedObject.getTypeName();
        switch (typeName) {
            case OracleConstants.TYPE_NAME_XML:
            case OracleConstants.TYPE_FQ_XML:
                return OracleXMLValueHandler.INSTANCE;
            case OracleConstants.TYPE_NAME_BFILE:
                return OracleBFILEValueHandler.INSTANCE;
            case OracleConstants.TYPE_NAME_REFCURSOR:
                return OracleRefCursorValueHandler.INSTANCE;
        }

        if (typeName.contains(OracleConstants.TYPE_NAME_TIMESTAMP) || typedObject.getDataKind() == DBPDataKind.DATETIME) {
            return new OracleTimestampValueHandler(preferences);
        } else {
            return null;
        }
    }

}