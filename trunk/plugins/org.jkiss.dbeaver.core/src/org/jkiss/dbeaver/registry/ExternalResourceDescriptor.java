/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.registry;

import org.jkiss.dbeaver.Log;
import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;

import java.net.URL;

/**
 * ExternalResourceDescriptor
 */
public class ExternalResourceDescriptor extends AbstractDescriptor
{
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.resources"; //$NON-NLS-1$

    static final Log log = Log.getLog(ExternalResourceDescriptor.class);
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