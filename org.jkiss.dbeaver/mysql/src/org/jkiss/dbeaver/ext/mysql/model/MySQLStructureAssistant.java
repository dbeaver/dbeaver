/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCStructureAssistant;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectType;

import java.sql.SQLException;
import java.util.List;

/**
 * MySQLStructureAssistant
 */
public class MySQLStructureAssistant extends JDBCStructureAssistant
{
    private final MySQLDataSource dataSource;

    public MySQLStructureAssistant(MySQLDataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    @Override
    protected JDBCDataSource getDataSource()
    {
        return dataSource;
    }

    public DBSObjectType[] getSupportedObjectTypes()
    {
        return new DBSObjectType[] {
            RelationalObjectType.TYPE_TABLE
            };
    }

    @Override
    protected void findObjectsByMask(JDBCExecutionContext context, DBSObjectType objectType, DBSObject parentObject, String objectNameMask, int maxResults, List<DBSObject> objects) throws DBException, SQLException
    {
        if (objectType == RelationalObjectType.TYPE_TABLE) {
            findTablesByMask(context, parentObject, objectNameMask, maxResults, objects);
        }
    }

    private void findTablesByMask(JDBCExecutionContext context, DBSObject parentObject, String tableNameMask, int maxResults, List<DBSObject> objects)
        throws SQLException, DBException
    {
        DBRProgressMonitor monitor = context.getProgressMonitor();
        MySQLCatalog catalog = parentObject instanceof MySQLCatalog ? (MySQLCatalog) parentObject : null;

        // Load tables
        JDBCPreparedStatement dbStat = context.prepareStatement(
            "SELECT * FROM " + MySQLConstants.META_TABLE_TABLES + " WHERE TABLE_NAME LIKE ? " +
                (catalog == null ? "" : " AND TABLE_SCHEMA=?") +
                " ORDER BY TABLE_NAME");
        try {
            dbStat.setString(1, tableNameMask.toLowerCase());
            if (catalog != null) {
                dbStat.setString(2, catalog.getName());
            }
            JDBCResultSet dbResult = dbStat.executeQuery();
            try {
                int tableNum = maxResults;
                while (dbResult.next() && tableNum-- > 0) {
                    String catalogName = JDBCUtils.safeGetString(dbResult, "TABLE_SCHEMA");
                    String tableName = JDBCUtils.safeGetString(dbResult, "TABLE_NAME");
                    MySQLCatalog tableCatalog = catalog != null ? catalog : dataSource.getCatalog(catalogName);
                    if (tableCatalog == null) {
                        log.debug("Table catalog '" + catalogName + "' not found");
                        continue;
                    }
                    MySQLTable table = tableCatalog.getTable(monitor, tableName);
                    if (table == null) {
                        log.debug("Table '" + catalogName + "' not found in catalog '" + tableCatalog.getName() + "'");
                        continue;
                    }
                    objects.add(table);
                }
            }
            finally {
                dbResult.close();
            }
        } finally {
            dbStat.close();
        }
    }

}
