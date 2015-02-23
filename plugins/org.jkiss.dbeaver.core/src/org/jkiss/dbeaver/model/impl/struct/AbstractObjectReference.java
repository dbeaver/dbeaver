/*
 * Copyright (C) 2010-2015 Serge Rieder
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
package org.jkiss.dbeaver.model.impl.struct;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.model.struct.DBSObjectType;

/**
 * Abstract object reference
 */
public abstract class AbstractObjectReference implements DBSObjectReference {

    private final String name;
    private final DBSObject container;
    private final String description;
    private final DBSObjectType type;

    protected AbstractObjectReference(String name, DBSObject container, String description, DBSObjectType type)
    {
        this.name = name;
        this.container = container;
        this.description = description;
        this.type = type;
    }

    @Override
    public String getName()
    {
        return name;
    }

    public DBSObject getContainer()
    {
        return container;
    }

    @Override
    public String getObjectDescription()
    {
        return description;
    }

    @Override
    public DBSObjectType getObjectType()
    {
        return type;
    }

    @Override
    public String getFullQualifiedName()
    {
        DBPDataSource dataSource = container.getDataSource();
        if (container == dataSource) {
            // In case if there are no schemas/catalogs supported
            // and data source is a root container
            return DBUtils.getQuotedIdentifier(dataSource, name);
        }
        return DBUtils.getFullQualifiedName(dataSource, container, this);

    }
}
