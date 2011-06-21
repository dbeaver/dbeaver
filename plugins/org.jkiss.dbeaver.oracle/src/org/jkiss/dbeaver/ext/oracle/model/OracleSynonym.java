/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;

/**
 * Oracle synonym
 */
public class OracleSynonym extends OracleSchemaObject {

    private OracleSchema objectOwner;
    private String objectTypeName;
    private String objectName;
    private OracleDBLink dbLink;

    public OracleSynonym(DBRProgressMonitor monitor, OracleSchema schema, ResultSet dbResult) throws DBException
    {
        super(schema, JDBCUtils.safeGetString(dbResult, "SYNONYM_NAME"), true);
        this.objectTypeName = JDBCUtils.safeGetString(dbResult, "OBJECT_TYPE");
        this.objectOwner = schema.getDataSource().schemaCache.getCachedObject(
            JDBCUtils.safeGetString(dbResult, "TABLE_OWNER"));
        this.objectName = JDBCUtils.safeGetString(dbResult, "TABLE_NAME");
        final String dbLinkName = JDBCUtils.safeGetString(dbResult, "DB_LINK");
        if (!CommonUtils.isEmpty(dbLinkName)) {
            this.dbLink = schema.dbLinkCache.getObject(monitor, schema, dbLinkName);
        }
    }

    public OracleObjectType getObjectType()
    {
        return OracleObjectType.getByType(objectTypeName);
    }

    @Property(name = "Name", viewable = true, editable = true, valueTransformer = JDBCObjectNameCaseTransformer.class, order = 1)
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
    public OracleSchema getObjectOwner()
    {
        return objectOwner;
    }

    @Property(name = "Object Reference", viewable = true, order = 4)
    public DBSObject getObject(DBRProgressMonitor monitor) throws DBException
    {
        OracleObjectType objectType = getObjectType();
        if (objectType == null) {
            log.warn("Unrecognized object type: " + objectTypeName);
            return null;
        }
        if (!objectType.isBrowsable()) {
            log.warn("Unsupported object type: " + objectTypeName);
            return null;
        }
        return objectType.findObject(monitor, objectOwner, objectName);
    }

    @Property(name = "DB Link", viewable = true, order = 4)
    public OracleDBLink getDbLink()
    {
        return dbLink;
    }

}
