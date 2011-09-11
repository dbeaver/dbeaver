/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityQualified;

/**
 * Abstract oracle schema object
 */
public abstract class OracleSchemaObject extends OracleObject<OracleSchema> implements DBSEntityQualified
{
    protected OracleSchemaObject(
        OracleSchema schema,
        String name,
        boolean persisted)
    {
        super(schema, name, persisted);
    }

    public OracleSchema getSchema()
    {
        return getParentObject();
    }

    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getParentObject(),
            this);
    }

    public boolean refreshEntity(DBRProgressMonitor monitor) throws DBException
    {
        return false;
    }


}
