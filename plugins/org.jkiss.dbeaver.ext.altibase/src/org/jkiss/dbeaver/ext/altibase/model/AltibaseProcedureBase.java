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
package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.altibase.AltibaseConstants;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPStatefulObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.DBSObjectWithScript;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class AltibaseProcedureBase extends GenericProcedure 
implements DBSObjectWithScript, DBPStatefulObject, DBPRefreshableObject {

    protected static final Log log = Log.getLog(AltibaseProcedureBase.class);
    
    protected String schemaName = null;
    protected boolean valid = false;    
    protected List<GenericProcedureParameter> columns;
    protected DBSProcedureType procedureType;

    /**
     * Constructor
     */
    public AltibaseProcedureBase(GenericStructContainer container, String procedureName, boolean valid,
            DBSProcedureType procedureType, GenericFunctionResultType functionResultType) {
        super(container, procedureName, procedureName, "", procedureType, functionResultType);
        schemaName = container.getName();
        this.procedureType = procedureType;
        this.valid = valid;
    }

    @Override
    public void addColumn(GenericProcedureParameter column) {
        if (this.columns == null) {
            this.columns = new ArrayList<>();
        }
        this.columns.add(new AltibaseProcedureParameter(column));
    }

    @Nullable
    @Override
    public Collection<GenericProcedureParameter> getParameters(@NotNull DBRProgressMonitor monitor)
            throws DBException {
        if (columns == null) {
            loadProcedureColumns(monitor);
        }
        return columns;
    }

    @Nullable
    @Override
    @Property(viewable = false, hidden = true, length = PropertyLength.MULTILINE, order = 100)
    public String getDescription() {
        return description;
    }
    
    @Property(viewable = false, hidden = true, order = 3, labelProvider = GenericCatalog.CatalogNameTermProvider.class)
    public GenericCatalog getCatalog() {
        return getContainer().getCatalog();
    }
    
    @Property(viewable = false, hidden = true, order = 7)
    public GenericFunctionResultType getFunctionResultType() {
        return super.getFunctionResultType();
    }
    
    /**
     * Set procedure type, especially for Typeset
     */
    public void setProcedureType(DBSProcedureType procedureType) {
        this.procedureType = procedureType;
    }
    
    /**
     * Get Procedure type, especially for TYPESET
     */
    public String getProcedureTypeName() {
        if (procedureType == DBSProcedureType.UNKNOWN) {
            return AltibaseConstants.OBJ_TYPE_TYPESET;
        } else {
            return procedureType.name();
        }
    }
    
    @Override
    @Property(hidden = true, editable = true, updatable = true)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return super.getObjectDefinitionText(monitor, options);
    }

    @Override
    public void setObjectDefinitionText(String source) {
        super.setSource(source);
    }
    
    public String getSchemaName() {
        return schemaName;
    }
    
    protected void refreshState(JDBCSession session) throws DBCException {
        try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT status FROM system_.sys_users_ u, system_.sys_procedures_ p"
                        + " WHERE u.user_id = p.user_id AND u.user_name = ? AND proc_name = ?")) {
            dbStat.setString(1, schemaName);
            dbStat.setString(2, getName());

            dbStat.executeStatement();

            try (JDBCResultSet dbResult = dbStat.getResultSet()) {
                if (dbResult != null && dbResult.next()) {
                    valid = JDBCUtils.safeGetBoolean(dbResult, 1, "0"); // 0 is Valid, 1 is invalid
                }
            }
        } catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        }
    }

    @Override
    public void refreshObjectState(DBRProgressMonitor monitor) throws DBCException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, 
                "Refresh state of " + getProcedureTypeName() + " '" + this.getName() + "'")) {
            refreshState(session);
        }
    }

    @Nullable
    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, 
                "Refresh state of " + getProcedureTypeName() + " '" + this.getName() + "'")) {
            refreshState(session);
        }
        
        return this;
    }
    
    @NotNull
    @Override
    public DBSObjectState getObjectState() {
        return valid ? DBSObjectState.NORMAL : DBSObjectState.INVALID;
    }
}
