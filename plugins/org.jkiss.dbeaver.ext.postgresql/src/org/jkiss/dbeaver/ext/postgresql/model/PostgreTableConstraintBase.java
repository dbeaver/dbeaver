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
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableConstraint;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * PostgreTableConstraintBase
 */
public abstract class PostgreTableConstraintBase<COLUMN extends PostgreTableConstraintColumn> extends JDBCTableConstraint<PostgreTableBase, COLUMN>
        implements PostgreObject, PostgreScriptObject, DBPInheritedObject, DBPNamedObject2 {
    private static final Log log = Log.getLog(PostgreTableConstraintBase.class);

    private long oid;
    private String constrDDL;
    private long indexId;
    private boolean isLocal;
    private boolean deferrable;
    private boolean deferred;

    public PostgreTableConstraintBase(PostgreTableBase table, String name, DBSEntityConstraintType constraintType, JDBCResultSet resultSet) throws DBException {
        super(table, name, null, constraintType, true);

        this.oid = JDBCUtils.safeGetLong(resultSet, "oid");
        this.indexId = JDBCUtils.safeGetLong(resultSet, "conindid");
        this.isLocal =
            !getDataSource().getServerType().supportsInheritance() ||
            this instanceof PostgreTableInheritance ||
            JDBCUtils.safeGetBoolean(resultSet, "conislocal", true);
        this.deferrable = JDBCUtils.safeGetBoolean(resultSet, "condeferrable");
        this.deferred = JDBCUtils.safeGetBoolean(resultSet, "condeferred");

        this.description = JDBCUtils.safeGetString(resultSet, "description");
    }

    public PostgreTableConstraintBase(PostgreTableBase table, String constraintName, DBSEntityConstraintType constraintType) {
        super(table, constraintName, null, constraintType, false);
        // conislocal is true by default for constraints which have such a parameter in metadata
        this.isLocal = getDataSource().getServerType().supportsInheritance() || this instanceof PostgreTableInheritance;
    }

    public PostgreTableConstraintBase(DBRProgressMonitor monitor, PostgreTableReal owner, PostgreTableConstraintBase srcConstr) throws DBException {
        super(owner, srcConstr, false);
        // Make constraint name unique
        int postfix = 1;
        while (owner.getSchema().getConstraintCache().getObject(monitor, owner.getSchema(), getName()) != null) {
            setName(srcConstr.getName() + "_" + postfix);
            postfix++;
        }
        this.isLocal = srcConstr.isLocal;
        this.deferrable = srcConstr.deferrable;
        this.deferred = srcConstr.deferred;

        this.description = srcConstr.description;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getTable().getContainer(),
            getTable(),
            this);
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    @NotNull
    @Override
    public PostgreDatabase getDatabase() {
        return getParentObject().getDatabase();
    }

    @Property(viewable = false, order = 10)
    @Override
    public long getObjectId() {
        return oid;
    }

    @Property(viewable = false, order = 11)
    public boolean isDeferrable() {
        return deferrable;
    }

    public void setDeferrable(boolean deferrable) {
        this.deferrable = deferrable;
    }

    @Property(viewable = false, order = 12)
    public boolean isDeferred() {
        return deferred;
    }

    public void setDeferred(boolean deferred) {
        this.deferred = deferred;
    }

    @Property(viewable = true, editable = true, updatable = true, length = PropertyLength.MULTILINE, order = 100)
    @Nullable
    @Override
    public String getDescription()
    {
        return super.getDescription();
    }

    //@Property(viewable = true, order = 12)
    public PostgreIndex getIndex(DBRProgressMonitor monitor) throws DBException {
        return indexId == 0 ? null : getTable().getSchema().getIndex(monitor, indexId);
    }

    @Override
    public boolean isInherited() {
        return !isLocal;
    }

    abstract void cacheAttributes(DBRProgressMonitor monitor, List<? extends PostgreTableConstraintColumn> children, boolean secondPass);

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException
    {
        if (constrDDL == null && isPersisted()) {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read constraint definition")) {
                constrDDL =
                    "CONSTRAINT " + DBUtils.getQuotedIdentifier(this) + " " +
                    JDBCUtils.queryString(session, "SELECT pg_catalog.pg_get_constraintdef(?)", getObjectId());
            } catch (SQLException e) {
                throw new DBDatabaseException(e, getDataSource());
            }
        }
        if (CommonUtils.getOption(options, DBPScriptObject.OPTION_EMBEDDED_SOURCE)) {
            return constrDDL;
        } else {
            return "ALTER TABLE " + getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + " ADD " + constrDDL;
        }
    }

    @Override
    public void setObjectDefinitionText(String sourceText) throws DBException {
        throw new DBException("Constraints DDL is read-only");
    }
}
