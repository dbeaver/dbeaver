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
package org.jkiss.dbeaver.ext.tibero.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.tibero.model.source.TiberoSourceObject;
import org.jkiss.dbeaver.ext.tibero.model.source.TiberoSourcePersistAction;
import org.jkiss.dbeaver.ext.tibero.model.source.TiberoSourceType;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.DBPScriptObjectExt;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSPackage;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class TiberoPackage implements
    TiberoSourceObject,
    DBPScriptObjectExt,
    DBSObjectContainer,
    DBSPackage,
    DBPRefreshableObject,
    DBPSaveableObject,
    DBPNamedObject2,
    DBSProcedureContainer {

    private final TiberoSchema schema;
    private final String name;
    private final List<TiberoProcedurePackaged> procedures = new ArrayList<>();
    private boolean persisted = true;
    private boolean valid = true;
    private Date created;
    private Date lastDDLTime;
    private boolean temporary;
    private String sourceDeclaration;
    private String sourceDefinition;

    public TiberoPackage(@NotNull TiberoSchema schema, @NotNull String name) {
        this.schema = schema;
        this.name = name;
    }

    public TiberoPackage(@NotNull TiberoSchema schema, @NotNull ResultSet dbResult) {
        this.schema = schema;
        this.name = JDBCUtils.safeGetString(dbResult, "OBJECT_NAME");
        this.valid = JDBCUtils.safeGetInt(dbResult, "ALL_VALID") == 1;
        this.created = JDBCUtils.safeGetTimestamp(dbResult, "CREATED");
        this.lastDDLTime = JDBCUtils.safeGetTimestamp(dbResult, "LAST_DDL_TIME");
        this.temporary = false;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public DBSObject getParentObject() {
        return schema;
    }

    @NotNull
    @Override
    public TiberoDataSource getDataSource() {
        return (TiberoDataSource) schema.getDataSource();
    }

    @NotNull
    @Override
    public TiberoSchema getSchema() {
        return schema;
    }

    @Override
    public boolean isPersisted() {
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted) {
        this.persisted = persisted;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(@NotNull DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(), schema, this);
    }

    @Override
    public void setName(@NotNull String name) {
        throw new UnsupportedOperationException("Tibero package rename is not supported yet");
    }

    @Override
    public TiberoSourceType getSourceType() {
        return TiberoSourceType.PACKAGE;
    }

    @Property(viewable = true, order = 2)
    public Date getCreated() {
        return created;
    }

    @Property(viewable = true, order = 3)
    public Date getLastDDLTime() {
        return lastDDLTime;
    }

    @Property(viewable = true, order = 4)
    public boolean isTemporary() {
        return temporary;
    }

    @Property(viewable = true, order = 5)
    public boolean isValid() {
        return valid;
    }

    @NotNull
    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(@NotNull DBRProgressMonitor monitor, @Nullable Map<String, Object> options) throws DBException {
        if (sourceDeclaration == null || (options != null && Boolean.TRUE.equals(options.get(OPTION_REFRESH)))) {
            sourceDeclaration = TiberoUtils.loadObjectSource(monitor, this, "PACKAGE", schema.getName());
        }
        return sourceDeclaration;
    }

    @Override
    public void setObjectDefinitionText(String source) {
        sourceDeclaration = source;
    }

    @NotNull
    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getExtendedDefinitionText(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (sourceDefinition == null) {
            sourceDefinition = TiberoUtils.loadObjectSource(monitor, this, "PACKAGE BODY", schema.getName());
        }
        return sourceDefinition;
    }

    public void setExtendedDefinitionText(String source) {
        sourceDefinition = source;
    }

    @Association
    public Collection<TiberoProcedurePackaged> getProcedures(@NotNull DBRProgressMonitor monitor) {
        return procedures;
    }

    @Association
    public Collection<TiberoProcedurePackaged> getProceduresOnly(@NotNull DBRProgressMonitor monitor) {
        List<TiberoProcedurePackaged> result = new ArrayList<>();
        for (TiberoProcedurePackaged procedure : procedures) {
            if (procedure.getProcedureType() == org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType.PROCEDURE) {
                result.add(procedure);
            }
        }
        return result;
    }

    @Association
    public Collection<TiberoProcedurePackaged> getFunctionsOnly(@NotNull DBRProgressMonitor monitor) {
        List<TiberoProcedurePackaged> result = new ArrayList<>();
        for (TiberoProcedurePackaged procedure : procedures) {
            if (procedure.getProcedureType() == org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType.FUNCTION) {
                result.add(procedure);
            }
        }
        return result;
    }

    @Override
    public DBSProcedure getProcedure(@NotNull DBRProgressMonitor monitor, @NotNull String uniqueName) {
        return DBUtils.findObject(procedures, uniqueName);
    }

    @Override
    public Collection<? extends DBSObject> getChildren(@NotNull DBRProgressMonitor monitor) {
        return procedures;
    }

    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) {
        return DBUtils.findObject(procedures, childName);
    }

    @NotNull
    @Override
    public Class<? extends DBSObject> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) {
        return TiberoProcedurePackaged.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {
        getDataSource().loadPackageProcedures(monitor, this);
    }

    public void addProcedure(@NotNull TiberoProcedurePackaged procedure) {
        procedures.add(procedure);
    }

    public void clearProcedures() {
        procedures.clear();
    }

    public void orderProcedures() {
        DBUtils.orderObjects(procedures);
    }

    @NotNull
    @Override
    public DBEPersistAction[] getCompileActions(@NotNull DBRProgressMonitor monitor) throws DBCException {
        List<DBEPersistAction> actions = new ArrayList<>();
        actions.add(new TiberoSourcePersistAction(
            TiberoSourceType.PACKAGE.name(),
            "Compile package",
            "ALTER PACKAGE " + getFullyQualifiedName(DBPEvaluationContext.DDL) + " COMPILE"
        ));
        try {
            if (!CommonUtils.isEmpty(getExtendedDefinitionText(monitor))) {
                actions.add(new TiberoSourcePersistAction(
                    TiberoSourceType.PACKAGE_BODY.name(),
                    "Compile package body",
                    "ALTER PACKAGE " + getFullyQualifiedName(DBPEvaluationContext.DDL) + " COMPILE BODY"
                ));
            } else {
                actions.add(new TiberoSourcePersistAction(
                    TiberoSourceType.PACKAGE_BODY.name(),
                    "Drop package body",
                    "DROP PACKAGE BODY " + getFullyQualifiedName(DBPEvaluationContext.DDL)
                ));
            }
        } catch (DBException e) {
            throw new DBCException("Error reading Tibero package body", e);
        }
        return actions.toArray(new DBEPersistAction[0]);
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    @NotNull
    @Override
    public DBSObjectState getObjectState() {
        return valid ? DBSObjectState.NORMAL : DBSObjectState.INVALID;
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException {
        boolean specValid = getDataSource().isObjectValid(monitor, this, "PACKAGE");
        boolean bodyValid = getDataSource().isObjectValid(monitor, this, "PACKAGE BODY");
        this.valid = specValid && bodyValid;
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        procedures.clear();
        sourceDeclaration = null;
        sourceDefinition = null;
        getDataSource().loadPackageProcedures(monitor, this);
        refreshObjectState(monitor);
        return this;
    }
}
