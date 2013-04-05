/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.generic;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.utils.CommonUtils;

public class GenericDataSourceProvider extends JDBCDataSourceProvider {

    public GenericDataSourceProvider()
    {
    }

    @Override
    public long getFeatures()
    {
        return FEATURE_CATALOGS | FEATURE_SCHEMAS;
    }

    @Override
    public String getConnectionURL(DBPDriver driver, DBPConnectionInfo connectionInfo)
    {
        try {
            DriverDescriptor.MetaURL metaURL = DriverDescriptor.parseSampleURL(driver.getSampleURL());
            StringBuilder url = new StringBuilder();
            for (String component : metaURL.getUrlComponents()) {
                String newComponent = component;
                if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
                    newComponent = newComponent.replace(makePropPattern(DriverDescriptor.PROP_HOST), connectionInfo.getHostName());
                }
                if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
                    newComponent = newComponent.replace(makePropPattern(DriverDescriptor.PROP_PORT), connectionInfo.getHostPort());
                }
                if (!CommonUtils.isEmpty(connectionInfo.getServerName())) {
                    newComponent = newComponent.replace(makePropPattern(DriverDescriptor.PROP_SERVER), connectionInfo.getServerName());
                }
                if (!CommonUtils.isEmpty(connectionInfo.getDatabaseName())) {
                    newComponent = newComponent.replace(makePropPattern(DriverDescriptor.PROP_DATABASE), connectionInfo.getDatabaseName());
                    newComponent = newComponent.replace(makePropPattern(DriverDescriptor.PROP_FOLDER), connectionInfo.getDatabaseName());
                    newComponent = newComponent.replace(makePropPattern(DriverDescriptor.PROP_FILE), connectionInfo.getDatabaseName());
                }
                if (newComponent.startsWith("[")) { //$NON-NLS-1$
                    if (!newComponent.equals(component)) {
                        url.append(newComponent.substring(1, newComponent.length() - 1));
                    }
                } else {
                    url.append(newComponent);
                }
            }
            return url.toString();
        } catch (DBException e) {
            log.error(e);
            return null;
        }
    }

    @Override
    public DBPDataSource openDataSource(
        DBRProgressMonitor monitor,
        DBSDataSourceContainer container)
        throws DBException
    {
        return new GenericDataSource(monitor, container);
    }

    private static String makePropPattern(String prop)
    {
        return "{" + prop + "}"; //$NON-NLS-1$ //$NON-NLS-2$
    }
}
