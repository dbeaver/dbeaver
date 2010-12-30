/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.ui.export.IDataExporter;

import java.util.ArrayList;
import java.util.List;

/**
 * EntityEditorDescriptor
 */
public class DataExporterDescriptor extends AbstractDescriptor
{
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.dataExportProvider";

    private String id;
    private String className;
    private List<String> sourceTypes = new ArrayList<String>();
    private String name;
    private String description;
    private String fileExtension;
    private Image icon;
    private List<PropertyGroupDescriptor> propertyGroups = new ArrayList<PropertyGroupDescriptor>();

    private Class<?> exporterClass;

    public DataExporterDescriptor(IConfigurationElement config)
    {
        super(config.getContributor());

        this.id = config.getAttribute("id");
        this.className = config.getAttribute("class");
        this.name = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.fileExtension = config.getAttribute("extension");
        String iconPath = config.getAttribute("icon");
        if (!CommonUtils.isEmpty(iconPath)) {
            this.icon = iconToImage(iconPath);
        }

        IConfigurationElement[] typesCfg = config.getChildren("sourceType");
        if (typesCfg != null) {
            for (IConfigurationElement typeCfg : typesCfg) {
                String objectType = typeCfg.getAttribute("type");
                if (objectType != null) {
                    sourceTypes.add(objectType);
                }
            }
        }

        IConfigurationElement[] propElements = config.getChildren(PropertyGroupDescriptor.PROPERTY_GROUP_TAG);
        for (IConfigurationElement prop : propElements) {
            propertyGroups.add(new PropertyGroupDescriptor(prop));
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

    public String getFileExtension()
    {
        return fileExtension;
    }

    public Image getIcon()
    {
        return icon;
    }

    public List<PropertyGroupDescriptor> getPropertyGroups() {
        return propertyGroups;
    }

    public boolean appliesToType(Class objectType)
    {
        if (sourceTypes.isEmpty()) {
            return true;
        }
        for (String sourceType : sourceTypes) {
            Class<?> objectClass = getObjectClass(sourceType);
            if (objectClass != null && objectClass.isAssignableFrom(objectType)) {
                return true;
            }
        }
        return false;
    }

    public Class getExporterClass()
    {
        if (exporterClass == null) {
            exporterClass = getObjectClass(className);
        }
        return exporterClass;
    }

    public IDataExporter createExporter() throws IllegalAccessException, InstantiationException
    {
        Class clazz = getExporterClass();
        if (clazz == null) {
            return null;
        }
        return (IDataExporter)clazz.newInstance();
    }
}
