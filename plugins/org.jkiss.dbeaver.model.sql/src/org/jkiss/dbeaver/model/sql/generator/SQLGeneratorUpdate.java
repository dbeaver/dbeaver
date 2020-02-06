/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;

public class SQLGeneratorUpdate extends TableAnalysisRunner {

    @Override
    public void generateSQL(DBRProgressMonitor monitor, StringBuilder sql, DBSEntity object) throws DBException {
        Collection<? extends DBSEntityAttribute> keyAttributes = getKeyAttributes(monitor, object);
        sql.append("UPDATE ").append(getEntityName(object))
            .append(getLineSeparator()).append("SET ");
        boolean hasAttr = false;
        for (DBSAttributeBase attr : getValueAttributes(monitor, object, keyAttributes)) {
            if (DBUtils.isPseudoAttribute(attr) || DBUtils.isHiddenObject(attr)) {
                continue;
            }
            if (hasAttr) sql.append(", ");
            sql.append(DBUtils.getObjectFullName(attr, DBPEvaluationContext.DML)).append("=");
            appendDefaultValue(sql, attr);
            hasAttr = true;
        }
        if (!CommonUtils.isEmpty(keyAttributes)) {
            sql.append(getLineSeparator()).append("WHERE ");
            hasAttr = false;
            for (DBSEntityAttribute attr : keyAttributes) {
                if (hasAttr) sql.append(" AND ");
                sql.append(DBUtils.getObjectFullName(attr, DBPEvaluationContext.DML)).append("=");
                appendDefaultValue(sql, attr);
                hasAttr = true;
            }
        }
        sql.append(";\n");
    }
}
