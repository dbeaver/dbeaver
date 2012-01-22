/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.DataTypeProviderDescriptor;

/**
 * Array holder
 */
public class JDBCArrayType implements DBSTypedObject {
    private String typeName;
    private int valueType;
    private DBDValueHandler valueHandler;

    public JDBCArrayType(String typeName, int valueType)
    {
        this.typeName = typeName;
        this.valueType = valueType;
    }

    public String getTypeName()
    {
        return typeName;
    }

    public int getTypeID()
    {
        return valueType;
    }

    public int getScale()
    {
        return 0;
    }

    public int getPrecision()
    {
        return 0;
    }

    @Override
    public long getMaxLength()
    {
        return 0;
    }

    public DBDValueHandler getValueHandler()
    {
        return valueHandler;
    }

    boolean resolveHandler(DBCExecutionContext context)
    {
        DataTypeProviderDescriptor typeProvider = DataSourceProviderRegistry.getDefault().getDataTypeProvider(context.getDataSource(), typeName, valueType);
        if (typeProvider != null) {
            valueHandler = typeProvider.getInstance().getHandler(context, typeName, valueType);
        }
        return valueHandler != null;
    }

}