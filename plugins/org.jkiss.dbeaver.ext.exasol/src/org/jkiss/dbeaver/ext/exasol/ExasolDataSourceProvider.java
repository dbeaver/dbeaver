/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.exasol;

import org.eclipse.core.runtime.Platform;
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
		// Default Port
		String port = ":8563";
		if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
			port = ":" + connectionInfo.getHostPort();
		}
		Map<String, String> properties = connectionInfo.getProperties();

		StringBuilder url = new StringBuilder(128);
		url.append("jdbc:exa:").append(connectionInfo.getHostName()).append(port);

		// check if we got an backup host list
		String backupHostList = connectionInfo.getProviderProperty(ExasolConstants.DRV_BACKUP_HOST_LIST);

		if (!CommonUtils.isEmpty(backupHostList))
			url.append(",").append(backupHostList).append(port);

		if (!url.toString().toUpperCase().contains("CLIENTNAME")) {
			// Client info can only be provided in the url with the exasol driver
			String clientName = Platform.getProduct().getName();

			Object propClientName = properties.get(ExasolConstants.DRV_CLIENT_NAME);
			if (propClientName != null)
				clientName = propClientName.toString();
			url.append(";clientname=").append(clientName);
		}

		if (!url.toString().toUpperCase().contains("CLIENTVERSION")) {
			String clientVersion = Platform.getProduct().getDefiningBundle().getVersion().toString();
			Object propClientName = properties.get(ExasolConstants.DRV_CLIENT_VERSION);
			if (propClientName != null)
				clientVersion = propClientName.toString();
			url.append(";clientversion=").append(clientVersion);
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
