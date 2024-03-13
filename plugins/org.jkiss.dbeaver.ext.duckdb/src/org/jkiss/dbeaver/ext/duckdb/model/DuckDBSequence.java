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
package org.jkiss.dbeaver.ext.duckdb.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.generic.model.GenericScriptObject;
import org.jkiss.dbeaver.ext.generic.model.GenericSequence;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPObjectWithLongId;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

public class DuckDBSequence extends GenericSequence implements DBPObjectWithLongId, GenericScriptObject {

    private long oid;
    private long startValue;
    private boolean cycle = false;
    private boolean temporary;

    private String sql;

    public DuckDBSequence(
        GenericStructContainer container,
        String name,
        String description,
        Number lastValue,
        Number minValue,
        Number maxValue,
        Number incrementBy,
        @NotNull JDBCResultSet dbResult
    ) {
        super(container, name, description, lastValue, minValue, maxValue, incrementBy);
        this.oid = JDBCUtils.safeGetLong(dbResult, "sequence_oid");
        this.startValue = JDBCUtils.safeGetLong(dbResult, "start_value");
        this.cycle = JDBCUtils.safeGetBoolean(dbResult, "cycle");
        this.temporary = JDBCUtils.safeGetBoolean(dbResult, "temporary");
        this.sql = JDBCUtils.safeGetString(dbResult, "sql");
    }

    public DuckDBSequence(@NotNull GenericStructContainer container, @NotNull String name) {
        super(container, name);
    }

    @Override
    @Property(viewable = true, order = 0)
    public long getObjectId() {
        return oid;
    }

    @Property(viewable = true, order = 6)
    public long getStartValue() {
        return startValue;
    }

    @Property(viewable = true, order = 7)
    public boolean isCycle() {
        return cycle;
    }

    @Property(viewable = true, order = 8)
    public boolean isTemporary() {
        return temporary;
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) {
        if (CommonUtils.isEmpty(sql)) {
            sql = "CREATE SEQUENCE " + getFullyQualifiedName(DBPEvaluationContext.DDL);
        }
        return sql;
    }
}
