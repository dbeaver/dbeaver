/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.firebird.model.sql.generator;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.generator.SQLGeneratorTable;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

public class SQLGeneratorExecuteBlock extends SQLGeneratorTable {

    @Override
    public void generateSQL(DBRProgressMonitor monitor, StringBuilder sql, DBSEntity object) throws DBException {
        sql.append("EXECUTE BLOCK").append(getLineSeparator());
        sql.append("AS").append(getLineSeparator());

        for (DBSEntityAttribute attr : getAllAttributes(monitor, object)) {
            if (DBUtils.isPseudoAttribute(attr) || DBUtils.isHiddenObject(attr)) {
                continue;
            }
            sql.append("  DECLARE VARIABLE ")
                .append(DBUtils.getObjectFullName(attr, DBPEvaluationContext.DML))
                .append(" ").append(attr.getTypeName());
            if (attr.getMaxLength() > 0 && isTypeLengthRequired(attr.getTypeName())) {
                sql.append("(").append(attr.getMaxLength()).append(")");
            }
            sql.append(";").append(getLineSeparator());
        }

        sql.append("BEGIN").append(getLineSeparator());
        sql.append("  /* TODO: add statements here */").append(getLineSeparator());
        sql.append("END").append(getLineSeparator());
    }

    private static boolean isTypeLengthRequired(String typeName) {
        if (typeName == null) {
            return false;
        }
        String upper = typeName.toUpperCase();
        return upper.startsWith("VARCHAR") || upper.startsWith("CHAR")
            || upper.startsWith("VARBINARY") || upper.startsWith("BINARY");
    }
}
