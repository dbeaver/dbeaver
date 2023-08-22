package org.jkiss.dbeaver.ext.dm.model;

import java.sql.SQLException;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.dm.model.utils.DmUtils;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionBootstrap;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCFeatureNotSupportedException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.utils.CommonUtils;

public class DmExecutionContext extends JDBCExecutionContext
		implements DBCExecutionContextDefaults<DBSCatalog, DmSchema> {

	private static final Log log = Log.getLog(DmExecutionContext.class);

	private String activeSchemaName;

	DmExecutionContext(@NotNull JDBCRemoteInstance instance, String purpose) {
		super(instance, purpose);
	}

	@NotNull
	@Override
	public DmDataSource getDataSource() {
		return (DmDataSource) super.getDataSource();
	}

	@NotNull
	@Override
	public DmExecutionContext getContextDefaults() {
		return this;
	}

	public String getActiveSchemaName() {
		return activeSchemaName;
	}

	@Override
	public DBSCatalog getDefaultCatalog() {
		return null;
	}

	@Override
	public DmSchema getDefaultSchema() {
		return activeSchemaName == null ? null : getDataSource().schemaCache.getCachedObject(activeSchemaName);
	}

	@Override
	public boolean supportsCatalogChange() {
		return false;
	}

	@Override
	public boolean supportsSchemaChange() {
		return true;
	}

	@Override
	public void setDefaultCatalog(DBRProgressMonitor monitor, DBSCatalog catalog, DmSchema schema) throws DBCException {
		throw new DBCFeatureNotSupportedException();
	}

	@Override // 设置默认模式
	public void setDefaultSchema(DBRProgressMonitor monitor, DmSchema schema) throws DBCException {
		final DmSchema oldSelectedEntity = getDefaultSchema();
		if (schema == null || oldSelectedEntity == schema) {
			return;
		}
		setCurrentSchema(monitor, schema);
		activeSchemaName = schema.getName();
		DBUtils.fireObjectSelectionChange(oldSelectedEntity, schema);
	}

	@Override
	public boolean refreshDefaults(DBRProgressMonitor monitor, boolean useBootstrapSettings) throws DBException {
		try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.META, "Query active schema")) {
			if (useBootstrapSettings) {
				DBPConnectionBootstrap bootstrap = getBootstrapSettings();
				if (!CommonUtils.isEmpty(bootstrap.getDefaultSchemaName())) {
					setCurrentSchema(monitor, bootstrap.getDefaultSchemaName());
				}
			}
			this.activeSchemaName = DmUtils.getCurrentSchema(session);
			if (this.activeSchemaName != null) {
				if (this.activeSchemaName.isEmpty()) {
					this.activeSchemaName = null;
				}
			}
		} catch (Exception e) {
			throw new DBCException(e, this);
		}

		return true;
	}

	void setCurrentSchema(DBRProgressMonitor monitor, DmSchema object) throws DBCException {
		if (object == null) {
			log.debug("Null current schema");
			return;
		}
		setCurrentSchema(monitor, object.getName());
	}

	private void setCurrentSchema(DBRProgressMonitor monitor, String activeSchemaName) throws DBCException {
		try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, "Set active schema")) {
			DmUtils.setCurrentSchema(session, activeSchemaName);
			this.activeSchemaName = activeSchemaName;
		} catch (SQLException e) {
			throw new DBCException(e, this);
		}
	}
}
