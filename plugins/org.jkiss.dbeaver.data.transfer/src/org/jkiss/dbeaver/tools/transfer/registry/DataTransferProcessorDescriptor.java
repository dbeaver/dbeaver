/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

package org.jkiss.dbeaver.tools.transfer.registry;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.app.DBPRegistryDescriptor;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProcessor;
import org.jkiss.dbeaver.tools.transfer.stream.IAppendableDataExporter;
import org.jkiss.dbeaver.tools.transfer.stream.IMultiStreamDataImporter;
import org.jkiss.dbeaver.utils.MimeTypes;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * DataTransferProcessorDescriptor
 */
public class DataTransferProcessorDescriptor extends AbstractDescriptor implements DBPRegistryDescriptor<IDataTransferProcessor> {
    private final DataTransferNodeDescriptor node;
    private final String id;
    private final ObjectType processorType;
    private final List<ObjectType> sourceTypes = new ArrayList<>();
    private final String name;
    private final String description;
    private final String contentType;
    private final String appFileExtension;
    private final String appName;
    private final int order;
    @NotNull
    private final DBPImage icon;
    private final DBPPropertyDescriptor[] properties;
    private final boolean isBinary;
    private final boolean isHTML;

    DataTransferProcessorDescriptor(@NotNull DataTransferNodeDescriptor node, @NotNull IConfigurationElement config) {
        super(config);
        this.node = node;
        this.id = config.getAttribute("id");
        this.processorType = new ObjectType(config.getAttribute("class"));
        this.name = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.icon = iconToImage(config.getAttribute("icon"), DBIcon.TYPE_UNKNOWN);
        this.isBinary = CommonUtils.getBoolean(config.getAttribute("binary"), false);
        this.isHTML = CommonUtils.getBoolean(config.getAttribute("html"), false);
        this.contentType = CommonUtils.toString(config.getAttribute("contentType"), MimeTypes.OCTET_STREAM);
        this.appFileExtension = config.getAttribute("appFileExtension");
        this.appName = config.getAttribute("appName");
        this.order = CommonUtils.toInt(config.getAttribute("order"));

        for (IConfigurationElement typeCfg : ArrayUtils.safeArray(config.getChildren("sourceType"))) {
            sourceTypes.add(new ObjectType(typeCfg.getAttribute("type")));
        }

        this.properties = PropertyDescriptor.extractPropertyGroups(config);
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @Nullable
    public String getContentType() {
        return contentType;
    }

    @Nullable
    public String getAppFileExtension() {
        return appFileExtension;
    }

    @Nullable
    public String getAppName() {
        return appName;
    }

    public int getOrder() {
        return order;
    }

    @NotNull
    public DBPImage getIcon() {
        return icon;
    }

    @NotNull
    public DBPPropertyDescriptor[] getProperties() {
        return properties;
    }

    @Nullable
    public DBPPropertyDescriptor getProperty(@NotNull String name) {
        for (DBPPropertyDescriptor prop : properties) {
            if (prop.getId().equals(name)) {
                return prop;
            }
        }
        return null;
    }

    boolean appliesToType(@NotNull Class<?> objectType) {
        if (sourceTypes.isEmpty()) {
            return true;
        }
        for (ObjectType sourceType : sourceTypes) {
            if (sourceType.matchesType(objectType)) {
                return true;
            }
        }
        return false;
    }

    public boolean adaptsToType(@NotNull IAdaptable adaptable) {
        if (sourceTypes.isEmpty()) {
            return true;
        }
        for (ObjectType sourceType : sourceTypes) {
            if (adaptable.getAdapter(sourceType.getObjectClass()) != null) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public IDataTransferProcessor getInstance() {
        try {
            processorType.checkObjectClass(IDataTransferProcessor.class);
            Class<? extends IDataTransferProcessor> clazz = processorType.getImplClass(IDataTransferProcessor.class);
            return clazz.getConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Can't instantiate data exporter", e);
        }
    }

    @NotNull
    public DataTransferNodeDescriptor getNode() {
        return node;
    }

    public boolean isBinaryFormat() {
        return isBinary;
    }

    public boolean isHTMLFormat() {
        return isHTML;
    }

    public boolean isAppendable() {
        return processorType.matchesType(IAppendableDataExporter.class);
    }

    public boolean isMulti() {
        return processorType.matchesType(IMultiStreamDataImporter.class);
    }

    public String getFullId() {
        return node.getId() + ":" + getId();
    }

    @NotNull
    public String getProcessorFileExtension() {
        DBPPropertyDescriptor extProperty = getProperty("extension");
        String ext = extProperty == null ? getAppFileExtension() : CommonUtils.toString(extProperty.getDefaultValue(), null);
        return CommonUtils.isEmpty(ext) ? "data" : ext;
    }

    @Override
    public String toString() {
        return getFullId();
    }
}
