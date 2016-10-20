/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.exasol.model.module;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.model.ExasolSchema;
import org.jkiss.dbeaver.ext.exasol.model.ExasolSchemaObject;
import org.jkiss.dbeaver.ext.exasol.model.ExasolScript;
import org.jkiss.dbeaver.ext.exasol.model.dict.ExasolScriptLanguage;
import org.jkiss.dbeaver.ext.exasol.model.dict.ExasolScriptResultType;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectSimpleCache;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;

public class ExasolModule extends ExasolSchemaObject implements DBSProcedureContainer, DBPRefreshableObject {

    private static final String C_SCRIPT = "select "
        + "script_name,script_owner,script_language,script_type,script_result_type,script_text,script_comment,b.created "
        + "from EXA_ALL_SCRIPTS a inner join EXA_ALL_OBJECTS b "
        + "on a.script_name = b.object_name and a.script_schema = b.root_name where a.script_schema = ? order by script_name";

    private final DBSObjectCache<ExasolModule, ExasolScript> scriptCache;


    private String remarks;
    private Timestamp createTime;
    private String owner;
    private ExasolScriptLanguage scriptLanguage;
    private String scriptSQL;
    private ExasolScriptResultType scriptReturnType;


    // -----------------------
    // Constructors
    // -----------------------
    public ExasolModule(ExasolSchema schema, ResultSet dbResult) {
        super(schema, JDBCUtils.safeGetStringTrimmed(dbResult, "SCRIPT_NAME"), true);
        this.owner = JDBCUtils.safeGetString(dbResult, "SCRIPT_OWNER");
        this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATED");
        this.remarks = JDBCUtils.safeGetString(dbResult, "SCRIPT_COMMENT");
        this.scriptLanguage = CommonUtils.valueOf(ExasolScriptLanguage.class, JDBCUtils.safeGetString(dbResult, "SCRIPT_LANGUAGE"));
        this.scriptReturnType = CommonUtils.valueOf(ExasolScriptResultType.class, JDBCUtils.safeGetString(dbResult, "SCRIPT_RESULT_TYPE"));
        this.scriptCache = new JDBCObjectSimpleCache<>(ExasolScript.class, C_SCRIPT, schema.getName(), name);
        this.scriptSQL = JDBCUtils.safeGetString(dbResult, "SCRIPT_TEXT");
        this.name = JDBCUtils.safeGetString(dbResult, "SCRIPT_NAME");


    }


    // -----------------------
    // Properties
    // -----------------------
    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return super.getName();
    }

    @Property(viewable = true, order = 2)
    public ExasolSchema getSchema() {
        return parent;
    }

    @Property(viewable = true, order = 3)
    public ExasolScriptLanguage getLanguage() {
        return this.scriptLanguage;
    }

    @Property(viewable = true, order = 4)
    public ExasolScriptResultType getResultType() {
        return this.scriptReturnType;
    }

    @Property(viewable = true, order = 5)
    public String getOwner() {
        return this.owner;
    }

    @Property(viewable = true, order = 6)
    public Timestamp getCreateTime() {
        return this.createTime;
    }


    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        this.scriptCache.clearCache();
        return this;
    }

    @Override
    public Collection<ExasolScript> getProcedures(DBRProgressMonitor monitor) throws DBException {
        // TODO Auto-generated method stub
        return scriptCache.getAllObjects(monitor, this);
    }

    @Override
    public DBSProcedure getProcedure(DBRProgressMonitor monitor, String uniqueName) throws DBException {
        // TODO Auto-generated method stub
        return null;
    }

}
