/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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

package org.jkiss.dbeaver.registry.transfer;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.wizard.IWizardPage;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.tools.transfer.IDataTransferNode;
import org.jkiss.dbeaver.tools.transfer.IDataTransferSettings;
import org.jkiss.utils.ArrayUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

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
        for (IConfigurationElement typeCfg : ArrayUtils.safeArray(config.getChildren(RegistryConstants.ATTR_SOURCE_TYPE))) {
            sourceTypes.add(new ObjectType(typeCfg.getAttribute(RegistryConstants.ATTR_TYPE)));
        }
        for (IConfigurationElement pageConfig : ArrayUtils.safeArray(config.getChildren(RegistryConstants.TAG_PAGE))) {
            pageTypes.add(new ObjectType(pageConfig.getAttribute(RegistryConstants.ATTR_CLASS)));
        }
        for (IConfigurationElement processorConfig : ArrayUtils.safeArray(config.getChildren(RegistryConstants.TAG_PROCESSOR))) {
            processors.add(new DataTransferProcessorDescriptor(this, processorConfig));
        }
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
     * @param objectTypes object types
     * @return list of editors
     */
    public Collection<DataTransferProcessorDescriptor> getAvailableProcessors(Collection<Class<?>> objectTypes)
    {
        List<DataTransferProcessorDescriptor> editors = new ArrayList<>();
        for (DataTransferProcessorDescriptor descriptor : processors) {
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
