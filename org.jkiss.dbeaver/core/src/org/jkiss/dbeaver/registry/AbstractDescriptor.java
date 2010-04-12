package org.jkiss.dbeaver.registry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.core.DBeaverIcons;
import org.osgi.framework.Bundle;

import java.io.IOException;
import java.net.URL;

import net.sf.jkiss.utils.CommonUtils;

/**
 * EntityEditorDescriptor
 */
public class AbstractDescriptor {

    static Log log = LogFactory.getLog(AbstractDescriptor.class);

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

}