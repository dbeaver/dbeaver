package org.jkiss.dbeaver.ext.tidb.mysql.model;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceInfo;
import org.osgi.framework.Version;

public class TiDBMySQLDataSourceInfo extends JDBCDataSourceInfo {
    private static final Log log = Log.getLog(TiDBMySQLDataSourceInfo.class);
    
	private final TiDBMySQLDataSource dataSource;
	
    public TiDBMySQLDataSourceInfo(TiDBMySQLDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super(metaData);
        this.dataSource = dataSource;
    }

    @Override
    public boolean supportsMultipleResults() {
        return true;
    }

    @Override
    public boolean needsTableMetaForColumnResolution() {
        return true;
    }
    
    @Override
    public String getDatabaseProductVersion() {
    	return dataSource.getServerVersion();
    }
    
    @Override
    public Version getDatabaseVersion() {
    	// TiDB version string looks like: `5.7.25-TiDB-v6.1.0` or `5.7.25-TiDB-v6.3.0-serverless`
        // JDBC will return major version 5 and minor version 7
        // But the real TiDB version is v6.1.0
    	String tidbVersion = this.getDatabaseProductVersion();
        String[] tidbVersionArray = tidbVersion.split("-");
        if (tidbVersionArray.length < 3 || !tidbVersionArray[1].equals("TiDB")) {
            // It means not a TiDB server actually
            return new Version(0, 0, 0);
        }
        
        // drop first char "v"
        String realTiDBVersion = tidbVersionArray[2].substring(1);
        String[] realTiDBVersionArray = realTiDBVersion.split("\\.");
        if (realTiDBVersionArray.length != 3) {
            // It means not a TiDB server actually
            return new Version(0, 0, 0);
        }
        
        try {
            int major = Integer.parseInt(realTiDBVersionArray[0]);
            int minor = Integer.parseInt(realTiDBVersionArray[1]);
            int patch = Integer.parseInt(realTiDBVersionArray[2]);
            
            return new Version(major, minor, patch);
        } catch (NumberFormatException e){
        	log.error(e);
        }
        return new Version(0, 0, 0);
    }
}