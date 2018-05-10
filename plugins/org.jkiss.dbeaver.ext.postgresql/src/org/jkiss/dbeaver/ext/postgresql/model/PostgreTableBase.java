/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.*;

/**
 * PostgreTable base
 */
public abstract class PostgreTableBase extends JDBCTable<PostgreDataSource, PostgreSchema> implements PostgreClass, PostgreScriptObject, PostgrePermissionsOwner, DBPNamedObject2
{
    private long oid;
    private long ownerId;
    private String description;
	private boolean isPartition;
    private Object acl;

    protected PostgreTableBase(PostgreSchema catalog)
    {
        super(catalog, false);
    }

    protected PostgreTableBase(
        PostgreSchema catalog,
        ResultSet dbResult)
    {
        super(catalog, JDBCUtils.safeGetString(dbResult, "relname"), true);
        this.oid = JDBCUtils.safeGetLong(dbResult, "oid");
        this.ownerId = JDBCUtils.safeGetLong(dbResult, "relowner");
        this.description = JDBCUtils.safeGetString(dbResult, "description");
        this.isPartition =
            getDataSource().isServerVersionAtLeast(10, 0) &&
            JDBCUtils.safeGetBoolean(dbResult, "relispartition");
        this.acl = JDBCUtils.safeGetObject(dbResult, "relacl");
    }

    // Copy constructor
    public PostgreTableBase(PostgreSchema container, DBSEntity source, boolean persisted) {
        super(container, source, persisted);
        if (source instanceof PostgreTableBase) {
            this.description = ((PostgreTableBase) source).description;
            this.isPartition = ((PostgreTableBase) source).isPartition;
        }
    }

    @Override
    public JDBCStructCache<PostgreSchema, ? extends PostgreClass, ? extends PostgreAttribute> getCache()
    {
        return getContainer().tableCache;
    }

    @NotNull
    @Override
    public PostgreDatabase getDatabase() {
        return getContainer().getDatabase();
    }

    @Property(viewable = true, editable = false, updatable = false, order = 9)
    @Override
    public long getObjectId() {
        return this.oid;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 100)
    @Nullable
    @Override
    public String getDescription()
    {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Property(viewable = true, editable = false, updatable = false, order = 10)
    public PostgreRole getOwner(DBRProgressMonitor monitor) throws DBException {
        return PostgreUtils.getObjectById(monitor, getDatabase().roleCache, getDatabase(), ownerId);
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getContainer(),
            this);
    }

    @NotNull
    public PostgreSchema getSchema() {
        final DBSObject parentObject = super.getParentObject();
        assert parentObject != null;
        return (PostgreSchema) parentObject;
    }

    /**
     * Table columns
     * @param monitor progress monitor
     * @throws DBException
     */
    @Override
    public List<PostgreTableColumn> getAttributes(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return getContainer().tableCache.getChildren(monitor, getContainer(), this);
    }

    public List<PostgreTableColumn> getCachedAttributes()
    {
        final DBSObjectCache<PostgreTableBase, PostgreTableColumn> childrenCache = getContainer().tableCache.getChildrenCache(this);
        if (childrenCache != null) {
            return childrenCache.getCachedObjects();
        }
        return Collections.emptyList();
    }

    @Override
    public PostgreTableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName)
        throws DBException
    {
        return getContainer().tableCache.getChild(monitor, getContainer(), this, attributeName);
    }

    @Override
    public Collection<PostgreTableConstraint> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    public PostgreTableConstraintBase getConstraint(@NotNull DBRProgressMonitor monitor, String ukName)
        throws DBException
    {
        return null;
    }

    @Override
    @Association
    public Collection<? extends DBSEntityAssociation> getReferences(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return null;
    }

    @Association
    @Override
    public synchronized Collection<? extends DBSEntityAssociation> getAssociations(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return null;
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        getContainer().constraintCache.clearObjectCache(this);
        getContainer().indexCache.clearObjectCache(this);
        return getContainer().tableCache.refreshObject(monitor, getContainer(), this);
    }

    @Override
    public Collection<PostgrePermission> getPermissions(DBRProgressMonitor monitor, boolean includeNestedObjects) throws DBException {
        List<PostgrePermission> tablePermissions = PostgreUtils.extractPermissionsFromACL(this, acl);
        if (!includeNestedObjects) {
            return tablePermissions;
        }
        tablePermissions = new ArrayList<>(tablePermissions);
        for (PostgreTableColumn column : CommonUtils.safeCollection(getAttributes(monitor))) {
            tablePermissions.addAll(column.getPermissions(monitor, true));
        }
        return tablePermissions;
    }

	public boolean isPartition() {
		return isPartition;
	}
    
    
}
