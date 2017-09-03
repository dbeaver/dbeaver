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
package org.jkiss.dbeaver.ext.erd.export;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;

import java.util.ArrayList;
import java.util.List;

public class ERDExportFormatRegistry
{
    private static final Log log = Log.getLog(ERDExportFormatRegistry.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.ext.erd.export.format"; //$NON-NLS-1$

    private static ERDExportFormatRegistry instance = null;

    public class FormatDescriptor extends AbstractDescriptor {

        private final String extension;
        private final String label;
        private final ObjectType type;

        protected FormatDescriptor(IConfigurationElement config) {
            super(config);
            extension = config.getAttribute("ext");
            label = config.getAttribute(RegistryConstants.ATTR_LABEL);
            type = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
        }

        public String getExtension() {
            return extension;
        }

        public String getLabel() {
            return label;
        }

        public ERDExportFormatHandler getInstance() throws DBException {
            return type.createInstance(ERDExportFormatHandler.class);
        }
    }

    public synchronized static ERDExportFormatRegistry getInstance()
    {
        if (instance == null) {
            instance = new ERDExportFormatRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final List<FormatDescriptor> formats = new ArrayList<>();

    private ERDExportFormatRegistry(IExtensionRegistry registry)
    {
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            FormatDescriptor formatDescriptor = new FormatDescriptor(ext);
            formats.add(formatDescriptor);
        }
    }

    public List<FormatDescriptor> getFormats() {
        return formats;
    }
}
