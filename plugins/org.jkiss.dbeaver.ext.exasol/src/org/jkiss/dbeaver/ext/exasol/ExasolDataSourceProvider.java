/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.exasol;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Exasol DataSource provider ddd
 */
public class ExasolDataSourceProvider extends JDBCDataSourceProvider {

    private static Map<String, String> connectionsProps = new HashMap<>();

    // ------------
    // Constructors
    // ------------

    public ExasolDataSourceProvider() {
    }

    public static Map<String, String> getConnectionsProps() {
        return connectionsProps;
    }

    @Override
    protected String getConnectionPropertyDefaultValue(String name, String value) {
        String ovrValue = connectionsProps.get(name);
        return ovrValue != null ? ovrValue : super.getConnectionPropertyDefaultValue(name, value);
    }

    @Override
    public long getFeatures() {
        return FEATURE_SCHEMAS;
    }

    @Override
    public String getConnectionURL(DBPDriver driver, DBPConnectionConfiguration connectionInfo) {
        //Default Port
        String port = ":8563";
        if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
            port = ":" + connectionInfo.getHostPort();
        }
        Map<String, String> properties = connectionInfo.getProperties();

        StringBuilder url = new StringBuilder(128);
        url.append("jdbc:exa:").append(connectionInfo.getHostName()).append(port);

        //check if we got an backup host list
        String backupHostList = connectionInfo.getProviderProperty(ExasolConstants.DRV_BACKUP_HOST_LIST);

        if (backupHostList != null)
            url.append(",").append(backupHostList).append(port);

        if (!url.toString().toUpperCase().contains("CLIENTNAME")) {
            String clientName = "DBeaver";

            Object propClientName = properties.get(ExasolConstants.DRV_CLIENT_NAME);
            if (propClientName != null)
                clientName = propClientName.toString();
            url.append(";clientname=").append(clientName);
        }
        Object querytimeout = properties.get(ExasolConstants.DRV_QUERYTIMEOUT);
        if (querytimeout != null)
            url.append(";").append(ExasolConstants.DRV_QUERYTIMEOUT).append("=").append(querytimeout);

        Object connecttimeout = properties.get(ExasolConstants.DRV_CONNECT_TIMEOUT);
        if (connecttimeout != null)
            url.append(";").append(ExasolConstants.DRV_CONNECT_TIMEOUT).append("=").append(connecttimeout);

        return url.toString();
    }

    @NotNull
    @Override
    public DBPDataSource openDataSource(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSourceContainer container)
        throws DBException {
        return new ExasolDataSource(monitor, container);
    }

}
