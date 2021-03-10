/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.data.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderDescriptor;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.data.IValueManager;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * ValueManagerDescriptor
 */
public class ValueManagerDescriptor extends AbstractDescriptor
{
    private static final Log log = Log.getLog(ValueManagerDescriptor.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.dataManager"; //$NON-NLS-1$
    public static final String TAG_MANAGER = "manager"; //$NON-NLS-1$
    public static final String TAG_SUPPORTS = "supports"; //$NON-NLS-1$
    private static final String ATTR_KIND = "kind";
    private static final String ATTR_TYPE = "type";
    private static final String ATTR_TYPE_NAME = "typeName";
    private static final String ATTR_DATA_SOURCE = "dataSource";
    private static final String ATTR_EXTENSION = "extension";

    private String id;
    private ObjectType implType;
    private final List<SupportInfo> supportInfos = new ArrayList<>();

    private static class SupportInfo {
        String typeName;
        DBPDataKind dataKind;
        ObjectType valueType;
        String extension;
        String dataSource;
    }

    private IValueManager instance;

    public ValueManagerDescriptor(IConfigurationElement config)
    {
        super(config);

        this.id = config.getAttribute("id");
        this.implType = new ObjectType(config.getAttribute("class"));

        IConfigurationElement[] typeElements = config.getChildren(TAG_SUPPORTS);
        for (IConfigurationElement typeElement : typeElements) {
            String kindName = typeElement.getAttribute(ATTR_KIND);
            String typeName = typeElement.getAttribute(ATTR_TYPE_NAME);
            String className = typeElement.getAttribute(ATTR_TYPE);
            String ext = typeElement.getAttribute(ATTR_EXTENSION);
            String dspId = typeElement.getAttribute(ATTR_DATA_SOURCE);
            if (!CommonUtils.isEmpty(kindName) || !CommonUtils.isEmpty(typeName) || !CommonUtils.isEmpty(className) || !CommonUtils.isEmpty(dspId) || !CommonUtils.isEmpty(ext)) {
                SupportInfo info = new SupportInfo();
                if (!CommonUtils.isEmpty(kindName)) {
                    try {
                        info.dataKind = DBPDataKind.valueOf(kindName);
                    } catch (IllegalArgumentException e) {
                        log.warn("Bad data kind: " + kindName);
                    }
                }
                if (!CommonUtils.isEmpty(typeName)) {
                    info.typeName = typeName;
                }
                if (!CommonUtils.isEmpty(className)) {
                    info.valueType = new ObjectType(typeElement, ATTR_TYPE);
                }
                if (!CommonUtils.isEmpty(ext)) {
                    info.extension = ext;
                }
                if (!CommonUtils.isEmpty(dspId)) {
                    info.dataSource = dspId;
                }
                supportInfos.add(info);
            }
        }
    }

    public String getId()
    {
        return id;
    }

    @NotNull
    public IValueManager getInstance()
    {
        if (instance == null) {
            try {
                this.instance = implType.createInstance(IValueManager.class);
            }
            catch (Exception e) {
                throw new IllegalStateException("Can't instantiate value manager '" + this.id + "'", e); //$NON-NLS-1$
            }
        }
        return instance;
    }

    public boolean supportsType(@Nullable DBPDataSource dataSource, DBSTypedObject typedObject, Class<?> valueType, boolean checkDataSource, boolean checkType)
    {
        final DBPDataKind dataKind = typedObject.getDataKind();
        for (SupportInfo info : supportInfos) {
            if (dataSource != null && info.dataSource != null) {
                if (!supportsAnyProvider(dataSource, info)) {
                    continue;
                }
            } else if (checkDataSource) {
                continue;
            }
            if (info.typeName != null) {
                if (info.typeName.equalsIgnoreCase(typedObject.getTypeName())) {
                    return true;
                }
            }
            if (info.valueType != null) {
                if (info.valueType.matchesType(valueType) && info.dataKind == null || info.dataKind == dataKind) {
                    return true;
                }
            } else if (checkType) {
                continue;
            }

            if (info.dataKind != null && info.dataKind == dataKind) {
                return true;
            }
            if (info.extension != null) {
                DBSDataType dataType = DBUtils.getDataType(typedObject);
                if (dataType != null && CommonUtils.equalObjects(info.extension, CommonUtils.toString(dataType.geTypeExtension()))) {
                    return true;
                }
            }
            if (!checkType && info.valueType == null && info.dataKind == null && info.typeName == null && info.extension == null) {
                return true;
            }
        }
        return false;
    }

    private boolean supportsAnyProvider(@NotNull DBPDataSource dataSource, SupportInfo info) {
        for (DBPDataSourceProviderDescriptor provider = dataSource.getContainer().getDriver().getProviderDescriptor();
            provider != null;
            provider = provider.getParentProvider())
        {
            if (info.dataSource.equals(provider.getId()) || info.dataSource.equals(dataSource.getClass().getName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return id;
    }
}