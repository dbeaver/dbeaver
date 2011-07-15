/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDDataFormatterSample;
import org.jkiss.dbeaver.ui.properties.PropertyDescriptorEx;

import java.util.ArrayList;
import java.util.List;

/**
 * DataFormatterDescriptor
 */
public class DataFormatterDescriptor extends AbstractDescriptor
{
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.dataFormatter"; //$NON-NLS-1$

    private String id;
    private String className;
    private String name;
    private String description;
    private List<PropertyDescriptorEx> properties = new ArrayList<PropertyDescriptorEx>();
    private DBDDataFormatterSample sample;
    private Class<?> formatterClass;

    public DataFormatterDescriptor(IConfigurationElement config)
    {
        super(config.getContributor());

        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.className = config.getAttribute(RegistryConstants.ATTR_CLASS);
        this.name = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);

        IConfigurationElement[] propElements = config.getChildren(PropertyDescriptorEx.TAG_PROPERTY_GROUP);
        for (IConfigurationElement prop : propElements) {
            properties.addAll(PropertyDescriptorEx.extractProperties(prop));
        }
        Class<?> objectClass = getObjectClass(config.getAttribute(RegistryConstants.ATTR_SAMPLE_CLASS));
        try {
            sample = (DBDDataFormatterSample)objectClass.newInstance();
        } catch (Exception e) {
            log.error("Could not instantiate data formatter '" + getId() + "' sample");
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

    public DBDDataFormatterSample getSample()
    {
        return sample;
    }

    public List<PropertyDescriptorEx> getProperties() {
        return properties;
    }

    public Class getFormatterClass()
    {
        if (formatterClass == null) {
            formatterClass = getObjectClass(className);
        }
        return formatterClass;
    }

    public DBDDataFormatter createFormatter() throws IllegalAccessException, InstantiationException
    {
        Class clazz = getFormatterClass();
        if (clazz == null) {
            return null;
        }
        return (DBDDataFormatter)clazz.newInstance();
    }

}
