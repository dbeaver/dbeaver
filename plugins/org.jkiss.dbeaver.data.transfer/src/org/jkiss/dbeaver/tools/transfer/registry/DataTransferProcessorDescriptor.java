/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.app.DBPRegistryDescriptor;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProcessor;
import org.jkiss.dbeaver.ui.ProgramInfo;
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
    private final String appFileExtension;
    private final String appName;
    private final int order;
    @NotNull
    private final DBPImage icon;
    private final List<DBPPropertyDescriptor> properties = new ArrayList<>();
    private boolean isBinary;

    private transient ProgramInfo program;

    DataTransferProcessorDescriptor(DataTransferNodeDescriptor node, IConfigurationElement config) {
        super(config);
        this.node = node;
        this.id = config.getAttribute("id");
        this.processorType = new ObjectType(config.getAttribute("class"));
        this.name = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.icon = iconToImage(config.getAttribute("icon"), DBIcon.TYPE_UNKNOWN);
        this.isBinary = CommonUtils.getBoolean(config.getAttribute("binary"), false);
        this.appFileExtension = config.getAttribute("appFileExtension");
        this.appName = config.getAttribute("appName");
        this.order = CommonUtils.toInt(config.getAttribute("order"));

        for (IConfigurationElement typeCfg : ArrayUtils.safeArray(config.getChildren("sourceType"))) {
            sourceTypes.add(new ObjectType(typeCfg.getAttribute("type")));
        }

        for (IConfigurationElement prop : ArrayUtils.safeArray(config.getChildren(PropertyDescriptor.TAG_PROPERTY_GROUP))) {
            properties.addAll(PropertyDescriptor.extractProperties(prop));
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getAppFileExtension() {
        return appFileExtension;
    }

    public String getAppName() {
        return appName;
    }

    public ProgramInfo getOpenWithApplication() {
        if (program == null) {
            if (!CommonUtils.isEmpty(appFileExtension)) {
                program = ProgramInfo.getProgram(appFileExtension);
            }
        }
        return program;
    }

    public int getOrder() {
        return order;
    }

    @NotNull
    public DBPImage getIcon() {
        return icon;
    }

    public List<DBPPropertyDescriptor> getProperties() {
        return properties;
    }

    public DBPPropertyDescriptor getProperty(String name) {
        for (DBPPropertyDescriptor prop : properties) {
            if (prop.getId().equals(name)) {
                return prop;
            }
        }
        return null;
    }

    boolean appliesToType(Class objectType) {
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

    public boolean adaptsToType(IAdaptable adaptable) {
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

    public IDataTransferProcessor getInstance() {
        try {
            processorType.checkObjectClass(IDataTransferProcessor.class);
            Class<? extends IDataTransferProcessor> clazz = processorType.getObjectClass(IDataTransferProcessor.class);
            return clazz.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Can't instantiate data exporter", e);
        }
    }

    public DataTransferNodeDescriptor getNode() {
        return node;
    }

    public boolean isBinaryFormat() {
        return isBinary;
    }

    public String getFullId() {
        return node.getId() + ":" + getId();
    }
}
