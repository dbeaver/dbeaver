package org.jkiss.dbeaver.ext.erd.model;

import org.jkiss.dbeaver.model.struct.DBSSchema;

/**
 * ERDTable
 */
public class ERDSchema extends ERDNode {

    private DBSSchema schema;

    public ERDSchema(DBSSchema schema)
    {
        super(schema);
        this.schema = schema;
    }

    public DBSSchema getSchema()
    {
        return schema;
    }

    public String getTipString()
    {
        return schema.getName();
    }

    @Override
    public String getId()
    {
        return schema.getName();
    }
}