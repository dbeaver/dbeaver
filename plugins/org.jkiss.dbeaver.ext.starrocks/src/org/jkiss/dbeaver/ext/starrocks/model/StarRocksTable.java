package org.jkiss.dbeaver.ext.starrocks.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTable;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * StarRocks Table - extends MySQLTable with catalog-aware fully qualified names
 */
public class StarRocksTable extends MySQLTable {
    
    public StarRocksTable(MySQLCatalog catalog) {
        super(catalog);
    }
    
    public StarRocksTable(MySQLCatalog catalog, JDBCResultSet dbResult) {
        super(catalog, dbResult);
    }
    
    /**
     * Get the StarRocks catalog (not to be confused with MySQL's catalog which is actually a database)
     */
    private StarRocksCatalog getStarRocksCatalog() {
        MySQLCatalog container = getContainer();
        if (container instanceof StarRocksDatabase) {
            return ((StarRocksDatabase) container).getCatalog();
        }
        return null;
    }
    
    /**
     * Override to provide catalog-aware fully qualified names
     * Format: catalog.database.table or `catalog`.`database`.`table`
     * 
     * MySQLTable already implements DBPQualifiedObject and has getFullyQualifiedName(),
     * so we're overriding it here.
     */
    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        StarRocksCatalog srCatalog = getStarRocksCatalog();
        
        switch (context) {
            case DML:
            case DDL:
                // For SQL contexts, include catalog.database.table
                if (srCatalog != null) {
                    return DBUtils.getQuotedIdentifier(srCatalog) + "." + 
                           DBUtils.getQuotedIdentifier(getContainer()) + "." + 
                           DBUtils.getQuotedIdentifier(this);
                }
                // Fall through to default if no StarRocks catalog found
            default:
                // Use parent's implementation for other contexts or if no catalog
                return super.getFullyQualifiedName(context);
        }
    }
}