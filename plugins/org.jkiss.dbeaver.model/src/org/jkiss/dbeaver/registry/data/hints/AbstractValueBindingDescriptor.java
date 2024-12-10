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
package org.jkiss.dbeaver.registry.data.hints;

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
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * AbstractValueBindingDescriptor
 */
public abstract class AbstractValueBindingDescriptor<TYPE> extends AbstractDescriptor {
    private static final Log log = Log.getLog(AbstractValueBindingDescriptor.class);

    public static final String TAG_SUPPORTS = "supports"; //$NON-NLS-1$
    private static final String ATTR_KIND = "kind";
    private static final String ATTR_TYPE = "type";
    private static final String ATTR_TYPE_NAME = "typeName";
    private static final String ATTR_DATA_SOURCE = "dataSource";
    private static final String ATTR_EXTENSION = "extension";

    private final String id;
    private final String description;
    protected ObjectType implType;
    protected final List<SupportInfo> supportInfos = new ArrayList<>();
    private TYPE instance;

    protected class SupportInfo {
        String typeName;
        DBPDataKind dataKind;
        ObjectType valueType;
        String extension;
        String dataSource;

        public SupportInfo(IConfigurationElement cfg) {
            String kindName = cfg.getAttribute(ATTR_KIND);
            String typeName = cfg.getAttribute(ATTR_TYPE_NAME);
            String className = cfg.getAttribute(ATTR_TYPE);
            String ext = cfg.getAttribute(ATTR_EXTENSION);
            String dspId = cfg.getAttribute(ATTR_DATA_SOURCE);
            if (!CommonUtils.isEmpty(kindName) || !CommonUtils.isEmpty(typeName) ||
                !CommonUtils.isEmpty(className) || !CommonUtils.isEmpty(dspId) || !CommonUtils.isEmpty(ext)
            ) {
                if (!CommonUtils.isEmpty(kindName)) {
                    try {
                        this.dataKind = DBPDataKind.valueOf(kindName);
                    } catch (IllegalArgumentException e) {
                        log.warn("Bad data kind: " + kindName);
                    }
                }
                if (!CommonUtils.isEmpty(typeName)) {
                    this.typeName = typeName;
                }
                if (!CommonUtils.isEmpty(className)) {
                    this.valueType = new ObjectType(cfg, ATTR_TYPE);
                }
                if (!CommonUtils.isEmpty(ext)) {
                    this.extension = ext;
                }
                if (!CommonUtils.isEmpty(dspId)) {
                    this.dataSource = dspId;
                }
            }
        }

        @Override
        public String toString() {
            StringBuilder str = new StringBuilder();
            if (valueType != null) str.append("valueType=").append(valueType.getImplName()).append("; ");
            if (typeName != null) str.append("typeName=").append(typeName).append(";");
            if (dataKind != null) str.append("dataKind=").append(dataKind).append(";");
            if (extension != null) str.append("extension=").append(extension).append(";");
            if (dataSource != null) str.append("dataSource=").append(dataSource).append(";");
            return str.toString();
        }
    }

    public AbstractValueBindingDescriptor(IConfigurationElement config) {
        super(config);

        this.id = config.getAttribute("id");
        this.description = config.getAttribute("description");
        this.implType = new ObjectType(config.getAttribute("class"));

        IConfigurationElement[] typeElements = config.getChildren(TAG_SUPPORTS);
        for (IConfigurationElement typeElement : typeElements) {
            supportInfos.add(new SupportInfo(typeElement));
        }
    }

    protected abstract Class<TYPE> getImplClass();

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    @NotNull
    public TYPE getInstance() {
        if (instance == null) {
            try {
                this.instance = implType.createInstance(getImplClass());
            } catch (Exception e) {
                throw new IllegalStateException("Can't instantiate value manager '" + this.getId() + "'", e); //$NON-NLS-1$
            }
        }
        return instance;
    }

    public boolean isEnabled() {
        return true;
    }

    public boolean supportsType(
        @Nullable DBPDataSource dataSource,
        DBSTypedObject typedObject,
        Class<?> valueType,
        boolean checkDataSource,
        boolean checkType
    ) {
        if (!isEnabled()) {
            return false;
        }
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
            if (info.valueType != null && valueType != null) {
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
             provider = provider.getParentProvider()) {
            if (info.dataSource.equals(provider.getId()) || info.dataSource.equals(dataSource.getClass().getName())) {
                return true;
            }
        }
        return false;
    }

    public boolean supportsAnyType(
        @Nullable DBPDataSource dataSource,
        DBSTypedObject typedObject,
        Class<?> valueType
    ) {
        if (!isEnabled()) {
            return false;
        }
        if (supportInfos.isEmpty()) {
            return true;
        }
        final DBPDataKind dataKind = typedObject.getDataKind();
        for (SupportInfo info : supportInfos) {
            if (dataSource != null && info.dataSource != null) {
                if (!supportsAnyProvider(dataSource, info)) {
                    return false;
                }
            }
            if (info.typeName != null && info.typeName.equalsIgnoreCase(typedObject.getTypeName())) {
                return true;
            }
            if (info.valueType != null && valueType != null &&
                info.valueType.matchesType(valueType) && (info.dataKind == null || info.dataKind == dataKind)) {
                return true;
            }
            if (info.dataKind != null && info.dataKind == dataKind) {
                return true;
            }
            if (info.extension != null) {
                DBSDataType dataType = DBUtils.getDataType(typedObject);
                if (dataType != null && CommonUtils.equalObjects(
                    info.extension,
                    CommonUtils.toString(dataType.geTypeExtension()))
                ) {
                    return true;
                }
            }
            if (info.valueType == null && info.dataKind == null && info.typeName == null && info.extension == null) {
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