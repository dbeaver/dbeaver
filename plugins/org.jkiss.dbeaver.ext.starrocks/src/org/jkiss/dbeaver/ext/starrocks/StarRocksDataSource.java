package org.jkiss.dbeaver.ext.starrocks;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.ext.starrocks.model.StarRocksCatalog;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;
import java.util.Collection;

public class StarRocksDataSource extends MySQLDataSource {
    
    private final CatalogCache catalogCache = new CatalogCache();

    public StarRocksDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container) 
            throws DBException {
        super(monitor, container);
    }

    @Override
    public String getName() {
        return "StarRocks";
    }
    
    /**
     * Get all catalogs in this StarRocks instance
     */
    @Association
    public Collection<StarRocksCatalog> getCatalogs(DBRProgressMonitor monitor) throws DBException {
        return catalogCache.getAllObjects(monitor, this);
    }
    
    /**
     * Get a specific catalog by name
     */
    public StarRocksCatalog getCatalog(DBRProgressMonitor monitor, String name) throws DBException {
        return catalogCache.getObject(monitor, this, name);
    }
    
    /**
     * Cache for catalogs
     */
    class CatalogCache extends JDBCObjectCache<StarRocksDataSource, StarRocksCatalog> {
        @NotNull
        @Override
        protected JDBCPreparedStatement prepareObjectsStatement(
                @NotNull JDBCSession session,
                @NotNull StarRocksDataSource owner) throws SQLException {
            return session.prepareStatement("SHOW CATALOGS");
        }
        
        @NotNull
        @Override
        protected StarRocksCatalog fetchObject(
                @NotNull JDBCSession session,
                @NotNull StarRocksDataSource owner,
                @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new StarRocksCatalog(owner, resultSet);
        }
    }
}
