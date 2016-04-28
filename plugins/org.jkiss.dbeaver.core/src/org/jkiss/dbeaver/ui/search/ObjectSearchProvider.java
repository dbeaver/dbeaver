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
package org.jkiss.dbeaver.ui.search;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;

public class ObjectSearchProvider extends AbstractDescriptor
{
    private static final Log log = Log.getLog(ObjectSearchProvider.class);

    private String id;
    private String label;
    private String description;
    private DBPImage icon;
    private ObjectType pageClass;
    private ObjectType resultsClass;

    ObjectSearchProvider(IConfigurationElement contributorConfig)
    {
        super(contributorConfig);

        this.id = contributorConfig.getAttribute(RegistryConstants.ATTR_ID);
        this.label = contributorConfig.getAttribute(RegistryConstants.ATTR_LABEL);
        this.description = contributorConfig.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.icon = iconToImage(contributorConfig.getAttribute(RegistryConstants.ATTR_ICON));
        this.pageClass = new ObjectType(contributorConfig.getAttribute("pageClass"));
        this.resultsClass = new ObjectType(contributorConfig.getAttribute("resultsClass"));
    }

    public String getId()
    {
        return id;
    }

    public String getLabel()
    {
        return label;
    }

    public String getDescription()
    {
        return description;
    }

    public DBPImage getIcon()
    {
        return icon;
    }

    public IObjectSearchPage createSearchPage() throws DBException
    {
        return pageClass.createInstance(IObjectSearchPage.class);
    }

    public IObjectSearchResultPage createResultsPage() throws DBException
    {
        return resultsClass.createInstance(IObjectSearchResultPage.class);
    }

}
