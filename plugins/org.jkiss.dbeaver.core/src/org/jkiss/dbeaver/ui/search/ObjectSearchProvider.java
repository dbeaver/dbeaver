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
package org.jkiss.dbeaver.ui.search;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.registry.AbstractDescriptor;

public class ObjectSearchProvider extends AbstractDescriptor
{
    static final Log log = LogFactory.getLog(ObjectSearchProvider.class);

    private String id;
    private String label;
    private String description;
    private ObjectType pageClass;
    private ObjectType resultsClass;

    ObjectSearchProvider(IConfigurationElement contributorConfig)
    {
        super(contributorConfig);

        this.id = contributorConfig.getAttribute("id");
        this.label = contributorConfig.getAttribute("label");
        this.description = contributorConfig.getAttribute("description");
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

    public IObjectSearchPage createSearchPage() throws DBException
    {
        return pageClass.createInstance(IObjectSearchPage.class);
    }

    public IObjectSearchResultPage createResultsPage() throws DBException
    {
        return resultsClass.createInstance(IObjectSearchResultPage.class);
    }

}
