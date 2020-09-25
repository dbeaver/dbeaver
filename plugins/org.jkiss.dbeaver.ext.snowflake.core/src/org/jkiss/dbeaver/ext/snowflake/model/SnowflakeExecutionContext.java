package org.jkiss.dbeaver.ext.snowflake.model;

import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericExecutionContext;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.snowflake.SnowflakeConstants;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;

public class SnowflakeExecutionContext extends GenericExecutionContext {
    public SnowflakeExecutionContext(JDBCRemoteInstance instance, String purpose) {
        super(instance, purpose);
    }

    @Override
    public void setDefaultCatalog(DBRProgressMonitor monitor, GenericCatalog catalog, GenericSchema schema) throws DBCException {
        super.setDefaultCatalog(monitor, catalog, schema);
        final DBPConnectionConfiguration connectionConfiguration = getDataSource().getContainer().getConnectionConfiguration();
        final String databaseName = connectionConfiguration.getDatabaseName();
        if (schema == null && catalog != null && catalog.getName().equals(databaseName)) {
            useDefaultSchema(monitor);
        }
    }

    void useDefaultSchema(final DBRProgressMonitor monitor) throws DBCException {
        final String schemaName = getDefaultSchemaName();
        if (schemaName.isEmpty()) {
            return;
        }
        useSchemaWithName(monitor, schemaName);
    }

    private String getDefaultSchemaName() {
        return CommonUtils.notEmpty(getDataSource().getContainer().getConnectionConfiguration().getProviderProperty(SnowflakeConstants.PROP_SCHEMA));
    }

    private void useSchemaWithName(final DBRProgressMonitor monitor, final String schemaName) throws DBCException {
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, "Set active schema")) {
            try (JDBCStatement dbStat = session.createStatement()) {
                dbStat.executeUpdate("USE SCHEMA " + schemaName);
            }
        } catch (SQLException e) {
            throw new DBCException(e, this);
        }
    }
}
