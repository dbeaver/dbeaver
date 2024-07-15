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
package org.jkiss.dbeaver.ext.postgresql.model.impls.redshift;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * PostgreTable base
 */
public class RedshiftExternalTable extends PostgreTable implements DBPRefreshableObject
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
        super(catalog);
    }

    protected RedshiftExternalTable(
        RedshiftExternalSchema catalog,
        ResultSet dbResult)
    {
        super(catalog);
        setName(JDBCUtils.safeGetString(dbResult, "tablename"));
        setPersisted(true);

        this.location = JDBCUtils.safeGetString(dbResult, "location");
        this.inputFormat = JDBCUtils.safeGetString(dbResult, "input_format");
        this.outputFormat = JDBCUtils.safeGetString(dbResult, "output_format");
        this.serializationLib = JDBCUtils.safeGetString(dbResult, "serialization_lib");
        this.serdeParameters = JDBCUtils.safeGetString(dbResult, "serde_parameters");
        this.compressed = JDBCUtils.safeGetInt(dbResult, "compressed");
        this.parameters = JDBCUtils.safeGetString(dbResult, "parameters");
    }

    // Copy constructor
    public RedshiftExternalTable(DBRProgressMonitor monitor, RedshiftExternalSchema container, RedshiftExternalTable source, boolean persisted) throws DBException {
        super(monitor, container, source, persisted);
        this.location = source.location;
        this.inputFormat = source.inputFormat;
        this.outputFormat = source.outputFormat;
        this.serializationLib = source.serializationLib;
        this.serdeParameters = source.serdeParameters;
        this.compressed = source.compressed;
        this.parameters = source.parameters;
    }

    @Override
    public RedshiftExternalSchema getContainer() {
        return (RedshiftExternalSchema)super.getContainer();
    }

    ////////////////////////////////////////////
    // Remove standard PG table properties

    @Override
    public long getObjectId() {
        return 0;
    }

    @Override
    @Nullable
    public String[] getRelOptions() {
        return null;
    }

    @Override
    public PostgreRole getOwner(DBRProgressMonitor monitor) throws DBException {
        return null;
    }

        ////////////////////////////////////////////
    // Redshift table properties

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
        return (RedshiftExternalSchema) super.getContainer();
    }

    /**
     * Table columns
     * @param monitor progress monitor
     */
    @Override
    public List<RedshiftExternalTableColumn> getAttributes(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return getContainer().getExternalTableCache().getChildren(monitor, getContainer(), this);
    }

    public List<RedshiftExternalTableColumn> getCachedAttributes()
    {
        final DBSObjectCache<RedshiftExternalTable, RedshiftExternalTableColumn> childrenCache = getContainer().getExternalTableCache().getChildrenCache(this);
        if (childrenCache != null) {
            return childrenCache.getCachedObjects();
        }
        return Collections.emptyList();
    }

    @Override
    public RedshiftExternalTableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName)
        throws DBException
    {
        return getContainer().getExternalTableCache().getChild(monitor, getContainer(), this, attributeName);
    }

    @Override
    public boolean isView() {
        return false;
    }

    @Override
    public Collection<PostgreIndex> getIndexes(@NotNull DBRProgressMonitor monitor) throws DBException {
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
        return getContainer().getExternalTableCache().refreshObject(monitor, getContainer(), this);
    }

    @Override
    public void setObjectDefinitionText(String sourceText) throws DBException {

    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return null;
    }

    @Override
    protected void readTableStatistics(JDBCSession session) throws SQLException {
        // Not supported
    }
}
