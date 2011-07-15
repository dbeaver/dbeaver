/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;

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

    public List<DataExporterDescriptor> getDataExporters(Class objectType)
    {
        List<DataExporterDescriptor> editors = new ArrayList<DataExporterDescriptor>();
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
    public List<DataExporterDescriptor> getDataExporters(List<Class> objectTypes)
    {
        List<DataExporterDescriptor> editors = new ArrayList<DataExporterDescriptor>();
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

    public DataExporterDescriptor getDataExporter(String id)
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
