/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDAttributeTransformer;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * DataTypeTransformerDescriptor
 */
public class DataTypeTransformerDescriptor extends AbstractDescriptor
{
    static final Log log = Log.getLog(DataTypeTransformerDescriptor.class);

    private String id;
    private ObjectType implType;
    private Set<Object> supportedTypes = new HashSet<>();
    private Set<DataSourceProviderDescriptor> supportedDataSources = new HashSet<>();

    private DBDAttributeTransformer instance;

    public DataTypeTransformerDescriptor(IConfigurationElement config)
    {
        super(config);

        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.implType = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));

        IConfigurationElement[] typeElements = config.getChildren(RegistryConstants.TAG_TYPE);
        for (IConfigurationElement typeElement : typeElements) {
            String typeName = typeElement.getAttribute(RegistryConstants.ATTR_NAME);
            if (typeName != null) {
                supportedTypes.add(typeName.toLowerCase(Locale.ENGLISH));
            } else {
                typeName = typeElement.getAttribute(RegistryConstants.ATTR_STANDARD);
                if (typeName == null) {
                    typeName = typeElement.getAttribute(RegistryConstants.ATTR_ID);
                    if (typeName == null) {
                        log.warn("Type element without name or standard type reference"); //$NON-NLS-1$
                        continue;
                    }
                    try {
                        int typeNumber = Integer.parseInt(typeName);
                        supportedTypes.add(typeNumber);
                    } catch (NumberFormatException e) {
                        log.warn("Type ID must be an integer while '" + typeName + "' was specified"); //$NON-NLS-1$
                    }
                } else {
                    try {
                        Field typeField = java.sql.Types.class.getField(typeName);
                        int typeNumber = typeField.getInt(null);
                        supportedTypes.add(typeNumber);
                    } catch (NoSuchFieldException e) {
                        log.warn("Standard type '" + typeName + "' not found in " + java.sql.Types.class.getName(), e); //$NON-NLS-1$
                    } catch (IllegalAccessException e) {
                        log.warn("Standard type '" + typeName + "' cannot be accessed", e); //$NON-NLS-1$
                    }
                }
            }
        }

        IConfigurationElement[] dsElements = config.getChildren(RegistryConstants.TAG_DATASOURCE);
        for (IConfigurationElement dsElement : dsElements) {
            String dsId = dsElement.getAttribute(RegistryConstants.ATTR_ID);
            if (dsId == null) {
                log.warn("Datasource reference with null ID"); //$NON-NLS-1$
                continue;
            }
            DataSourceProviderDescriptor dsProvider = DataSourceProviderRegistry.getInstance().getDataSourceProvider(dsId);
            if (dsProvider == null) {
                log.warn("Datasource provider '" + dsId + "' not found. Bad data type mapping."); //$NON-NLS-1$
                continue;
            }
            supportedDataSources.add(dsProvider);
        }
    }

    public String getId()
    {
        return id;
    }

    public DBDAttributeTransformer getInstance()
    {
        if (instance == null && implType != null) {
            try {
                this.instance = implType.createInstance(DBDAttributeTransformer.class);
            }
            catch (Exception e) {
                log.error("Can't instantiate attribute transformer '" + this.id + "'", e); //$NON-NLS-1$
            }
        }
        return instance;
    }

    public boolean supportsType(DBSTypedObject typedObject)
    {
        String typeName = typedObject.getTypeName();
        return
            supportedTypes.contains(typedObject.getTypeID()) ||
            (typeName != null && supportedTypes.contains(typeName.toLowerCase(Locale.ENGLISH))) ||
            supportedTypes.contains(typedObject.getDataKind());
    }

    public Set<Object> getSupportedTypes()
    {
        return supportedTypes;
    }

    public boolean isDefault()
    {
        return supportedDataSources.isEmpty();
    }

    public boolean supportsDataSource(DataSourceProviderDescriptor descriptor)
    {
        return supportedDataSources.contains(descriptor);
    }

    public Set<DataSourceProviderDescriptor> getSupportedDataSources()
    {
        return supportedDataSources;
    }

}