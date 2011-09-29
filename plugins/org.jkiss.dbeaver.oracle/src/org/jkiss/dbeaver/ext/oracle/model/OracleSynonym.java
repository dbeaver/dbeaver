/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;

/**
 * Oracle synonym
 */
public class OracleSynonym extends OracleSchemaObject {

    private String objectOwner;
    private String objectTypeName;
    private String objectName;
    private String dbLink;

    public OracleSynonym(OracleSchema schema, ResultSet dbResult)
    {
        super(schema, JDBCUtils.safeGetString(dbResult, "SYNONYM_NAME"), true);
        this.objectTypeName = JDBCUtils.safeGetString(dbResult, "OBJECT_TYPE");
        this.objectOwner = JDBCUtils.safeGetString(dbResult, "TABLE_OWNER");
        this.objectName = JDBCUtils.safeGetString(dbResult, "TABLE_NAME");
        this.dbLink = JDBCUtils.safeGetString(dbResult, "DB_LINK");
    }

    public OracleObjectType getObjectType()
    {
        return OracleObjectType.getByType(objectTypeName);
    }

    @Property(name = "Name", viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    public String getName()
    {
        return super.getName();
    }

    @Property(name = "Object Type", viewable = true, order = 2)
    public String getObjectTypeName()
    {
        return objectTypeName;
    }

    @Property(name = "Object Owner", viewable = true, order = 3)
    public Object getObjectOwner()
    {
        final OracleSchema schema = getDataSource().schemaCache.getCachedObject(objectOwner);
        return schema == null ? objectOwner : schema;
    }

    @Property(name = "Object", viewable = true, order = 4)
    public Object getObject(DBRProgressMonitor monitor) throws DBException
    {
        return OracleObjectType.resolveObject(
            monitor,
            getDataSource(),
            dbLink,
            objectTypeName,
            objectOwner,
            objectName);
    }

    @Property(name = "DB Link", viewable = true, order = 5)
    public Object getDbLink(DBRProgressMonitor monitor) throws DBException
    {
        return OracleDBLink.resolveObject(monitor, getSchema(), dbLink);
    }

}
