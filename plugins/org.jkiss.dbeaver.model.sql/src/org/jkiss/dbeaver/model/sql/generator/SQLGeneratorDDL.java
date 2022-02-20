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
package org.jkiss.dbeaver.model.sql.generator;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBPScriptObjectExt;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBStructUtils;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SQLGeneratorDDL extends SQLGenerator<DBPScriptObject> {

    @Override
    public boolean isDDLOption() {
        return true;
    }

    @Override
    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        boolean allTables = true;
        List<DBSTable> tableList = new ArrayList<>();
        for (DBPScriptObject object : objects) {
            if (!(object instanceof DBSTable)) {
                allTables = false;
                break;
            } else {
                tableList.add((DBSTable) object);
            }
        }
        if (!allTables) {
            super.run(monitor);
            return;
        }

        StringBuilder sql = new StringBuilder(100);
        Map<String, Object> options = new HashMap<>();
        addOptions(options);
        try {
            DBStructUtils.generateTableListDDL(monitor, sql, tableList, options, false);
        } catch (DBException e) {
            throw new InvocationTargetException(e);
        }
        result = sql.toString().trim();
    }

    @Override
    public void generateSQL(DBRProgressMonitor monitor, StringBuilder sql, DBPScriptObject object) throws DBException {
        if (sql.length() > 0) {
            sql.append("\n");
        }
        Map<String, Object> options = new HashMap<>();
        options.put(DBPScriptObject.OPTION_REFRESH, true);
        addOptions(options);

        String definitionText = CommonUtils.notEmpty(object.getObjectDefinitionText(monitor, options)).trim();
        sql.append(definitionText);
        String delimiter = SQLConstants.DEFAULT_STATEMENT_DELIMITER;
        if (object instanceof DBSObject) {
            SQLDialect sqlDialect = ((DBSObject) object).getDataSource().getSQLDialect();
            delimiter =  SQLUtils.getDefaultScriptDelimiter(sqlDialect);
        }
        if (!definitionText.endsWith(delimiter)) {
            sql.append(SQLConstants.DEFAULT_STATEMENT_DELIMITER);
        }
        sql.append("\n");
        if (object instanceof DBPScriptObjectExt) {
            String extendedDefinitionText = ((DBPScriptObjectExt) object).getExtendedDefinitionText(monitor);
            if (CommonUtils.isNotEmpty(extendedDefinitionText)) {
                String definition2 = extendedDefinitionText.trim();
                sql.append("\n");
                sql.append(definition2);
                if (!definition2.endsWith(SQLConstants.DEFAULT_STATEMENT_DELIMITER)) {
                    sql.append(SQLConstants.DEFAULT_STATEMENT_DELIMITER);
                }
                sql.append("\n");
            }
        }
    }

    @Override
    protected void addOptions(Map<String, Object> options) {
        super.addOptions(options);
        options.put(DBPScriptObject.OPTION_REFRESH, true);
        options.put(DBPScriptObject.OPTION_INCLUDE_OBJECT_DROP, true);
    }
}
