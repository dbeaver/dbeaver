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
package org.jkiss.dbeaver.registry.datatype;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.app.DBPRegistryDescriptor;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;

import java.lang.reflect.Field;
import java.util.*;

/**
 * DataTypeAbstractDescriptor
 */
public abstract class DataTypeAbstractDescriptor<DESCRIPTOR> extends AbstractDescriptor implements DBPRegistryDescriptor<DESCRIPTOR>
{
    private static final Log log = Log.getLog(ValueHandlerDescriptor.class);

    public static final String ALL_TYPES_PATTERN = "*";

    private final Class<DESCRIPTOR> instanceType;
    private final String id;
    private ObjectType implType;
    private Set<Object> supportedTypes = new HashSet<>();
    private List<String> supportedDataSources = new ArrayList<>();

    private boolean hasAll, hasTypeIds, hasDataKinds, hasTypeNames;

    protected DESCRIPTOR instance;

    public DataTypeAbstractDescriptor(IConfigurationElement config, Class<DESCRIPTOR> instanceType)
    {
        super(config);
        this.instanceType = instanceType;

        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.implType = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));

        IConfigurationElement[] typeElements = config.getChildren(RegistryConstants.TAG_TYPE);
        for (IConfigurationElement typeElement : typeElements) {
            String typeName = typeElement.getAttribute(RegistryConstants.ATTR_NAME);
            if (typeName != null) {
                if (typeName.equals(ALL_TYPES_PATTERN)) {
                    hasAll = true;
                } else {
                    supportedTypes.add(typeName.toLowerCase(Locale.ENGLISH));
                    hasTypeNames = true;
                }
            } else {
                typeName = typeElement.getAttribute("kind");
                if (typeName != null) {
                    try {
                        supportedTypes.add(DBPDataKind.valueOf(typeName));
                    } catch (IllegalArgumentException e) {
                        log.warn(e);
                    }
                    hasDataKinds = true;
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
                            hasTypeIds = true;
                        } catch (NumberFormatException e) {
                            log.warn("Type ID must be an integer while '" + typeName + "' was specified"); //$NON-NLS-1$
                        }
                    } else {
                        try {
                            Field typeField = java.sql.Types.class.getField(typeName);
                            int typeNumber = typeField.getInt(null);
                            supportedTypes.add(typeNumber);
                            hasTypeIds = true;
                        } catch (Exception e) {
                            log.warn("Standard type '" + typeName + "' cannot be accessed", e); //$NON-NLS-1$
                        }
                    }
                }
            }
        }

        IConfigurationElement[] dsElements = config.getChildren(RegistryConstants.TAG_DATASOURCE);
        for (IConfigurationElement dsElement : dsElements) {
            String dsId = dsElement.getAttribute(RegistryConstants.ATTR_ID);
            String dsClassName = dsElement.getAttribute(RegistryConstants.ATTR_CLASS);
            if (dsId == null && dsClassName == null) {
                log.warn("Datasource reference with null ID/Class"); //$NON-NLS-1$
                continue;
            }
            supportedDataSources.add(dsId != null ? dsId : dsClassName);
        }
    }

    @Override
    public String getId()
    {
        return id;
    }

    @Override
    public DESCRIPTOR getInstance()
    {
        if (instance == null && implType != null) {
            try {
                this.instance = implType.createInstance(instanceType);
            }
            catch (Exception e) {
                throw new IllegalStateException("Can't instantiate data type provider '" + this.id + "'", e); //$NON-NLS-1$
            }
        }
        return instance;
    }

    public boolean supportsType(@NotNull DBSTypedObject typedObject) {
        if (hasAll || (hasTypeIds && supportedTypes.contains(typedObject.getTypeID()))) {
            return true;
        }
        if (hasTypeNames) {
            String typeName = typedObject.getTypeName();
            if (typeName != null && supportedTypes.contains(typeName.toLowerCase(Locale.ENGLISH))) {
                return true;
            }
        }
        return hasDataKinds && supportedTypes.contains(typedObject.getDataKind());
    }

    public Set<Object> getSupportedTypes()
    {
        return supportedTypes;
    }

    public boolean isGlobal()
    {
        return supportedDataSources.isEmpty();
    }

    public boolean supportsDataSource(DBPDataSource dataSource, DataSourceProviderDescriptor descriptor)
    {
        return supportedDataSources.contains(descriptor.getId()) || supportedDataSources.contains(dataSource.getClass().getName());
    }

    @Override
    public String toString() {
        return getId();
    }
}