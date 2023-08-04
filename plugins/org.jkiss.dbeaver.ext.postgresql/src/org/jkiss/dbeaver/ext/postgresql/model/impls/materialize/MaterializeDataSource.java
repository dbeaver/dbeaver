package org.jkiss.dbeaver.ext.postgresql.model.impls.materialize;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.utils.CommonUtils;

public class MaterializeDataSource extends PostgreDataSource {

    public MaterializeDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        super(monitor, container);
    }

    @Override
    protected PreparedStatement prepareReadDatabaseListStatement(@NotNull DBRProgressMonitor monitor,
            @NotNull Connection bootstrapConnection, @NotNull DBPConnectionConfiguration configuration)
            throws SQLException {
        // Make initial connection to read database list
        DBSObjectFilter catalogFilters = getContainer().getObjectFilter(PostgreDatabase.class, null, false);
        StringBuilder catalogQuery = new StringBuilder("SELECT db.oid,db.* FROM pg_catalog.pg_database db WHERE 1 = 1");
        boolean addExclusionName = false;
        String connectionDBName = getContainer().getConnectionConfiguration().getDatabaseName();
        {
            final boolean showTemplates = CommonUtils
                    .toBoolean(configuration.getProviderProperty(PostgreConstants.PROP_SHOW_TEMPLATES_DB));
            // final boolean showUnavailable = CommonUtils
            // .toBoolean(configuration.getProviderProperty(PostgreConstants.PROP_SHOW_UNAVAILABLE_DB));

            // if (!showUnavailable) {
            // catalogQuery.append(" AND datallowconn");
            // }
            if (!showTemplates) {
                catalogQuery.append(" AND NOT datistemplate ");
                if (!CommonUtils.isEmpty(connectionDBName)) {
                    // User can add the name of template database in the Database field of
                    // connection settings. We must take it into account
                    catalogQuery.append("OR db.datname =?");
                    addExclusionName = true;
                }
            }
            if (catalogFilters != null) {
                JDBCUtils.appendFilterClause(catalogQuery, catalogFilters, "datname", false, this);
            }
            catalogQuery.append("\nORDER BY db.datname");
        }

        // Get all databases
        PreparedStatement dbStat = bootstrapConnection.prepareStatement(catalogQuery.toString());

        if (addExclusionName) {
            dbStat.setString(1, connectionDBName);
        }
        if (catalogFilters != null) {
            JDBCUtils.setFilterParameters(dbStat, addExclusionName ? 2 : 1, catalogFilters);
        }

        return dbStat;
    }
}
