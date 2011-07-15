/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/**
 * EntityEditorDescriptor
 */
public class DataTypeProviderDescriptor extends AbstractDescriptor
{
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.dataTypeProvider"; //$NON-NLS-1$

    static final Log log = LogFactory.getLog(DataSourceProviderRegistry.class);

    private String id;
    private String className;
    private String name;
    private String description;
    private Set<Object> supportedTypes = new HashSet<Object>();
    private Set<DataSourceProviderDescriptor> supportedDataSources = new HashSet<DataSourceProviderDescriptor>();

    private DBDValueHandlerProvider instance;

    public DataTypeProviderDescriptor(DataSourceProviderRegistry registry, IConfigurationElement config)
    {
        super(config.getContributor());

        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.className = config.getAttribute(RegistryConstants.ATTR_CLASS);
        this.name = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);

        if (className == null) {
            log.error("Empty class name of data type provider '" + this.id + "'");
        } else {
            Class<?> providerClass = super.getObjectClass(className);
            if (providerClass == null) {
                log.error("Could not find data type provider class '" + this.className + "'");
            } else {
                try {
                    this.instance = (DBDValueHandlerProvider) providerClass.newInstance();
                }
                catch (Exception e) {
                    log.error("Can't instantiate data type provider '" + this.id + "'", e);
                }
            }
        }

        IConfigurationElement[] typeElements = config.getChildren(RegistryConstants.TAG_TYPE);
        for (IConfigurationElement typeElement : typeElements) {
            String typeName = typeElement.getAttribute(RegistryConstants.ATTR_NAME);
            if (typeName != null) {
                supportedTypes.add(typeName.toLowerCase());
            } else {
                typeName = typeElement.getAttribute(RegistryConstants.ATTR_STANDARD);
                if (typeName == null) {
                    log.warn("Type element without name or standard type reference");
                    continue;
                }
                try {
                    Field typeField = java.sql.Types.class.getField(typeName);
                    int typeNumber = typeField.getInt(null);
                    supportedTypes.add(typeNumber);
                }
                catch (NoSuchFieldException e) {
                    log.warn("Standard type '" + typeName + "' not found in " + java.sql.Types.class.getName(), e);
                }
                catch (IllegalAccessException e) {
                    log.warn("Standard type '" + typeName + "' cannot be accessed", e);
                }
            }
        }

        IConfigurationElement[] dsElements = config.getChildren(RegistryConstants.TAG_DATASOURCE);
        for (IConfigurationElement dsElement : dsElements) {
            String dsId = dsElement.getAttribute(RegistryConstants.ATTR_ID);
            if (dsId == null) {
                log.warn("Datasource reference with null ID");
                continue;
            }
            DataSourceProviderDescriptor dsProvider = registry.getDataSourceProvider(dsId);
            if (dsProvider == null) {
                log.warn("Datasource provider '" + dsId + "' not found");
                continue;
            }
            supportedDataSources.add(dsProvider);
        }
    }

    public String getId()
    {
        return id;
    }

    public String getClassName()
    {
        return className;
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public DBDValueHandlerProvider getInstance()
    {
        return instance;
    }

    public boolean supportsType(DBSTypedObject type)
    {
        return supportedTypes.contains(type.getValueType()) || supportedTypes.contains(type.getTypeName().toLowerCase());
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