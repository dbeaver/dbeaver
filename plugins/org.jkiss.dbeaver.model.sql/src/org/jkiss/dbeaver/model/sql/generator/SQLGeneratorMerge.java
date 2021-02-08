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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;

public class SQLGeneratorMerge extends SQLGeneratorTable {

    @Override
    public void generateSQL(DBRProgressMonitor monitor, StringBuilder sql, DBSEntity object) throws DBException {
        boolean hasAttr = false;

        sql.append("MERGE INTO ").append(getEntityName(object)).append(" AS tgt").append(getLineSeparator());
        sql.append("USING SOURCE_TABLE AS src").append(getLineSeparator());
        Collection<? extends DBSEntityAttribute> keyAttributes = getKeyAttributes(monitor, object);
        if (!CommonUtils.isEmpty(keyAttributes)) {
            sql.append("ON (");
            for (DBSEntityAttribute attr : keyAttributes) {
                if (hasAttr) sql.append(" AND ");
                sql.append("tgt.").append(DBUtils.getQuotedIdentifier(attr))
                    .append("=src.").append(DBUtils.getQuotedIdentifier(attr));
                hasAttr = true;
            }
            sql.append(")\n");
        }
        sql.append("WHEN MATCHED\nTHEN UPDATE SET").append(getLineSeparator());
        hasAttr = false;
        for (DBSAttributeBase attr : getValueAttributes(monitor, object, keyAttributes)) {
            if (hasAttr) sql.append(", ");
            sql.append("tgt.").append(DBUtils.getQuotedIdentifier(object.getDataSource(), attr.getName()))
                .append("=src.").append(DBUtils.getQuotedIdentifier(object.getDataSource(), attr.getName()));
            hasAttr = true;
        }
        sql.append(getLineSeparator()).append("WHEN NOT MATCHED").append(getLineSeparator()).append("THEN INSERT (");
        hasAttr = false;
        for (DBSEntityAttribute attr : getAllAttributes(monitor, object)) {
            if (hasAttr) sql.append(", ");
            sql.append(DBUtils.getQuotedIdentifier(attr));
            hasAttr = true;
        }
        sql.append(")").append(getLineSeparator()).append("VALUES (");
        hasAttr = false;
        for (DBSEntityAttribute attr : getAllAttributes(monitor, object)) {
            if (hasAttr) sql.append(", ");
            sql.append("src.").append(DBUtils.getQuotedIdentifier(attr));
            hasAttr = true;
        }
        sql.append(");\n");
    }
}
