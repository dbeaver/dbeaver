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

package org.jkiss.dbeaver.tools.transfer.registry;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.wizard.IWizardPage;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.tools.transfer.IDataTransferNode;
import org.jkiss.dbeaver.tools.transfer.IDataTransferSettings;
import org.jkiss.utils.ArrayUtils;

import java.util.*;

/**
 * DataTransferNodeDescriptor
 */
public class DataTransferNodeDescriptor extends AbstractDescriptor
{
    private static final Log log = Log.getLog(DataTransferNodeDescriptor.class);

    enum NodeType {
        PRODUCER,
        CONSUMER
    }

    @NotNull
    private final String id;
    @NotNull
    private final String name;
    private final String description;
    @NotNull
    private final DBPImage icon;
    private final NodeType nodeType;
    private final ObjectType implType;
    private final ObjectType settingsType;
    private final List<ObjectType> sourceTypes = new ArrayList<>();
    private final List<ObjectType> pageTypes = new ArrayList<>();
    private final List<DataTransferProcessorDescriptor> processors = new ArrayList<>();

    public DataTransferNodeDescriptor(IConfigurationElement config)
    {
        super(config);

        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.name = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.icon = iconToImage(config.getAttribute(RegistryConstants.ATTR_ICON), DBIcon.TYPE_UNKNOWN);
        this.nodeType = NodeType.valueOf(config.getAttribute(RegistryConstants.ATTR_TYPE).toUpperCase(Locale.ENGLISH));
        this.implType = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
        this.settingsType = new ObjectType(config.getAttribute(RegistryConstants.ATTR_SETTINGS));

        loadNodeConfigurations(config);
    }

    void loadNodeConfigurations(IConfigurationElement config) {
        for (IConfigurationElement typeCfg : ArrayUtils.safeArray(config.getChildren(RegistryConstants.ATTR_SOURCE_TYPE))) {
            sourceTypes.add(new ObjectType(typeCfg.getAttribute(RegistryConstants.ATTR_TYPE)));
        }
        for (IConfigurationElement pageConfig : ArrayUtils.safeArray(config.getChildren(RegistryConstants.TAG_PAGE))) {
            pageTypes.add(new ObjectType(pageConfig.getAttribute(RegistryConstants.ATTR_CLASS)));
        }
        for (IConfigurationElement processorConfig : ArrayUtils.safeArray(config.getChildren(RegistryConstants.TAG_PROCESSOR))) {
            processors.add(new DataTransferProcessorDescriptor(this, processorConfig));
        }
        processors.sort(Comparator.comparing(DataTransferProcessorDescriptor::getName));
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    @NotNull
    public DBPImage getIcon()
    {
        return icon;
    }

    public Class<? extends IDataTransferNode> getNodeClass()
    {
        return implType.getObjectClass(IDataTransferNode.class);
    }

    public IDataTransferNode createNode() throws DBException
    {
        implType.checkObjectClass(IDataTransferNode.class);
        try {
            return implType.getObjectClass(IDataTransferNode.class).newInstance();
        } catch (Throwable e) {
            throw new DBException("Can't create data transformer node", e);
        }
    }

    public IDataTransferSettings createSettings() throws DBException
    {
        settingsType.checkObjectClass(IDataTransferSettings.class);
        try {
            return settingsType.getObjectClass(IDataTransferSettings.class).newInstance();
        } catch (Throwable e) {
            throw new DBException("Can't create node settings", e);
        }
    }

    public IWizardPage[] createWizardPages()
    {
        List<IWizardPage> pages = new ArrayList<>();
        for (ObjectType type : pageTypes) {
            try {
                type.checkObjectClass(IWizardPage.class);
                pages.add(type.getObjectClass(IWizardPage.class).newInstance());
            } catch (Throwable e) {
                log.error("Can't create wizard page", e);
            }
        }
        return pages.toArray(new IWizardPage[pages.size()]);
    }

    public NodeType getNodeType()
    {
        return nodeType;
    }

    public boolean appliesToType(Class objectType)
    {
        if (!sourceTypes.isEmpty()) {
            for (ObjectType sourceType : sourceTypes) {
                if (sourceType.matchesType(objectType)) {
                    return true;
                }
            }
        }
        for (DataTransferProcessorDescriptor processor : processors) {
            if (processor.appliesToType(objectType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns data exporter which supports ALL specified object types
     * @param surceObjects object types
     * @return list of editors
     */
    public Collection<DataTransferProcessorDescriptor> getAvailableProcessors(Collection<DBSObject> surceObjects)
    {
        List<DataTransferProcessorDescriptor> editors = new ArrayList<>();
        for (DataTransferProcessorDescriptor descriptor : processors) {
            boolean supports = true;
            for (DBSObject sourceObject : surceObjects) {
                if (!descriptor.appliesToType(sourceObject.getClass())) {
                    boolean adapts = false;
                    if (sourceObject instanceof IAdaptable) {
                        if (descriptor.adaptsToType((IAdaptable)sourceObject)) {
                            adapts = true;
                        }
                    }
                    if (!adapts) {
                        supports = false;
                        break;
                    }
                }
            }
            if (supports) {
                editors.add(descriptor);
            }
        }
        return editors;
    }

    public DataTransferProcessorDescriptor getProcessor(String id)
    {
        for (DataTransferProcessorDescriptor descriptor : processors) {
            if (descriptor.getId().equals(id)) {
                return descriptor;
            }
        }
        return null;
    }

}
