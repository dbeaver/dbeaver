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
    private final String tableType;
    private final String tableDescription;

    public DBSTablePath(String catalogName, String schemaName, String tableName, String tableType, String tableDescription)
    {
        this.catalogName = catalogName;
        this.schemaName = schemaName;
        this.tableName = tableName;
        this.tableType = tableType;
        this.tableDescription = tableDescription;
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

    public String getTableType()
    {
        return tableType;
    }

    public String getTableDescription()
    {
        return tableDescription;
    }
}
