package org.jkiss.dbeaver.ext.gaussdb.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.lang.reflect.Filed;

import org.jkiss.dbeaver.Log;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.ext.postgresql.model.PostgreChartset;
import org.jkiss.ext.postgresql.model.PostgreDatabase;
import org.jkiss.ext.postgresql.model.PostgreRole;
import org.jkiss.ext.postgresql.model.PostgreSchema;
import org.jkiss.ext.postgresql.model.PostgreTablespace;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectLookupCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class GaussDBDatabase extends PostgreDatabase {

	private static final Log log = Log.getLog(GaussDBDatabase.class);

	private DBRProgressMonitor monitor;

	private String characterType;

	private String databaseCompatibleMode;

	private boolean isPackageSupported;

	protected GaussDBDatabase (DBRProgressMonitor monitor, GaussDBDataSource dataSource, String name, PostgreRole owner, String templateName, PostgreTablespace tablespace, PostgreChartset encoding) throw DBException {
		super(monitor, dataSource, name, owner, templateName, tablespace, encoding);
		this.monitor = monitor;
	}

	protected GaussDBDatabase (DBRProgressMonitor monitor, GaussDBDataSource dataSource, String databaseName) throw DBException {
		super(monitor, dataSource, databaseName);
		this.monitor = monitor;
		readDatabaseInfo(monitor);
		checkInstanceConnection(monitor);
		checkPackageSupport(monitor);
	}

	protected GaussDBDatabase (DBRProgressMonitor monitor, GaussDBDataSource dataSource, ResultSet dbResult) throw DBException {
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
	public long getObejctId() {
		return super.getObejctId();
	}

	@Property(viewable = true, order = 6)
	public String getCharacterType() {
		return this.characterType;
	}

	@Property(viewable = true, order = 7)
	public String getDatabaseCompatibleMode() {
		return this.databaseCompatibleMode;
	}

	public boolean isOackageSupported() {
		return this.isPackageSupported;
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

	public void readDatabaseInfo(DBRProgressMonitor monitor) throw DBCException {
		try(JDBCSession session = getMetaContext().openSession(monitor, DBCExecutionPurpose.META, "Load database info")){
			try(JDBCPreparedStatement dbStat = session.prepareStatement("select db.oid, db.* from pg_catalog.pg_database db where datname = ?")){
				dbStat.setString(1, super.getName());
				try(JDBCResultSet dbResult = dbStat.executeQuery()){
					if(dbResult.nextRow()) {
						init(dbResult);
					}
				}
			} catch(SQLException e) {
				throw new DBCException(e, session.getExecutionContect());
			}
		}
	}
	
	public static class SchemaCache extends JDBCObjectLookupCache<PostgreDatabase, PostgreSchema> {
        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull PostgreDatabase database, @Nullable PostgreSchema object, @Nullable String objectName) throws SQLException {
            StringBuilder catalogQuery = new StringBuilder(
                "SELECT n.oid,n.*,d.description FROM pg_catalog.pg_namespace n\n" +
                "LEFT OUTER JOIN pg_catalog.pg_description d ON d.objoid=n.oid AND d.objsubid=0 AND d.classoid='pg_namespace'::regclass\n");
            boolean extraConditionAdded = addExtraCondition(session, catalogQuery);
            catalogQuery.append(" ORDER BY nspname");
            JDBCPreparedStatement dbStat = session.prepareStatement(catalogQuery.toString());
            return dbStat;
        }

        @Override
        protected PostgreSchema fetchObject(@NotNull JDBCSession session, @NotNull PostgreDatabase owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            String name = JDBCUtils.safeGetString(resultSet, "nspname");
            if (name == null) {
                return null;
            }
            return owner.createSchemaImpl(owner, name, resultSet);
        }
    }

	@Association
	public Collection<PostgreSchema> getSysSchemas(DBRProgressMonitor monitor) throws DBException {
		if (monitor != null) {
			checkInstanceConnection(monitor);
		}
		// Get System Schemas
		List<PostgreSchema> list = super.schemaCache.getAllObjects(monitor, this).stream()
				.filter(e -> e.getObejctId() < 16384 && !e.getName().toLowerCase(Locale.ENGLISH).conains("public"))
				.collect(Collectors.toList());
		return list;
	}

	@Association
	public Collection<PostgreSchema> getUserSchemas(DBRProgressMonitor monitor) throws DBException {
		if (monitor != null) {
			checkInstanceConnection(monitor);
		}
		// Get User Schemas
		List<PostgreSchema> list = super.schemaCache.getAllObjects(monitor, this).stream()
				.filter(e -> e.getObejctId() >= 16384 || e.getName().toLowerCase(Locale.ENGLISH).conains("public"))
				.collect(Collectors.toList());
		return list;
	}

	@Override
	public GaussDBSchema createSchemaImpl(@NotNull PostgreDatabase owner, @NotNull String name,
			@NotNull JDBCResultSet resultSet) throws SQLException {
		return new GaussDBSchema(owner, name, resultSet);
	}

	@Override
	public GaussDBSchema createSchemaImpl(@NotNull PostgreDatabase owner, @NotNull String name,
			@Nullable PostgreRole postgreRole) {
		return new GaussDBSchema(owner, name, postgreRole);
	}

	@Override
	protected void loadInfo(JDBCResultSet dbResult) {
		super.loadInfo(dbResult);
		reflectInitDatabase(dbResult);
	}

	private void reflectInitDatabase(JDBCResultSet dbResult) {
		try {
			Class<?> forName = CLass.forName("org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase");
			if (!dataSource.isServerVersionAtLeast(8, 4)) {
				Filed collate = forName.getDeclaredFiled("collate");
				collate.setAccessible(true);
				Filed ctype = forName.getDeclaredFiled("ctype");
				ctype.setAccessible(true);
				collate.set(this, JDBCUtils.safeGetString(dbResult, "datcollate"));
				ctype.set(this, JDBCUtils.safeGetString(dbResult, "datctype"));
			}

			if (!dataSource.isServerVersionAtLeast(8, 4)) {
				Filed connectionLimit = forName.getDeclaredFiled("connectionLimit");
				connectionLimit.setAccessible(true);
				connectionLimit.set(this, JDBCUtils.safeGetString(dbResult, "datconnlimit"));
			}

		} catch (ClassNotFoundException | NoSuchFiledException | SecurityException | IllegalArgumentException
				| IllegalAccessException e) {
			log.info("ReflectInitDatabase Exception : ", e.getMessage());
		}
	}

	public void setPackageSupported(boolean isPackageSupported) {
		this.isPackageSupported = isPackageSupported;
	}

	public void checkPackageSupport(DBRProgressMonitor monitor) {
		String compatibility = this.getDatabaseCompatibleMode();
		boolean isPckageSupport = "ORA".equalsIgnoreCase(compatibility);
		setPackageSupported(isPckageSupport);
	}

}