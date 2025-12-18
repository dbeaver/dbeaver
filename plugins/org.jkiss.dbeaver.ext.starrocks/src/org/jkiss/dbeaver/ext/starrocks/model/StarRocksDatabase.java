package org.jkiss.dbeaver.ext.starrocks.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableBase;
import org.jkiss.dbeaver.ext.mysql.model.MySQLView;
import org.jkiss.dbeaver.ext.starrocks.StarRocksDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;
import java.util.Collection;

/**
 * StarRocks Database - wraps MySQLCatalog but belongs to a StarRocks Catalog
 * Implements DBPQualifiedObject to provide catalog-aware fully qualified names
 */
public class StarRocksDatabase extends MySQLCatalog implements DBPQualifiedObject {
    
    private final StarRocksCatalog catalog;
    private final StarRocksTableCache tableCache = new StarRocksTableCache();
    
    public StarRocksDatabase(
            @NotNull StarRocksCatalog catalog,
            @NotNull JDBCResultSet resultSet) {
        super(catalog.getDataSource(), resultSet);
        this.catalog = catalog;
        tableCache.setCaseSensitive(!getDataSource().getSQLDialect().useCaseInsensitiveNameLookup());
    }
    
    @Override
    public StarRocksCatalog getParentObject() {
        return catalog;
    }
    
    public StarRocksCatalog getCatalog() {
        return catalog;
    }
    
    @Override
    public StarRocksDataSource getDataSource() {
        return (StarRocksDataSource) super.getDataSource();
    }
    
    @Override
    public TableCache getTableCache() {
        return tableCache;
    }
    
    @Override
    @Association
    public Collection<MySQLTableBase> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
        return tableCache.getAllObjects(monitor, this);
    }
    
    /**
     * Implement DBPQualifiedObject to provide catalog-aware fully qualified names
     * Format: catalog.database or `catalog`.`database`
     */
    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        switch (context) {
            case DML:
            case DDL:
                // For SQL contexts, include catalog name
                return DBUtils.getQuotedIdentifier(catalog) + "." + DBUtils.getQuotedIdentifier(this);
            default:
                // For UI contexts, just show database name
                return getName();
        }
    }
    
    /**
     * Helper method to switch catalog context
     */
    private void switchToCatalogContext(JDBCSession session) throws SQLException {
        String catalogName = catalog.getName();
        String useCatalogSQL = "SET CATALOG `" + catalogName + "`";
        
        try {
            session.getOriginal().createStatement().execute(useCatalogSQL);
        } catch (SQLException e) {
            throw e;
        }
    }
    
    /**
     * Custom table cache that switches to the correct catalog before querying
     * and creates StarRocksTable instances instead of MySQLTable
     */
    class StarRocksTableCache extends TableCache {
        
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(
                @NotNull JDBCSession session,
                @NotNull MySQLCatalog owner) throws SQLException {
            
            // Switch to correct catalog context
            switchToCatalogContext(session);
            
            // Call parent to generate SHOW FULL TABLES query
            return super.prepareObjectsStatement(session, owner);
        }
        
        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(
                @NotNull JDBCSession session,
                @NotNull MySQLCatalog owner,
                @Nullable MySQLTableBase object,
                @Nullable String objectName) throws SQLException {
            
            // Switch to correct catalog context
            switchToCatalogContext(session);
            
            // Call parent to generate appropriate query
            return super.prepareLookupStatement(session, owner, object, objectName);
        }
        
        @Override
        protected JDBCStatement prepareChildrenStatement(
                @NotNull JDBCSession session,
                @NotNull MySQLCatalog owner,
                @Nullable MySQLTableBase forTable) throws SQLException {
            
            // Switch to correct catalog context
            switchToCatalogContext(session);
            
            // Call parent to generate column query
            return super.prepareChildrenStatement(session, owner, forTable);
        }
        
        /**
         * Override to create StarRocksTable instead of MySQLTable
         */
        @Override
        protected MySQLTableBase fetchObject(
                @NotNull JDBCSession session, 
                @NotNull MySQLCatalog owner, 
                @NotNull JDBCResultSet dbResult)
                throws SQLException, DBException {
            
            final String tableType = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_TABLE_TYPE);
            if (tableType != null && tableType.contains("VIEW")) {
                return new MySQLView(owner, dbResult);
            } else {
                // Create StarRocksTable instead of MySQLTable
                return new StarRocksTable(owner, dbResult);
            }
        }
        
        @Override
        protected void detectCaseSensitivity(org.jkiss.dbeaver.model.struct.DBSObject object) {
            this.setCaseSensitive(!getDataSource().getSQLDialect().useCaseInsensitiveNameLookup());
        }
    }
}