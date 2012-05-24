/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Abstract oracle schema object
 */
public abstract class OracleSchemaObject extends OracleObject<OracleSchema> implements DBPQualifiedObject
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

    @Override
    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getParentObject(),
            this);
    }

}
