/*
 * Copyright (C) 2010-2015 Serge Rieder
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

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.registry.AbstractDescriptor;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

public class GenericDataSourceProvider extends JDBCDataSourceProvider {

    private final Map<String, GenericMetaModel> metaModels = new HashMap<String, GenericMetaModel>();
    private static final String EXTENSION_ID = "org.jkiss.dbeaver.generic.meta";

    public GenericDataSourceProvider()
    {
        metaModels.put(GenericConstants.META_MODEL_STANDARD, new GenericMetaModel(GenericConstants.META_MODEL_STANDARD));
        IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
        IConfigurationElement[] extElements = extensionRegistry.getConfigurationElementsFor(EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            String metaClass = ext.getAttribute("class");
            GenericMetaModel metaModel;
            if (!CommonUtils.isEmpty(metaClass)) {
                try {
                    Class<? extends GenericMetaModel> metaClassImpl = AbstractDescriptor.getObjectClass(
                        Platform.getBundle(ext.getContributor().getName()),
                        metaClass,
                        GenericMetaModel.class);
                    if (metaClassImpl != null) {
                        metaModel = metaClassImpl
                            .getConstructor(IConfigurationElement.class)
                            .newInstance(ext);
                    } else {
                        log.warn("Generic meta model implementation '" + metaClass + "' not found");
                        continue;
                    }
                } catch (Exception e) {
                    log.error(e);
                    continue;
                }
            } else {
                //for (IConfigurationElement metaChild : ext.getChildren("meta")) {
                metaModel = new GenericMetaModel(ext);
            }
            metaModels.put(metaModel.getId(), metaModel);
            for (String driverClass : metaModel.getDriverClass()) {
                metaModels.put(driverClass, metaModel);
            }
        }
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

    @NotNull
    @Override
    public DBPDataSource openDataSource(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSDataSourceContainer container)
        throws DBException
    {
        GenericMetaModel metaModel = null;
        Object metaModelId = container.getDriver().getDriverParameter(GenericConstants.PARAM_META_MODEL);
        if (metaModelId != null && !GenericConstants.META_MODEL_STANDARD.equals(metaModelId)) {
            metaModel = metaModels.get(metaModelId.toString());
            if (metaModel == null) {
                log.warn("Meta model '" + metaModelId + "' not recognized. Default one will be used");
            }
        }
        if (metaModel == null) {
            // Try to get model by driver class
            metaModel = metaModels.get(container.getDriver().getDriverClassName());
        }
        if (metaModel == null) {
            metaModel = metaModels.get(GenericConstants.META_MODEL_STANDARD);
        }
        return new GenericDataSource(monitor, container, metaModel);
    }

    private static String makePropPattern(String prop)
    {
        return "{" + prop + "}"; //$NON-NLS-1$ //$NON-NLS-2$
    }

}
