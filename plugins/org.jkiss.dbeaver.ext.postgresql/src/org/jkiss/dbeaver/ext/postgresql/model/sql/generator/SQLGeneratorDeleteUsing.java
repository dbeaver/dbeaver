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
package org.jkiss.dbeaver.ext.postgresql.model.sql.generator;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.generator.SQLGeneratorTable;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;

public class SQLGeneratorDeleteUsing extends SQLGeneratorTable {

    @Override
    public void generateSQL(DBRProgressMonitor monitor, StringBuilder sql, DBSEntity object) throws DBException {
        String entityName = getEntityName(object);
        sql.append("DELETE FROM ").append(entityName).append(" AS tgt");
        sql.append(getLineSeparator()).append("USING SOURCE_TABLE AS src");
        sql.append(getLineSeparator()).append("WHERE ");
        Collection<? extends DBSEntityAttribute> keyAttributes = getKeyAttributes(monitor, object);
        if (CommonUtils.isEmpty(keyAttributes)) {
            keyAttributes = getAllAttributes(monitor, object);
        }
        boolean hasAttr = false;
        for (DBSEntityAttribute attr : keyAttributes) {
            if (hasAttr) {
                sql.append(" AND ");
            }
            sql.append("tgt." + DBUtils.getObjectFullName(attr, DBPEvaluationContext.DML)).append("=");
            sql.append("src." + DBUtils.getObjectFullName(attr, DBPEvaluationContext.DML));
            hasAttr = true;
        }
        sql.append(";").append(getLineSeparator());
    }
}
