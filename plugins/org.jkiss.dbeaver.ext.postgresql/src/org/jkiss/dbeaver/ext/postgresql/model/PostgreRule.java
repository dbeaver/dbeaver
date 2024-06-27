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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.util.Map;

/**
 * PostgreRule
 */
public class PostgreRule implements PostgreObject, PostgreScriptObject, DBPQualifiedObject
{
    private static final Log log = Log.getLog(PostgreRule.class);

    private PostgreTableBase table;
    private long objectId;

    protected String name;
    private boolean persisted;
    private String type;
    private String enabled;
    private boolean instead;
    private String definition;
    public String body;

    protected String description;

    public PostgreRule(
        DBRProgressMonitor monitor,
        PostgreTableReal table,
        ResultSet dbResult) throws DBException
    {
        this.table = table;
        this.persisted = true;
        this.objectId = JDBCUtils.safeGetLong(dbResult, "oid");
        this.name = JDBCUtils.safeGetString(dbResult, "rulename");
        this.type = JDBCUtils.safeGetString(dbResult, "ev_type");
        this.enabled = JDBCUtils.safeGetString(dbResult, "ev_enabled");
        this.instead = JDBCUtils.safeGetBoolean(dbResult, "is_instead");

        this.definition = JDBCUtils.safeGetString(dbResult, "definition");
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Override
    public boolean isPersisted() {
        return persisted;
    }

    public PostgreTableBase getTable()
    {
        return table;
    }

    @Override
    @Property(viewable = true, order = 10)
    public long getObjectId() {
        return objectId;
    }

    @Property(viewable = true, order = 20)
    public String getType() {
        return type;
    }

    @Property(viewable = true, order = 21)
    public String getEnabled() {
        return enabled;
    }

    @Property(viewable = true, order = 22)
    public boolean isInstead() {
        return instead;
    }

    //@Property(viewable = true, editable = true, updatable = true, multiline = true, order = 100)
    @Nullable
    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public PostgreTableBase getParentObject()
    {
        return table;
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource()
    {
        return table.getDataSource();
    }

    @NotNull
    @Override
    public PostgreDatabase getDatabase() {
        return table.getDatabase();
    }

    @Override
    @Property(hidden = true, order = 80)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException
    {
        if (body == null) {
            StringBuilder ddl = new StringBuilder();
            ddl.append("-- DROP RULE ").append(DBUtils.getQuotedIdentifier(this)).append(" ON ")
                    .append(getTable().getFullyQualifiedName(DBPEvaluationContext.DDL)).append(";\n\n");

            ddl.append(definition);
            this.body = ddl.toString();
        }
        return body;
    }

    @Override
    public void setObjectDefinitionText(String sourceText) throws DBException
    {
        definition = sourceText;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getParentObject(),
            this);
    }

}
