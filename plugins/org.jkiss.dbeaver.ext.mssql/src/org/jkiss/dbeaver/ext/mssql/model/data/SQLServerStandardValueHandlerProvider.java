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
package org.jkiss.dbeaver.ext.mssql.model.data;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mssql.SQLServerConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCStandardValueHandlerProvider;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

public class SQLServerStandardValueHandlerProvider extends JDBCStandardValueHandlerProvider {

    @Nullable
    @Override
    public DBDValueHandler getValueHandler(DBPDataSource dataSource, DBDFormatSettings preferences, DBSTypedObject typedObject)
    {
        String typeName = typedObject.getTypeName();
        if (typeName.equals(SQLServerConstants.TYPE_MONEY) || typeName.equals(SQLServerConstants.TYPE_SMALLMONEY)) {
            return SQLServerMoneyValueHandler.INSTANCE;
        } else if (typeName.equals(SQLServerConstants.TYPE_SQL_VARIANT)) {
            return SQLServerSQLVariantValueHandler.INSTANCE;
        } else {
            return null;
        }
    }
}
