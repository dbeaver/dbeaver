/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.model.data.DBDDataTypeProvider;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/**
 * EntityEditorDescriptor
 */
public class DataTypeProviderDescriptor extends AbstractDescriptor
{
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.dataTypeProvider";

    static final Log log = LogFactory.getLog(DataSourceRegistry.class);

    private DataSourceRegistry registry;
    private String id;
    private String className;
    private String name;
    private String description;
    private Set<Object> supportedTypes = new HashSet<Object>();
    private Set<DataSourceProviderDescriptor> supportedDataSources = new HashSet<DataSourceProviderDescriptor>();

    private DBDDataTypeProvider instance;

    public DataTypeProviderDescriptor(DataSourceRegistry registry, IConfigurationElement config)
    {
        super(config.getContributor());
        this.registry = registry;

        this.id = config.getAttribute("id");
        this.className = config.getAttribute("class");
        this.name = config.getAttribute("label");
        this.description = config.getAttribute("description");

        if (className == null) {
            log.error("Empty class name of data type provider '" + this.id + "'");
        } else {
            Class<?> providerClass = super.getObjectClass(className);
            if (providerClass == null) {
                log.error("Could not find datatype provider class '" + this.className + "'");
            } else {
                try {
                    this.instance = (DBDDataTypeProvider) providerClass.newInstance();
                }
                catch (Exception e) {
                    log.error("Can't instantiate data type provider '" + this.id + "'", e);
                }
            }
        }

        IConfigurationElement[] typeElements = config.getChildren("type");
        for (IConfigurationElement typeElement : typeElements) {
            String typeName = typeElement.getAttribute("name");
            if (typeName != null) {
                supportedTypes.add(typeName.toLowerCase());
            } else {
                typeName = typeElement.getAttribute("standard");
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

        IConfigurationElement[] dsElements = config.getChildren("datasource");
        for (IConfigurationElement dsElement : dsElements) {
            String dsId = dsElement.getAttribute("id");
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

    public DBDDataTypeProvider getInstance()
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