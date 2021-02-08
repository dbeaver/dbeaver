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
package org.jkiss.dbeaver.model.sql.generator;

import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSEntity;

import java.lang.reflect.InvocationTargetException;

public class SQLGeneratorJoin extends SQLGenerator<DBSEntity> {

    @Override
    public void run(DBRProgressMonitor monitor) throws InvocationTargetException {
        StringBuilder sql = new StringBuilder(100);
        try {
            sql.append("SELECT ");
            for (int i = 0; i < objects.size(); i++) {
                if (i > 0) sql.append(", ");
                sql.append(SQLUtils.getTableAlias(objects.get(i))).append(".*");
            }
            sql.append(getLineSeparator()).append("FROM ");
            for (int i = 0; i < objects.size(); i++) {
                DBSEntity entity = objects.get(i);
                if (i > 0) sql.append(", ");
                sql.append(getEntityName(entity)).append(" ").append(SQLUtils.getTableAlias(entity));
            }
            sql.append(getLineSeparator()).append("WHERE ");
            boolean hasCond = false;
            for (int i = 1; i < objects.size(); i++) {
                boolean foundJoin = false;
                for (int k = 0; k < i; k++) {
                    String tableJoin = SQLUtils.generateTableJoin(
                        monitor, objects.get(k), SQLUtils.getTableAlias(objects.get(k)), objects.get(i), SQLUtils.getTableAlias(objects.get(i)));
                    if (tableJoin != null) {
                        sql.append(getLineSeparator()).append("\t");
                        if (hasCond) sql.append("AND ");
                        sql.append(tableJoin);
                        hasCond = true;
                        foundJoin = true;
                        break;
                    }
                }
                if (!foundJoin) {
                    sql.append("\n-- Can't determine condition to join table ").append(DBUtils.getQuotedIdentifier(objects.get(i)));
                }
            }
        } catch (Exception e) {
            throw new InvocationTargetException(e);
        }
        result = sql.toString();
    }

    @Override
    public void generateSQL(DBRProgressMonitor monitor, StringBuilder sql, DBSEntity object) {
        // Do nothing for each individual table
    }
}
