
package org.jkiss.dbeaver.ext.gaussdb.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.lang.reflect.Field;

import org.jkiss.dbeaver.Log;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreCharset;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreRole;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTablespace;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectLookupCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class GaussDBDatabase extends PostgreDatabase {

    private static final Log   log = Log.getLog(GaussDBDatabase.class);

    private DBRProgressMonitor monitor;

    /**
     * Character Type
     */
    private String             characterType;

    /**
     * dataBase Compatibility Mode
     */
    private String             databaseCompatibleMode;

    private boolean            isPackageSupported;

    protected GaussDBDatabase(DBRProgressMonitor monitor,
                              GaussDBDataSource dataSource,
                              String name,
                              PostgreRole owner,
                              String templateName,
                              PostgreTablespace tablespace,
                              PostgreCharset encoding) throws DBException {
        super(monitor, dataSource, name, owner, templateName, tablespace, encoding);
        this.monitor = monitor;
    }

    protected GaussDBDatabase(DBRProgressMonitor monitor, GaussDBDataSource dataSource, String databaseName) throws DBException {
        super(monitor, dataSource, databaseName);
        this.monitor = monitor;
        readDatabaseInfo(monitor);
        checkInstanceConnection(monitor);
        checkPackageSupport(monitor);
    }

    protected GaussDBDatabase(DBRProgressMonitor monitor, GaussDBDataSource dataSource, ResultSet dbResult) throws DBException {
        super(monitor, dataSource, dbResult);
        this.monitor = monitor;
        init(dbResult);
        checkInstanceConnection(monitor);
        checkPackageSupport(monitor);
    }

    @NotNull
    @Override
    public GaussDBDataSource getDataSource() {
        return (GaussDBDataSource) dataSource;
    }

    @Override
    @Property(viewable = true, order = 1)
    public long getObjectId() {
        return super.getObjectId();
    }

    @Property(viewable = true, order = 6)
    public String getCharacterType() {
        return this.characterType;
    }

    @Property(viewable = true, order = 7)
    public String getDatabaseCompatibleMode() {
        return this.databaseCompatibleMode;
    }

    /**
     * is package supported
     * 
     * @return is package supported
     */
    public boolean isPackageSupported() {
        return isPackageSupported;
    }

    private void init(ResultSet dbResult) {
        this.databaseCompatibleMode = JDBCUtils.safeGetString(dbResult, "datcompatibility");
        this.characterType = JDBCUtils.safeGetString(dbResult, "datctype");
    }

    public void setDatabaseCompatibleMode(String databaseCompatibleMode) {
        this.databaseCompatibleMode = databaseCompatibleMode;
    }

    public DBRProgressMonitor getMonitor() {
        return monitor;
    }

    public void readDatabaseInfo(DBRProgressMonitor monitor) throws DBCException {
        try (JDBCSession session = getMetaContext().openSession(monitor, DBCExecutionPurpose.META, "Load database info")) {
            try (JDBCPreparedStatement dbStat = session
                        .prepareStatement("SELECT db.oid,db.* FROM pg_catalog.pg_database db WHERE datname=?")) {
                dbStat.setString(1, super.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.nextRow()) {
                        init(dbResult);
                    }
                }
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        }
    }

    public static class SchemaCache extends JDBCObjectLookupCache<PostgreDatabase, PostgreSchema> {
        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session,
                                                    @NotNull PostgreDatabase database,
                                                    @Nullable PostgreSchema object,
                                                    @Nullable String objectName) throws SQLException {
            StringBuilder catalogQuery = new StringBuilder("SELECT n.oid,n.*,d.description FROM pg_catalog.pg_namespace n\n"
                        + "LEFT OUTER JOIN pg_catalog.pg_description d ON d.objoid=n.oid AND d.objsubid=0 AND d.classoid='pg_namespace'::regclass\n");
            catalogQuery.append(" ORDER BY nspname");
            JDBCPreparedStatement dbStat = session.prepareStatement(catalogQuery.toString());
            return dbStat;
        }

        @Override
        protected PostgreSchema fetchObject(@NotNull JDBCSession session,
                                            @NotNull PostgreDatabase owner,
                                            @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            String name = JDBCUtils.safeGetString(resultSet, "nspname");
            if (name == null) {
                return null;
            }
            return owner.createSchemaImpl(owner, name, resultSet);
        }
    }

    @Association
    public Collection<PostgreSchema> getSysSchemas(DBRProgressMonitor monitor) throws DBException {
        checkInstanceConnection(monitor);
        // Get System schemas
        List<PostgreSchema> list = super.schemaCache.getAllObjects(monitor, this).stream()
                    .filter(e -> e.getObjectId() < 16384 && !e.getName().toLowerCase(Locale.ENGLISH).contains("public"))
                    .collect(Collectors.toList());
        return list;
    }

    @Association
    public Collection<PostgreSchema> getUserSchemas(DBRProgressMonitor monitor) throws DBException {
        checkInstanceConnection(monitor);
        // Get User schemas
        List<PostgreSchema> list = super.schemaCache.getAllObjects(monitor, this).stream()
                    .filter(e -> e.getObjectId() >= 16384 && !e.getName().contains("pg_")
                                || e.getName().toLowerCase(Locale.ENGLISH).contains("public"))
                    .collect(Collectors.toList());
        return list;
    }

    @Override
    public GaussDBSchema createSchemaImpl(@NotNull PostgreDatabase owner,
                                          @NotNull String name,
                                          @NotNull JDBCResultSet resultSet) throws SQLException {
        return new GaussDBSchema(owner, name, resultSet);
    }

    @Override
    public GaussDBSchema createSchemaImpl(@NotNull PostgreDatabase owner, @NotNull String name, @Nullable PostgreRole postgreRole) {
        return new GaussDBSchema(owner, name, postgreRole);
    }

    @Override
    protected void loadInfo(ResultSet dbResult) {
        super.loadInfo(dbResult);
        reflectInitDataBase(dbResult);
    }

    private void reflectInitDataBase(ResultSet unusedDbResult) {
        log.info("ReflectInitDataBase TODO");
    }

    /**
     * set package supported
     * 
     * @param isPackageSupported
     *            is package supported
     */
    public void setPackageSupported(boolean isPackageSupported) {
        this.isPackageSupported = isPackageSupported;
    }

    /**
     * check package is supported
     * 
     * @param monitor
     * @throws DBCException
     * @throws SQLException
     */
    public void checkPackageSupport(DBRProgressMonitor monitor) {
        try (JDBCSession session = getMetaContext().openSession(monitor, DBCExecutionPurpose.META, "Load database info")) {
            boolean isV5R2 = this.isV5R2(session);
            boolean isDistributed = this.isDistributedClusterV5(session);
            boolean isPackageSupport = isV5R2 && !isDistributed;
            if (isDistributed) {
                // 分布式校验Oracle兼容和版本号
                String compatibility = this.getDatabaseCompatibleMode();
                int versionNum = this.getWorkVersionNum(session);
                String version = this.getVersion(session);
                isPackageSupport = isV5R2 && isDistributed && "ORA".equalsIgnoreCase(compatibility)
                            && (versionNum == 93464 || version.contains("503.2.T55"));
            }
            setPackageSupported(isPackageSupport);
        } catch (DBCException | SQLException e) {
            log.info("checkPackageSupport Exception", e);
        }
    }

    private boolean isV5R2(JDBCSession session) throws DBCException {
        try (JDBCPreparedStatement dbStat = session.prepareStatement("select version();")) {
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                if (dbResult.nextRow()) {
                    String version = JDBCUtils.safeGetString(dbResult, "version");
                    if (version != null && (version.contains("Kernel") || version.contains("openGauss"))) {
                        int idxAfterKernel = version.indexOf("Kernel") + "Kernel ".length();
                        int idxAfterOpenGauss = version.indexOf("openGauss") + "openGauss ".length();
                        String kernelVersion = version.substring(idxAfterKernel, idxAfterKernel + "V500R002C00".length());
                        String openGaussVersion = version.substring(idxAfterOpenGauss, idxAfterOpenGauss + "2.0.0".length());
                        return kernelVersion.compareTo("V500R002C00") >= 0 || openGaussVersion.compareTo("2.0.0") >= 0;
                    }
                    return false;
                }
            }
        } catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        }
        return false;
    }

    /**
     * get version
     * 
     * @return the version
     * @throws DBCException
     */
    private String getVersion(JDBCSession session) throws DBCException {
        String version = "";
        try (JDBCPreparedStatement dbStat = session.prepareStatement("select version();")) {
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                if (dbResult.nextRow()) {
                    version = JDBCUtils.safeGetString(dbResult, "version");
                }
            }
        } catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        }
        return version;
    }

    /**
     * get version number.
     * 
     * @return version
     * @throws DBCException
     */
    public int getWorkVersionNum(JDBCSession session) throws DBCException {
        int version = 0;
        try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT working_version_num();")) {
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                if (dbResult.nextRow()) {
                    version = JDBCUtils.safeGetInt(dbResult, "working_version_num");
                }
            }
        } catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        }
        return version;

    }

    /**
     * is Distributed Or Centralized Support
     * 
     * return true if Distributed in v5 and olap connections
     * 
     * @param monitor
     * @throws DBCException
     * @throws SQLException
     */
    public boolean isDistributedClusterV5(JDBCSession session) throws DBCException, SQLException {
        try (JDBCPreparedStatement dbStat = session.prepareStatement("select count(*) from pgxc_node where node_type = 'C';")) {
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                if (dbResult.nextRow()) {
                    int count = JDBCUtils.safeGetInt(dbResult, "count");
                    return count > 0;
                }
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        }
        return false;
    }
}
