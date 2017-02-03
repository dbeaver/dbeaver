/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.generic;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModelDescriptor;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

public class GenericDataSourceProvider extends JDBCDataSourceProvider {

    private final Map<String, GenericMetaModelDescriptor> metaModels = new HashMap<>();
    private static final String EXTENSION_ID = "org.jkiss.dbeaver.generic.meta";

    public GenericDataSourceProvider()
    {
        metaModels.put(GenericConstants.META_MODEL_STANDARD, new GenericMetaModelDescriptor());
        IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
        IConfigurationElement[] extElements = extensionRegistry.getConfigurationElementsFor(EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            GenericMetaModelDescriptor metaModel = new GenericMetaModelDescriptor(ext);
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
    public String getConnectionURL(DBPDriver driver, DBPConnectionConfiguration connectionInfo)
    {
        try {
            String urlTemplate = driver.getSampleURL();
            if (CommonUtils.isEmptyTrimmed(urlTemplate)) {
                return connectionInfo.getUrl();
            }
            DriverDescriptor.MetaURL metaURL = DriverDescriptor.parseSampleURL(urlTemplate);
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
                newComponent = newComponent.replace(makePropPattern(DriverDescriptor.PROP_USER), CommonUtils.notEmpty(connectionInfo.getUserName()));
                newComponent = newComponent.replace(makePropPattern(DriverDescriptor.PROP_PASSWORD), CommonUtils.notEmpty(connectionInfo.getUserPassword()));

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
        @NotNull DBPDataSourceContainer container)
        throws DBException
    {
        GenericMetaModelDescriptor metaModel = null;
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
            metaModel = getStandardMetaModel();
        }
        GenericMetaModel metaModelInstance = metaModel.getInstance();
        return metaModelInstance.createDataSource(monitor, container);
    }

    protected GenericMetaModelDescriptor getStandardMetaModel() {
        return metaModels.get(GenericConstants.META_MODEL_STANDARD);
    }

    private static String makePropPattern(String prop)
    {
        return "{" + prop + "}"; //$NON-NLS-1$ //$NON-NLS-2$
    }

}
