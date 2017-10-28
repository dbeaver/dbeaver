/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.exasol.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.ExasolConstants;
import org.jkiss.dbeaver.ext.exasol.editors.ExasolSourceObject;
import org.jkiss.dbeaver.ext.exasol.model.dict.ExasolScriptLanguage;
import org.jkiss.dbeaver.ext.exasol.model.dict.ExasolScriptResultType;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractProcedure;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameter;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Map;

public class ExasolScript extends AbstractProcedure<ExasolDataSource, ExasolSchema> implements DBSProcedure, DBPRefreshableObject, ExasolSourceObject {


    private String remarks;
    private Timestamp createTime;
    private String owner;
    private ExasolScriptLanguage scriptLanguage;
    private String scriptSQL;
    private ExasolScriptResultType scriptReturnType;
    private String scriptType;
    private ExasolSchema exasolSchema;

    public ExasolScript(ExasolSchema schema, ResultSet dbResult) {
    	super(schema, true);
        this.owner = JDBCUtils.safeGetString(dbResult, "SCRIPT_OWNER");
        this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATED");
        this.remarks = JDBCUtils.safeGetString(dbResult, "SCRIPT_COMMENT");
        this.scriptLanguage = CommonUtils.valueOf(ExasolScriptLanguage.class, JDBCUtils.safeGetString(dbResult, "SCRIPT_LANGUAGE"));
        this.scriptReturnType = CommonUtils.valueOf(ExasolScriptResultType.class, JDBCUtils.safeGetString(dbResult, "SCRIPT_RESULT_TYPE"));
        this.scriptSQL = JDBCUtils.safeGetString(dbResult, "SCRIPT_TEXT");
        this.name = JDBCUtils.safeGetString(dbResult, "SCRIPT_NAME");
        this.scriptType = JDBCUtils.safeGetString(dbResult, "SCRIPT_TYPE");
        exasolSchema = schema;

    }
    
    public ExasolScript(ExasolSchema schema)
    {
        super(schema, false);
        exasolSchema = schema;
        scriptSQL = "";
    }


    @Override
    public ExasolScript refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        getContainer().scriptCache.clearCache();
        getContainer().scriptCache.getAllObjects(monitor, getSchema());
        return getContainer().scriptCache.getObject(monitor, exasolSchema, getName());
    }


    // -----------------------
    // Properties
    // -----------------------


    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return this.name;
    }

    @Property(viewable = true, order = 2)
    public ExasolSchema getSchema() {
        return exasolSchema;
    }

    @Property(viewable = true, order = 5)
    public ExasolScriptLanguage getLanguage() {
        return scriptLanguage;
    }

    @Property(order = 10)
    public ExasolScriptResultType getResultType() {
        return scriptReturnType;
    }

    @Nullable
    @Override
    @Property(viewable = true, editable = true, updatable = true, order = 11)
    public String getDescription() {
        return this.remarks;
    }


    @Nullable
    @Property(order = 12)
    public String getType() {
        return this.scriptType;
    }

    @NotNull
    @Property(hidden = true, editable = true, updatable = true)
    public String getSql() {
        return this.scriptSQL;
    }

    @NotNull
    @Property(viewable = true, order = 6)
    public Timestamp getCreationTime() {
        return this.createTime;
    }

    @Property(category = ExasolConstants.CAT_OWNER)
    public String getOwner() {
        return owner;
    }


    @Override
    public ExasolSchema getContainer() {
        return exasolSchema;
    }

    @Override
    public DBSProcedureType getProcedureType() {

        if (CommonUtils.equalObjects(scriptType, "SCRIPTING") || CommonUtils.equalObjects(scriptType, "ADAPTER"))
            return DBSProcedureType.PROCEDURE;
        else
            return DBSProcedureType.FUNCTION;
    }

    @Override
    public Collection<? extends DBSProcedureParameter> getParameters(DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(), getContainer(), this);
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return this.scriptSQL;
    }

    @Override
    public void setObjectDefinitionText(String sourceText) throws DBException
    {
        this.scriptSQL = sourceText;
    }
    
    @Override
    public void setDescription(String description)
    {
        this.remarks = description;
    }


}
