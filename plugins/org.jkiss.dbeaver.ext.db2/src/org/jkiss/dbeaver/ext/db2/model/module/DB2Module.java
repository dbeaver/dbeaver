/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.db2.model.module;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.model.*;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2OwnerType;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectSimpleCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;

/**
 * DB2 Module
 * 
 * @author Denis Forveille
 */
public class DB2Module extends DB2SchemaObject implements DBSProcedureContainer, DBPRefreshableObject {

    private static final String C_CON = "SELECT * FROM SYSCAT.CONDITIONS WHERE CONDSCHEMA = ? AND CONDMODULENAME = ? ORDER BY CONDNAME WITH UR";
    private static final String C_FCT = "SELECT * FROM SYSCAT.ROUTINES WHERE ROUTINESCHEMA = ? AND ROUTINEMODULENAME = ? AND ROUTINETYPE = 'F' ORDER BY ROUTINENAME WITH UR";
    private static final String C_MOD = "SELECT * FROM SYSCAT.ROUTINES WHERE ROUTINESCHEMA = ? AND ROUTINEMODULENAME = ? AND ROUTINETYPE = 'M' ORDER BY ROUTINENAME WITH UR";
    private static final String C_PRC = "SELECT * FROM SYSCAT.ROUTINES WHERE ROUTINESCHEMA = ? AND ROUTINEMODULENAME = ? AND ROUTINETYPE = 'P' ORDER BY ROUTINENAME WITH UR";
    private static final String C_TYP = "SELECT * FROM SYSCAT.DATATYPES WHERE TYPESCHEMA = ? AND TYPEMODULENAME = ? ORDER BY TYPENAME WITH UR";
    private static final String C_VAR = "SELECT * FROM SYSCAT.VARIABLES WHERE VARSCHEMA = ? AND VARMODULENAME = ? ORDER BY VARNAME WITH UR";

    private final DBSObjectCache<DB2Module, DB2ModuleCondition> conditionCache;
    private final DBSObjectCache<DB2Module, DB2Routine> functionCache;
    private final DBSObjectCache<DB2Module, DB2Routine> methodCache;
    private final DBSObjectCache<DB2Module, DB2Routine> procedureCache;
    private final DBSObjectCache<DB2Module, DB2DataType> typeCache;
    private final DBSObjectCache<DB2Module, DB2Variable> variableCache;

    private Integer moduleId;
    private String owner;
    private DB2OwnerType ownerType;
    private String dialect;
    private DB2ModuleType type;
    private Timestamp createTime;
    private String remarks;

    // -----------------------
    // Constructors
    // -----------------------

    public DB2Module(DB2Schema schema, ResultSet dbResult)
    {
        super(schema, JDBCUtils.safeGetStringTrimmed(dbResult, "MODULENAME"), true);

        this.moduleId = JDBCUtils.safeGetInteger(dbResult, "MODULEID");
        this.owner = JDBCUtils.safeGetString(dbResult, "OWNER");
        this.ownerType = CommonUtils.valueOf(DB2OwnerType.class, JDBCUtils.safeGetString(dbResult, "OWNERTYPE"));
        this.dialect = JDBCUtils.safeGetString(dbResult, "DIALECT");
        this.type = CommonUtils.valueOf(DB2ModuleType.class, JDBCUtils.safeGetString(dbResult, "MODULETYPE"));
        this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATE_TIME");
        this.remarks = JDBCUtils.safeGetString(dbResult, "REMARKS");

        this.conditionCache = new JDBCObjectSimpleCache<>(DB2ModuleCondition.class, C_CON,
            schema.getName(), name);
        this.functionCache = new JDBCObjectSimpleCache<>(DB2Routine.class, C_FCT, schema.getName(), name);
        this.methodCache = new JDBCObjectSimpleCache<>(DB2Routine.class, C_MOD, schema.getName(), name);
        this.procedureCache = new JDBCObjectSimpleCache<>(DB2Routine.class, C_PRC, schema.getName(), name);
        this.typeCache = new JDBCObjectSimpleCache<>(DB2DataType.class, C_TYP, schema.getName(), name);
        this.variableCache = new JDBCObjectSimpleCache<>(DB2Variable.class, C_VAR, schema.getName(), name);

    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        conditionCache.clearCache();
        functionCache.clearCache();
        procedureCache.clearCache();
        typeCache.clearCache();
        variableCache.clearCache();
        return this;
    }

    // -----------------
    // Association
    // -----------------

    @Association
    public Collection<DB2ModuleCondition> getConditions(DBRProgressMonitor monitor) throws DBException
    {
        return conditionCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<DB2Routine> getFunctions(DBRProgressMonitor monitor) throws DBException
    {
        return functionCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<DB2Routine> getProcedures(DBRProgressMonitor monitor) throws DBException
    {
        return procedureCache.getAllObjects(monitor, this);
    }

    @Override
    public DBSProcedure getProcedure(DBRProgressMonitor monitor, String uniqueName) throws DBException {
        return procedureCache.getObject(monitor, this, uniqueName);
    }

    @Association
    public Collection<DB2DataType> getTypes(DBRProgressMonitor monitor) throws DBException
    {
        return typeCache.getAllObjects(monitor, this);
    }

    public DB2DataType getType(DBRProgressMonitor monitor, String name) throws DBException
    {
        return typeCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<DB2Variable> getVariables(DBRProgressMonitor monitor) throws DBException
    {
        return variableCache.getAllObjects(monitor, this);
    }

    // -----------------------
    // Properties
    // -----------------------

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return super.getName();
    }

    @Property(viewable = true, order = 2)
    public DB2Schema getSchema()
    {
        return parent;
    }

    @Property(viewable = true, order = 10)
    public Integer getModuleId()
    {
        return moduleId;
    }

    @Property(viewable = true, order = 11)
    public String getDialect()
    {
        return dialect;
    }

    @Property(viewable = true, order = 12)
    public DB2ModuleType getType()
    {
        return type;
    }

    @Property(viewable = false, order = 13, category = DB2Constants.CAT_OWNER)
    public String getOwner()
    {
        return owner;
    }

    @Property(viewable = false, order = 14, category = DB2Constants.CAT_OWNER)
    public DB2OwnerType getOwnerType()
    {
        return ownerType;
    }

    @Property(viewable = false, order = 15, category = DB2Constants.CAT_DATETIME)
    public Timestamp getCreateTime()
    {
        return createTime;
    }

    @Nullable
    @Override
    @Property(viewable = false, multiline = true)
    public String getDescription()
    {
        return remarks;
    }

    // -------------------------
    // Standards Getters
    // -------------------------
    public DBSObjectCache<DB2Module, DB2ModuleCondition> getConditionCache()
    {
        return conditionCache;
    }

    public DBSObjectCache<DB2Module, DB2Routine> getFunctionCache()
    {
        return functionCache;
    }

    public DBSObjectCache<DB2Module, DB2Routine> getMethodCache()
    {
        return methodCache;
    }

    public DBSObjectCache<DB2Module, DB2Routine> getProcedureCache()
    {
        return procedureCache;
    }

    public DBSObjectCache<DB2Module, DB2DataType> getTypeCache()
    {
        return typeCache;
    }

    public DBSObjectCache<DB2Module, DB2Variable> getVariableCache()
    {
        return variableCache;
    }

}
