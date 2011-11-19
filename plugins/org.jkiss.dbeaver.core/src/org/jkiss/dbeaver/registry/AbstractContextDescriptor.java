/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBPTool;
import org.osgi.framework.Bundle;

import java.util.ArrayList;
import java.util.List;

/**
 * DataSourceToolDescriptor
 */
public abstract class AbstractContextDescriptor extends AbstractDescriptor
{
    private List<ObjectType> objectTypes = new ArrayList<ObjectType>();

    public AbstractContextDescriptor(
        IContributor contributor, IConfigurationElement config)
    {
        super(contributor);
        if (config != null) {
            String objectType = config.getAttribute(RegistryConstants.ATTR_OBJECT_TYPE);
            if (objectType != null) {
                objectTypes.add(new ObjectType(objectType));
            }
            for (IConfigurationElement typeCfg : config.getChildren(RegistryConstants.TAG_OBJECT_TYPE)) {
                objectTypes.add(new ObjectType(typeCfg));
            }
        }
    }

    public boolean appliesTo(DBPObject object)
    {
        for (ObjectType objectType : objectTypes) {
            if (objectType.appliesTo(object)) {
                return true;
            }
        }
        return false;
    }

}
