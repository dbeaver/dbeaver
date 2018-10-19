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
package org.jkiss.dbeaver.ext.postgresql.model.impls.redshift;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * PostgreTable base
 */
public class RedshiftExternalTable extends JDBCTable<PostgreDataSource, RedshiftExternalSchema> implements DBPRefreshableObject
{
    private static final Log log = Log.getLog(RedshiftExternalTable.class);
    private String location;
    private String inputFormat;
    private String outputFormat;
    private String serializationLib;
    private String serdeParameters;
    private int compressed;
    private String parameters;

    protected RedshiftExternalTable(RedshiftExternalSchema catalog)
    {
        super(catalog, false);
    }

    protected RedshiftExternalTable(
        RedshiftExternalSchema catalog,
        ResultSet dbResult)
    {
        super(catalog, JDBCUtils.safeGetString(dbResult, "tablename"), true);

        this.location = JDBCUtils.safeGetString(dbResult, "location");
        this.inputFormat = JDBCUtils.safeGetString(dbResult, "input_format");
        this.outputFormat = JDBCUtils.safeGetString(dbResult, "output_format");
        this.serializationLib = JDBCUtils.safeGetString(dbResult, "serialization_lib");
        this.serdeParameters = JDBCUtils.safeGetString(dbResult, "serde_parameters");
        this.compressed = JDBCUtils.safeGetInt(dbResult, "compressed");
        this.parameters = JDBCUtils.safeGetString(dbResult, "parameters");
    }

    // Copy constructor
    public RedshiftExternalTable(RedshiftExternalSchema container, DBSEntity source, boolean persisted) {
        super(container, source, persisted);
        if (source instanceof RedshiftExternalTable) {
            RedshiftExternalTable rsSource = (RedshiftExternalTable)source;
            this.location = rsSource.location;
            this.inputFormat = rsSource.inputFormat;
            this.outputFormat = rsSource.outputFormat;
            this.serializationLib = rsSource.serializationLib;
            this.serdeParameters = rsSource.serdeParameters;
            this.compressed = rsSource.compressed;
            this.parameters = rsSource.parameters;
        }
    }

    @Override
    public JDBCStructCache<RedshiftExternalSchema, ? extends RedshiftExternalTable, ? extends RedshiftExternalTableColumn> getCache()
    {
        return getContainer().externalTableCache;
    }

    @Property(viewable = true, order = 10)
    public String getLocation() {
        return location;
    }

    @Property(viewable = true, order = 11)
    public String getInputFormat() {
        return inputFormat;
    }

    @Property(viewable = true, order = 12)
    public String getOutputFormat() {
        return outputFormat;
    }

    @Property(viewable = false, order = 13)
    public String getSerializationLib() {
        return serializationLib;
    }

    @Property(viewable = false, order = 14)
    public String getSerdeParameters() {
        return serdeParameters;
    }

    @Property(viewable = false, order = 15)
    public int getCompressed() {
        return compressed;
    }

    @Property(viewable = false, order = 16)
    public String getParameters() {
        return parameters;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
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
    public RedshiftExternalSchema getSchema() {
        return super.getContainer();
    }

    /**
     * Table columns
     * @param monitor progress monitor
     */
    @Override
    public List<RedshiftExternalTableColumn> getAttributes(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return getContainer().externalTableCache.getChildren(monitor, getContainer(), this);
    }

    public List<RedshiftExternalTableColumn> getCachedAttributes()
    {
        final DBSObjectCache<RedshiftExternalTable, RedshiftExternalTableColumn> childrenCache = getContainer().externalTableCache.getChildrenCache(this);
        if (childrenCache != null) {
            return childrenCache.getCachedObjects();
        }
        return Collections.emptyList();
    }

    @Override
    public RedshiftExternalTableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName)
        throws DBException
    {
        return getContainer().externalTableCache.getChild(monitor, getContainer(), this, attributeName);
    }

    @Override
    public boolean isView() {
        return false;
    }

    @Override
    public Collection<? extends DBSTableIndex> getIndexes(DBRProgressMonitor monitor) throws DBException {
        return null;
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
        return getContainer().externalTableCache.refreshObject(monitor, getContainer(), this);
    }

}
