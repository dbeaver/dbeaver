/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
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
        return new GenericDataSource(container);
    }

    private static String makePropPattern(String prop)
    {
        return "{" + prop + "}"; //$NON-NLS-1$ //$NON-NLS-2$
    }
}
