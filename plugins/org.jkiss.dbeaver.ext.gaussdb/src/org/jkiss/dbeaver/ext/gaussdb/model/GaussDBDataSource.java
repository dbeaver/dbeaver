package org.jkiss.dbeaver.ext.gaussdb.model;

import java.lang.reflect.Field;
import java.sql.ResultSet;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreCharset;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreRole;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTablespace;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriverConfigurationType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

public class GaussDBDataSource extends PostgreDataSource {

    private static final Log log = Log.getLog(GaussDBDataSource.class);

    public GaussDBDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        super(monitor, container);
        initSqlDialect();
    }

    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.initialize(monitor);
    }

    @NotNull
    public GaussDBDatabase createDatabaseImpl(@NotNull DBRProgressMonitor monitor, ResultSet dbResult) throws DBException {
        return new GaussDBDatabase(monitor, this, dbResult);
    }

    @NotNull
    public GaussDBDatabase createDatabaseImpl(@NotNull DBRProgressMonitor monitor, String name) throws DBException {
        return new GaussDBDatabase(monitor, this, name);
    }

    @NotNull
    public GaussDBDatabase createDatabaseImpl(DBRProgressMonitor monitor,
                                              String name,
                                              PostgreRole owner,
                                              String templateName,
                                              PostgreTablespace tablespace,
                                              PostgreCharset encoding) throws DBException {
        return new GaussDBDatabase(monitor, this, name, owner, templateName, tablespace, encoding);
    }

    // True if we need multiple databases
    protected boolean isReadDatabaseList(DBPConnectionConfiguration configuration) {
        // It is configurable by default
        return configuration.getConfigurationType() != DBPDriverConfigurationType.URL
                    && CommonUtils.getBoolean(configuration.getProviderProperty(PostgreConstants.PROP_SHOW_NON_DEFAULT_DB), true);
    }

    private void initSqlDialect() {
        try {
            Class<?> forName = Class.forName("org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource");
            Field sqlDialect = forName.getDeclaredField("sqlDialect");
            sqlDialect.setAccessible(true);
            sqlDialect.set(this, new GaussDBDialect());
        } catch (ClassNotFoundException | NoSuchFieldException | SecurityException | IllegalArgumentException
                    | IllegalAccessException e) {
            log.debug("GaussDBDataSource initSqlDialect error : " + e.getMessage());
        }
    }
}