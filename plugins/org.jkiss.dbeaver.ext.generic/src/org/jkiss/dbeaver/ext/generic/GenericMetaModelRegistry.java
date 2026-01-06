/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModelDescriptor;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.utils.ArrayUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GenericMetaModelRegistry {
    static final protected Log log = Log.getLog(GenericMetaModelRegistry.class);
    private final Map<String, GenericMetaModelDescriptor> metaModels = new HashMap<>();

    private static final String EXTENSION_ID = "org.jkiss.dbeaver.generic.meta";

    private static volatile GenericMetaModelRegistry instance = null;

    public synchronized static GenericMetaModelRegistry getInstance() {
        if (instance == null) {
            instance = new GenericMetaModelRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private GenericMetaModelRegistry(IExtensionRegistry extensionRegistry) {
        metaModels.put(GenericConstants.META_MODEL_STANDARD, new GenericMetaModelDescriptor());

        IConfigurationElement[] extElements = extensionRegistry.getConfigurationElementsFor(EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            GenericMetaModelDescriptor metaModel = new GenericMetaModelDescriptor(ext);
            metaModels.put(metaModel.getId(), metaModel);
            for (String replaces : metaModel.getModelReplacements()) {
                metaModels.put(replaces, metaModel);
            }
        }
        for (GenericMetaModelDescriptor metaModel : new ArrayList<>(metaModels.values())) {
            for (String driverClass : ArrayUtils.safeArray(metaModel.getDriverClass())) {
                metaModels.put(driverClass, metaModel);
            }
        }
    }

    public GenericMetaModelDescriptor getStandardMetaModel() {
        return metaModels.get(GenericConstants.META_MODEL_STANDARD);
    }

    public GenericMetaModel getMetaModel(DBPDataSourceContainer dataSourceContainer) throws DBException {
        GenericMetaModelDescriptor metaModel = null;
        Object metaModelId = dataSourceContainer.getDriver().getDriverParameter(GenericConstants.PARAM_META_MODEL);
        if (metaModelId != null && !GenericConstants.META_MODEL_STANDARD.equals(metaModelId)) {
            metaModel = metaModels.get(metaModelId.toString());
            if (metaModel == null) {
                log.warn("Meta model '" + metaModelId + "' not recognized. Default one will be used");
            }
        }
        if (metaModel == null) {
            // Try to get model by driver class
            metaModel = metaModels.get(dataSourceContainer.getDriver().getDriverClassName());
        }
        if (metaModel == null) {
            metaModel = getStandardMetaModel();
        }
        return metaModel.getInstance();
    }
}
