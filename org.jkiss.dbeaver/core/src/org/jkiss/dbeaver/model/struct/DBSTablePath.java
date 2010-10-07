/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSTablePath
 *
 * @author Serge Rieder
 */
public class DBSTablePath {

    private final String catalogName;
    private final String schemaName;
    private final String tableName;

    public DBSTablePath(String catalogName, String schemaName, String tableName)
    {
        this.catalogName = catalogName;
        this.schemaName = schemaName;
        this.tableName = tableName;
    }

    public String getCatalogName()
    {
        return catalogName;
    }

    public String getSchemaName()
    {
        return schemaName;
    }

    public String getTableName()
    {
        return tableName;
    }

}
