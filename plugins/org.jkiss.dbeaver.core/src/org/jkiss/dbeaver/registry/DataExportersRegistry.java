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

package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporterDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * EntityEditorsRegistry
 */
public class DataExportersRegistry {

    private static final String CFG_EXPORT = "export"; //$NON-NLS-1$

    private List<DataExporterDescriptor> dataExporters = new ArrayList<DataExporterDescriptor>();

    public DataExportersRegistry(IExtensionRegistry registry)
    {
        // Load datasource providers from external plugins
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(DataExporterDescriptor.EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            if (CFG_EXPORT.equals(ext.getName())) {
                DataExporterDescriptor descriptor = new DataExporterDescriptor(ext);
                dataExporters.add(descriptor);
            }
        }

    }

    public List<IStreamDataExporterDescriptor> getDataExporters(Class objectType)
    {
        List<IStreamDataExporterDescriptor> editors = new ArrayList<IStreamDataExporterDescriptor>();
        for (DataExporterDescriptor descriptor : dataExporters) {
            if (descriptor.appliesToType(objectType)) {
                editors.add(descriptor);
            }
        }
        return editors;
    }

    /**
     * Returns data exporter which supports ALL specified object types
     * @param objectTypes object types
     * @return list of editors
     */
    public List<IStreamDataExporterDescriptor> getDataExporters(List<Class> objectTypes)
    {
        List<IStreamDataExporterDescriptor> editors = new ArrayList<IStreamDataExporterDescriptor>();
        for (DataExporterDescriptor descriptor : dataExporters) {
            boolean supports = true;
            for (Class objectType : objectTypes) {
                if (!descriptor.appliesToType(objectType)) {
                    supports = false;
                    break;
                }
            }
            if (supports) {
                editors.add(descriptor);
            }
        }
        return editors;
    }

    public IStreamDataExporterDescriptor getDataExporter(String id)
    {
        for (DataExporterDescriptor descriptor : dataExporters) {
            if (descriptor.getId().equals(id)) {
                return descriptor;
            }
        }
        return null;
    }

    public void dispose()
    {
        dataExporters.clear();
    }
}
