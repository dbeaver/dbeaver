/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleStatefulObject;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.IPropertyCacheValidator;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSForeignKey;
import org.jkiss.dbeaver.model.struct.DBSIndex;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.DBSObjectStateful;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.List;

/**
 * OracleTable base
 */
public abstract class OracleTableBase extends JDBCTable<OracleDataSource, OracleSchema> implements DBPNamedObject2, OracleStatefulObject
{
    static final Log log = LogFactory.getLog(OracleTableBase.class);

    public static class TableAdditionalInfo {
        volatile boolean loaded = false;

        public boolean isLoaded() { return loaded; }
    }

    public static class AdditionalInfoValidator implements IPropertyCacheValidator<OracleTableBase> {
        public boolean isPropertyCached(OracleTableBase object, Object propertyId)
        {
            return object.getAdditionalInfo().isLoaded();
        }
    }

    public abstract TableAdditionalInfo getAdditionalInfo();

    protected abstract String getTableTypeName();

    protected boolean valid;
    private String comment;
    private List<OracleTableColumn> columns;
    private List<OracleConstraint> constraints;

    protected OracleTableBase(OracleSchema schema, String name, boolean persisted)
    {
        super(schema, name, persisted);
    }

    protected OracleTableBase(OracleSchema oracleSchema, ResultSet dbResult)
    {
        super(oracleSchema, true);
        setName(JDBCUtils.safeGetString(dbResult, "TABLE_NAME"));
        this.valid = "VALID".equals(JDBCUtils.safeGetString(dbResult, "STATUS"));
        this.comment = JDBCUtils.safeGetString(dbResult, "COMMENTS");
    }

    public OracleSchema getSchema()
    {
        return super.getContainer();
    }

    @Property(name = "Name", viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1, description = "Name of the table")
    public String getName()
    {
        return super.getName();
    }

    @Property(name = "Comments", viewable = true, editable = true, order = 100)
    public String getDescription()
    {
        return comment;
    }

    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getContainer(),
            this);
    }

    public List<OracleTableColumn> getColumns(DBRProgressMonitor monitor)
        throws DBException
    {
        if (columns == null) {
            getContainer().tableCache.loadChildren(monitor, getContainer(), this);
        }
        return columns;
    }

    public OracleTableColumn getColumn(DBRProgressMonitor monitor, String columnName)
        throws DBException
    {
        return DBUtils.findObject(getColumns(monitor), columnName);
    }

    @Override
    public boolean refreshEntity(DBRProgressMonitor monitor) throws DBException
    {
        columns = null;
        constraints = null;
        return true;
    }

    public boolean isColumnsCached()
    {
        return columns != null;
    }

    public void setColumns(List<OracleTableColumn> columns)
    {
        this.columns = columns;
    }

    @Association
    public Collection<OracleTrigger> getTriggers(DBRProgressMonitor monitor)
        throws DBException
    {
        return getContainer().triggerCache.getObjects(monitor, getContainer(), this);
    }

    public List<? extends DBSIndex> getIndexes(DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    @Association
    public List<OracleConstraint> getConstraints(DBRProgressMonitor monitor)
        throws DBException
    {
        if (constraints == null) {
            getContainer().constraintCache.getObjects(monitor, getContainer(), this);
        }
        return constraints;
    }

    public OracleConstraint getConstraint(DBRProgressMonitor monitor, String ukName)
        throws DBException
    {
        return DBUtils.findObject(getConstraints(monitor), ukName);
    }

    void setConstraints(List<OracleConstraint> constraints)
    {
        this.constraints = constraints;
    }

    List<OracleConstraint> getConstraintsCache()
    {
        return constraints;
    }

    public DBSForeignKey getForeignKey(DBRProgressMonitor monitor, String ukName) throws DBException
    {
        return DBUtils.findObject(getForeignKeys(monitor), ukName);
    }

    public List<? extends DBSForeignKey> getForeignKeys(DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    public List<? extends DBSForeignKey> getReferences(DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    public String getDDL(DBRProgressMonitor monitor)
        throws DBException
    {
        return OracleUtils.getDDL(monitor, getTableTypeName(), this);
    }

    public DBSObjectState getObjectState()
    {
        return valid ? DBSObjectState.NORMAL : DBSObjectState.INVALID;
    }

    public static OracleTableBase findTable(DBRProgressMonitor monitor, OracleDataSource dataSource, String ownerName, String tableName) throws DBException
    {
        OracleSchema refSchema = dataSource.getSchema(monitor, ownerName);
        if (refSchema == null) {
            log.warn("Referenced schema '" + ownerName + "' not found");
            return null;
        } else {
            OracleTableBase refTable = refSchema.tableCache.getObject(monitor, refSchema, tableName);
            if (refTable == null) {
                log.warn("Referenced table '" + tableName + "' not found in schema '" + ownerName + "'");
            }
            return refTable;
        }
    }

}
