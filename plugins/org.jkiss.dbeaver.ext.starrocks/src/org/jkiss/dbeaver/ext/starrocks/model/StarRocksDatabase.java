package org.jkiss.dbeaver.ext.starrocks.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableBase;
import org.jkiss.dbeaver.ext.starrocks.StarRocksDataSource;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;
import java.util.Collection;

/**
 * StarRocks Database - wraps MySQLCatalog but belongs to a StarRocks Catalog
 */
public class StarRocksDatabase extends MySQLCatalog {
    
    private static final Log log = Log.getLog(StarRocksDatabase.class);
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
    
    /**
     * CRITICAL: This method must be called for DBeaver to attempt to load tables
     */
    @Override
    @Association
    public Collection<MySQLTableBase> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
        System.out.println("==========================================");
        System.out.println("StarRocksDatabase.getChildren() CALLED!");
        System.out.println("Database: " + getName());
        System.out.println("Catalog: " + catalog.getName());
        System.out.println("==========================================");
        
        log.debug("StarRocks: Loading tables for database '" + getName() + "' in catalog '" + catalog.getName() + "'");
        
        Collection<MySQLTableBase> tables = tableCache.getAllObjects(monitor, this);
        
        System.out.println("StarRocksDatabase.getChildren() RETURNED " + tables.size() + " tables");
        
        return tables;
    }
    
    /**
     * Helper method to switch catalog context
     */
    private void switchToCatalogContext(JDBCSession session) throws SQLException {
        String catalogName = catalog.getName();
        String useCatalogSQL = "SET CATALOG `" + catalogName + "`";
        
        System.out.println("Executing: " + useCatalogSQL);
        log.debug("StarRocks: Switching to catalog '" + catalogName + "' for database '" + getName() + "'");
        
        try {
            session.getOriginal().createStatement().execute(useCatalogSQL);
            System.out.println("Catalog switch successful");
            log.debug("StarRocks: Successfully switched to catalog '" + catalogName + "'");
        } catch (SQLException e) {
            System.err.println("Catalog switch FAILED: " + e.getMessage());
            log.error("StarRocks: Failed to switch to catalog '" + catalogName + "'", e);
            throw e;
        }
    }
    
    /**
     * Custom table cache that switches to the correct catalog before querying
     */
    class StarRocksTableCache extends TableCache {
        
        /**
         * Override prepareObjectsStatement for batch loading of all tables
         */
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(
                @NotNull JDBCSession session,
                @NotNull MySQLCatalog owner) throws SQLException {
            
            System.out.println("StarRocksTableCache.prepareObjectsStatement() CALLED");
            
            // Switch to correct catalog context
            switchToCatalogContext(session);
            
            // Call parent to generate SHOW FULL TABLES query
            JDBCStatement stmt = super.prepareObjectsStatement(session, owner);
            System.out.println("Statement prepared: " + stmt);
            return stmt;
        }
        
        /**
         * Override prepareLookupStatement for individual table lookups
         */
        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(
                @NotNull JDBCSession session,
                @NotNull MySQLCatalog owner,
                @Nullable MySQLTableBase object,
                @Nullable String objectName) throws SQLException {
            
            System.out.println("StarRocksTableCache.prepareLookupStatement() CALLED");
            System.out.println("  object: " + object);
            System.out.println("  objectName: " + objectName);
            
            // Switch to correct catalog context
            switchToCatalogContext(session);
            
            // Call parent to generate appropriate query
            JDBCStatement stmt = super.prepareLookupStatement(session, owner, object, objectName);
            System.out.println("Statement prepared: " + stmt);
            return stmt;
        }
        
        /**
         * Override prepareChildrenStatement to ensure catalog context when loading columns
         */
        @Override
        protected JDBCStatement prepareChildrenStatement(
                @NotNull JDBCSession session,
                @NotNull MySQLCatalog owner,
                @Nullable MySQLTableBase forTable) throws SQLException {
            
            System.out.println("StarRocksTableCache.prepareChildrenStatement() CALLED");
            
            // Switch to correct catalog context
            switchToCatalogContext(session);
            
            // Call parent to generate column query
            return super.prepareChildrenStatement(session, owner, forTable);
        }
        
        @Override
        protected void detectCaseSensitivity(org.jkiss.dbeaver.model.struct.DBSObject object) {
            this.setCaseSensitive(!getDataSource().getSQLDialect().useCaseInsensitiveNameLookup());
        }
    }
}