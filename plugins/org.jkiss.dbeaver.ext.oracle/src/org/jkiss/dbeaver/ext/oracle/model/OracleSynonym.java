/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
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

    @NotNull
    @Override
    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    public String getName()
    {
        return super.getName();
    }

    @Property(viewable = true, order = 2)
    public String getObjectTypeName()
    {
        return objectTypeName;
    }

    @Property(viewable = true, order = 3)
    public Object getObjectOwner()
    {
        final OracleSchema schema = getDataSource().schemaCache.getCachedObject(objectOwner);
        return schema == null ? objectOwner : schema;
    }

    @Property(viewable = true, order = 4)
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

    @Property(viewable = true, order = 5)
    public Object getDbLink(DBRProgressMonitor monitor) throws DBException
    {
        return OracleDBLink.resolveObject(monitor, getSchema(), dbLink);
    }

}
