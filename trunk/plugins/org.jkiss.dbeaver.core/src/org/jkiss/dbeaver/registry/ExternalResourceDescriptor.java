/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.registry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * ExternalResourceDescriptor
 */
public class ExternalResourceDescriptor extends AbstractDescriptor
{
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.resources"; //$NON-NLS-1$

    static final Log log = LogFactory.getLog(ExternalResourceDescriptor.class);
    private final String name;

    public ExternalResourceDescriptor(IConfigurationElement config)
    {
        super(config);

        this.name = config.getAttribute(RegistryConstants.ATTR_NAME);
    }

    public String getName()
    {
        return name;
    }

    public URL getURL()
    {
        return getContributorBundle().getEntry(name);
    }

}