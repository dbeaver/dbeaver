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
package org.jkiss.dbeaver.ext.kingbase.model;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.kingbase.KingbaseUtils;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBPScriptObjectExt2;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.dpi.DPIContainer;
import org.jkiss.dbeaver.model.dpi.DPIElement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectWithType;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

/**
 * KingbaseTableBase
 */
public abstract class KingbaseTableBase extends JDBCTable<KingbaseDataSource, KingbaseTableContainer>
    implements
    KingbaseClass,
    KingbaseScriptObject,
    DBPScriptObjectExt2,
    KingbasePrivilegeOwner,
    DBPNamedObject2,
    DBSObjectWithType
{
    private static final Log log = Log.getLog(KingbaseTableBase.class);

    private long oid;
    private long ownerId;
    private String description;
	private boolean isPartition;
    private KingbaseTablePersistence persistence;
    private Object acl;
    private String[] relOptions;

    protected KingbaseTableBase(KingbaseTableContainer container)
    {
        super(container, false);
        this.persistence = KingbaseTablePersistence.PERMANENT;
    }

    protected KingbaseTableBase(
        KingbaseTableContainer container,
        ResultSet dbResult)
    {
        super(container, JDBCUtils.safeGetString(dbResult, "relname"), true);
        this.oid = JDBCUtils.safeGetLong(dbResult, "oid");
        this.ownerId = JDBCUtils.safeGetLong(dbResult, "relowner");
        this.description = JDBCUtils.safeGetString(dbResult, "description");
        this.isPartition = JDBCUtils.safeGetBoolean(dbResult, "relispartition");
        this.acl = JDBCUtils.safeGetObject(dbResult, "relacl");
       
        this.relOptions = KingbaseUtils.safeGetStringArray(dbResult, "reloptions");
        
        persistence = KingbaseTablePersistence.getByCode(JDBCUtils.safeGetString(dbResult, "relpersistence"));
        
    }

    // Copy constructor
    public KingbaseTableBase(DBRProgressMonitor monitor, KingbaseTableContainer container, KingbaseTableBase source, boolean persisted) throws DBException {
        super(container, source, persisted);
        this.ownerId = source.ownerId;
        this.description = source.description;
        this.isPartition = source.isPartition;
        this.acl = source.acl;
        this.relOptions = source.relOptions;
        this.persistence = source.persistence;

        DBSObjectCache<KingbaseTableBase, KingbaseTableColumn> colCache = getSchema().getTableCache().getChildrenCache(this);
        // Copy columns
        for (KingbaseTableColumn srcColumn : CommonUtils.safeCollection(source.getAttributes(monitor))) {
            if (DBUtils.isHiddenObject(srcColumn)) {
                continue;
            }
            KingbaseTableColumn column = new KingbaseTableColumn(monitor, this, srcColumn);
            colCache.cacheObject(column);
        }
    }

    @Override
    public JDBCStructCache<KingbaseTableContainer, ? extends KingbaseClass, ? extends KingbaseAttribute> getCache()
    {
        return getContainer().getSchema().getTableCache();
    }

    @NotNull
    @Override
    public KingbaseDatabase getDatabase() {
        return getContainer().getDatabase();
    }

    @Property(viewable = true, order = 9)
    @Override
    public long getObjectId() {
        return this.oid;
    }

    @Property(viewable = true, length = PropertyLength.MULTILINE, order = 90)
    @Nullable
    public String[] getRelOptions() {
        return relOptions;
    }

    public Object getAcl() {
        return acl;
    }

    @Property(viewable = true, editable = true, updatable = true, length = PropertyLength.MULTILINE, order = 100)
    @Nullable
    @Override
    public String getDescription()
    {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTableTypeName() {
        return "TABLE";
    }

    @Property(viewable = true, order = 10)
    public KingbaseRole getOwner(DBRProgressMonitor monitor) throws DBException {
        return getDatabase().getRoleById(monitor, ownerId);
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        KingbaseDatabase database = getDatabase();
        return DBUtils.getFullQualifiedName(getDataSource(),
            database.isSharedDatabase() ? database : null,
            getSchema(),
            this);
    }

    @DPIContainer
    @NotNull
    public KingbaseSchema getSchema() {
        final DBSObject parentObject = super.getParentObject();
        assert parentObject != null;
        return parentObject instanceof KingbaseSchema ?
            (KingbaseSchema) parentObject :
            ((KingbaseTableBase) parentObject).getSchema();
    }

    /**
     * Table columns
     * @param monitor progress monitor
     */
    @Override
    public List<? extends KingbaseTableColumn> getAttributes(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return getContainer().getSchema().getTableCache().getChildren(monitor, getContainer(), this);
    }

    protected KingbaseTableColumn getAttributeByPos(DBRProgressMonitor monitor, int position) throws DBException {
        for (KingbaseTableColumn attr : CommonUtils.safeCollection(getAttributes(monitor))) {
            if (attr.getOrdinalPosition() == position) {
                return attr;
            }
        }
        return null;
    }

    @Association
    public List<? extends KingbaseTableColumn> getCachedAttributes()
    {
        final DBSObjectCache<KingbaseTableBase, KingbaseTableColumn> childrenCache = getContainer().getSchema().getTableCache().getChildrenCache(this);
        if (childrenCache != null) {
            return childrenCache.getCachedObjects();
        }
        return Collections.emptyList();
    }

    @Override
    public KingbaseTableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName)
        throws DBException
    {
        return getContainer().getSchema().getTableCache().getChild(monitor, getContainer(), this, attributeName);
    }

    @Override
    public Collection<KingbaseTableConstraint> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    public KingbaseTableConstraintBase getConstraint(@NotNull DBRProgressMonitor monitor, String ukName)
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
        KingbaseSchema schema = getContainer().getSchema();
        schema.getConstraintCache().clearObjectCache(this);
        if (schema.getIndexCache() != null) {
            schema.getIndexCache().clearObjectCache(this);
        }
        return schema.getTableCache().refreshObject(monitor, schema, this);
    }

    @Override
    public Collection<KingbasePrivilege> getPrivileges(@NotNull DBRProgressMonitor monitor, boolean includeNestedObjects) throws DBException {
        if (!isPersisted()) {
            return Collections.emptyList();
        }
        return getDataSource().getServerType().readObjectPermissions(monitor, this, includeNestedObjects);
    }

    @DPIElement(cache = true)
	public boolean isPartition() {
		return isPartition;
	}

    public void setPartition(boolean partition) {
        isPartition = partition;
    }

    @DPIElement(cache = true)
    @NotNull
    public KingbaseTablePersistence getPersistence() {
        return persistence;
    }

    /**
     * Extra table DDL modifiers
     */
    public void appendTableModifiers(DBRProgressMonitor monitor, StringBuilder ddl) {
        // Nothing
    }

    @Override
    public String generateChangeOwnerQuery(@NotNull String owner, @NotNull Map<String, Object> options) {
        return "ALTER TABLE " + DBUtils.getEntityScriptName(this, options) + " OWNER TO " + owner;
    }

    @Override
    public boolean supportsObjectDefinitionOption(String option) {
        if (DBPScriptObject.OPTION_INCLUDE_COMMENTS.equals(option) && getDataSource().getServerType().supportsShowingOfExtraComments()) {
            return true;
        }
        if (DBPScriptObject.OPTION_INCLUDE_PERMISSIONS.equals(option)) {
            return true;
        }
        return !this.isView() &&
               (DBPScriptObject.OPTION_DDL_ONLY_FOREIGN_KEYS.equals(option) ||
               DBPScriptObject.OPTION_DDL_SKIP_FOREIGN_KEYS.equals(option));
    }

    public KingbaseTableColumn createTableColumn(DBRProgressMonitor monitor, KingbaseSchema schema, JDBCResultSet dbResult)
        throws DBException
    {
        return new KingbaseTableColumn(monitor, this, dbResult);
    }

    public static class TablespaceListProvider implements IPropertyValueListProvider<KingbaseTableBase> {
        @Override
        public boolean allowCustomValue()
        {
            return false;
        }
        @Override
        public Object[] getPossibleValues(KingbaseTableBase object)
        {
            if (!object.getDataSource().getServerType().supportsTablespaces()) {
                return new Object[0];
            }
            try {
                Collection<KingbaseTablespace> tablespaces = object.getDatabase().getTablespaces(new VoidProgressMonitor());
                return tablespaces.toArray(new Object[0]);
            } catch (DBException e) {
                log.error(e);
                return new Object[0];
            }
        }
    }

}
