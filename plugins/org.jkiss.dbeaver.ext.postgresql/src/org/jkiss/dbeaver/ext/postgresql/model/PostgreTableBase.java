/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.util.*;

/**
 * PostgreTable base
 */
public abstract class PostgreTableBase extends JDBCTable<PostgreDataSource, PostgreSchema> implements PostgreClass, PostgreScriptObject, PostgrePermissionsOwner, DBPNamedObject2
{
    private static final Log log = Log.getLog(PostgreTableBase.class);

    private long oid;
    private long ownerId;
    private String description;
	private boolean isPartition;
    private Object acl;
    private String[] relOptions;

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
        if (getDataSource().isServerVersionAtLeast(8, 2)) {
            this.relOptions = JDBCUtils.safeGetArray(dbResult, "reloptions");
        }
        //this.reloptions = PostgreUtils.parseObjectString()
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
        return getContainer().getTableCache();
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

    @Property(order = 90)
    @Nullable
    public String[] getRelOptions() {
        return relOptions;
    }

    public Object getAcl() {
        return acl;
    }

    @Property(viewable = true, editable = true, updatable = true, multiline = true, order = 100)
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
        return getDatabase().getRoleById(monitor, ownerId);
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
     */
    @Override
    public List<PostgreTableColumn> getAttributes(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return getContainer().getTableCache().getChildren(monitor, getContainer(), this);
    }

    public PostgreTableColumn getAttributeByPos(DBRProgressMonitor monitor, int position) throws DBException {
        for (PostgreTableColumn attr : getAttributes(monitor)) {
            if (attr.getOrdinalPosition() == position) {
                return attr;
            }
        }
        return null;
    }

    public List<PostgreTableColumn> getCachedAttributes()
    {
        final DBSObjectCache<PostgreTableBase, PostgreTableColumn> childrenCache = getContainer().getTableCache().getChildrenCache(this);
        if (childrenCache != null) {
            return childrenCache.getCachedObjects();
        }
        return Collections.emptyList();
    }

    @Override
    public PostgreTableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName)
        throws DBException
    {
        return getContainer().getTableCache().getChild(monitor, getContainer(), this, attributeName);
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
        return getContainer().getTableCache().refreshObject(monitor, getContainer(), this);
    }

    @Override
    public Collection<PostgrePermission> getPermissions(DBRProgressMonitor monitor, boolean includeNestedObjects) throws DBException {
        if (!isPersisted()) {
            return Collections.emptyList();
        }
        return getDataSource().getServerType().readObjectPermissions(monitor, this, includeNestedObjects);
    }

	public boolean isPartition() {
		return isPartition;
	}

    /**
     * Extra table DDL modifiers
     */
    public void appendTableModifiers(DBRProgressMonitor monitor, StringBuilder ddl) {
        // Nothing
    }

    @Override
    public String generateChangeOwnerQuery(String owner) {
        return "ALTER TABLE " + DBUtils.getObjectFullName(this, DBPEvaluationContext.DDL) + " OWNER TO " + owner;
    }

    public static class TablespaceListProvider implements IPropertyValueListProvider<PostgreTableBase> {
        @Override
        public boolean allowCustomValue()
        {
            return false;
        }
        @Override
        public Object[] getPossibleValues(PostgreTableBase object)
        {
            try {
                Collection<PostgreTablespace> tablespaces = object.getDatabase().getTablespaces(new VoidProgressMonitor());
                return tablespaces.toArray(new Object[tablespaces.size()]);
            } catch (DBException e) {
                log.error(e);
                return new Object[0];
            }
        }
    }

}
