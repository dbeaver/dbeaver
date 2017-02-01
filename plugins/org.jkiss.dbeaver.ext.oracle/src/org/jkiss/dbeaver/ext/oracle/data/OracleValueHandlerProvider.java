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

import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDPreferences;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.Types;

/**
 * Oracle data types provider
 */
public class OracleValueHandlerProvider implements DBDValueHandlerProvider {

    @Override
    public DBDValueHandler getValueHandler(DBPDataSource dataSource, DBDPreferences preferences, DBSTypedObject typedObject)
    {
        final String typeName = typedObject.getTypeName();
        if (typedObject.getTypeID() == Types.BLOB) {
            return OracleBLOBValueHandler.INSTANCE;
        } else if (typedObject.getTypeID() == Types.CLOB || typedObject.getTypeID() == Types.NCLOB) {
            return OracleCLOBValueHandler.INSTANCE;
        } else if (OracleConstants.TYPE_NAME_XML.equals(typeName) || OracleConstants.TYPE_FQ_XML.equals(typeName)) {
            return OracleXMLValueHandler.INSTANCE;
        } else if (OracleConstants.TYPE_NAME_BFILE.equals(typeName)) {
            return OracleBFILEValueHandler.INSTANCE;
        } else if (typedObject.getTypeID() == java.sql.Types.STRUCT) {
            return OracleObjectValueHandler.INSTANCE;
        } else if (typeName.contains("TIMESTAMP") || typedObject.getDataKind() == DBPDataKind.DATETIME) {
            return new OracleTimestampValueHandler(preferences.getDataFormatterProfile());
        } else {
            return null;
        }
    }

}