/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverIcons;
import org.osgi.framework.Bundle;

import java.io.IOException;
import java.net.URL;

/**
 * EntityEditorDescriptor
 */
public class AbstractDescriptor {

    static final Log log = LogFactory.getLog(AbstractDescriptor.class);

    private IContributor contributor;

    public AbstractDescriptor(IContributor contributor)
    {
        this.contributor = contributor;
    }

    public IContributor getContributor()
    {
        return contributor;
    }

    public String getContributorName()
    {
        return contributor.getName();
    }

    public Bundle getContributorBundle()
    {
        return Platform.getBundle(getContributorName());
    }

    protected Image iconToImage(String icon)
    {
        if (CommonUtils.isEmpty(icon)) {
            return null;
        } else if (icon.startsWith("#")) {
            // Predefined image
            return DBeaverIcons.getImage(icon.substring(1));
        } else {
            URL iconPath = getContributorBundle().getEntry(icon);
            if (iconPath != null) {
                try {
                    iconPath = FileLocator.toFileURL(iconPath);
                }
                catch (IOException ex) {
                    log.error(ex);
                    return null;
                }
                ImageDescriptor descriptor = ImageDescriptor.createFromURL(iconPath);
                return descriptor.createImage();
            }
        }
        return null;
    }

    public Class<?> getObjectClass(String className)
    {
        Class<?> objectClass = null;
        try {
            objectClass = DBeaverCore.getInstance().getPlugin().getBundle().loadClass(className);
        } catch (ClassNotFoundException ex) {
            // do nothing
        }
        if (objectClass == null) {
            try {
                objectClass = getContributorBundle().loadClass(className);
            } catch (ClassNotFoundException ex) {
                log.error("Can't determine object class '" + className + "'", ex);
            }
        }
        return objectClass;
    }

}